package com.example.data.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.TikoDatabase
import com.example.data.model.TaskEntity
import com.musab.tiko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1L)
        val now = System.currentTimeMillis()
        val delay = if (scheduledTime > 0) now - scheduledTime else 0L

        Log.d("TikoReminder", "ReminderReceiver fired: taskId=$taskId, delay=${delay}ms, scheduled=$scheduledTime, now=$now")
        ReminderScheduler.recordFiredDelay(context, delay)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                    return@launch
                }

                // Query Room database with a short timeout so we remain fast
                val taskFromDb = withTimeoutOrNull(2000L) {
                    try {
                        val database = TikoDatabase.getInstance(context)
                        database.taskDao().getTaskByIdOnce(taskId)
                    } catch (e: Exception) {
                        Log.w("TikoReminder", "Failed to query task from DB: ${e.message}")
                        null
                    }
                }

                if (taskFromDb != null) {
                    if (taskFromDb.done || taskFromDb.deleted) {
                        Log.d("TikoReminder", "Task $taskId is done or deleted; dropping reminder notification.")
                        return@launch
                    }
                    showNotification(context, taskFromDb)
                } else {
                    // Fallback to extras if Room lookup is slow or fails
                    val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""
                    if (title.isNotBlank()) {
                        Log.d("TikoReminder", "Room lookup timed out or failed; posting notification from extras for taskId=$taskId")
                        val fallbackTask = TaskEntity(
                            id = taskId,
                            title = title,
                            category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "Work",
                            notes = intent.getStringExtra(EXTRA_TASK_NOTES) ?: "",
                            dueTime = intent.getStringExtra(EXTRA_TASK_DUE_TIME) ?: TaskEntity.DEFAULT_DUE_TIME
                        )
                        showNotification(context, fallbackTask)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showNotification(context: Context, task: TaskEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        createChannelIfNeeded(notificationManager)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = task.id.hashCode()

        // Action: Mark done
        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10 min
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (task.category.isNotBlank() && task.dueTime.isNotBlank()) {
            "${task.category} • ${task.dueTime}"
        } else {
            task.category.ifBlank { task.dueTime }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(Color.parseColor("#6B4EFF"))
            .setContentTitle(task.title)
            .setContentText(bodyText)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, "Mark done", donePendingIntent)
            .addAction(0, "Snooze 10 min", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for scheduled tasks"
                    enableLights(true)
                    lightColor = Color.parseColor("#6B4EFF")
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Task reminders"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_CATEGORY = "task_category"
        const val EXTRA_TASK_NOTES = "task_notes"
        const val EXTRA_TASK_DUE_TIME = "task_due_time"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Reminders for scheduled tasks"
                        enableLights(true)
                        lightColor = Color.parseColor("#6B4EFF")
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }
}
