package com.example.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.TikoDatabase
import com.example.data.model.TaskEntity
import com.musab.tiko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TikoDatabase.getInstance(context)
                val task = database.taskDao().getTaskByIdOnce(taskId)
                val prefs = context.getSharedPreferences("tiko_settings_prefs", Context.MODE_PRIVATE)
                val notifEnabled = prefs.getBoolean("notif_enabled", false)
                val hasPermission = NotificationPermissionHelper.hasNotificationPermission(context)
                if (!notifEnabled || !hasPermission) {
                    return@launch
                }
                if (task == null || task.done || task.deleted) {
                    return@launch
                }
                showNotification(context, task)
            } finally {
                pendingResult.finish()
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
            .addAction(0, "Mark done", donePendingIntent)
            .addAction(0, "Snooze 10 min", snoozePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Task reminders"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}

