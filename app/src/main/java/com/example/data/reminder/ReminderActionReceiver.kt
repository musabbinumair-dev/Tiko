package com.example.data.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.data.local.TikoDatabase
import com.example.data.repository.TaskRepository
import com.musab.tiko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderReceiver.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(ReminderReceiver.EXTRA_TASK_TITLE) ?: ""
        val category = intent.getStringExtra(ReminderReceiver.EXTRA_TASK_CATEGORY) ?: "Work"
        val notificationId = intent.getIntExtra(
            ReminderReceiver.EXTRA_NOTIFICATION_ID,
            taskId.hashCode()
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        when (intent.action) {
            ACTION_MARK_DONE -> {
                // 1. Update the notification in place immediately to a celebration
                showDoneCelebration(context, notificationManager, notificationId, taskTitle, category)
                NotificationGroupingHelper.onTaskDismissedOrDone(context, taskId, notificationManager)

                // 2. Perform DB operations and alarm cancellation in background
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ReminderScheduler.cancel(context, taskId)
                        val database = TikoDatabase.getInstance(context)
                        val repository = TaskRepository(context, database.taskDao())
                        repository.completeTask(taskId)
                    } catch (_: Exception) {
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                // 1. Update the notification in place immediately to snoozed state
                showSnoozedConfirmation(context, notificationManager, notificationId, taskTitle, category)

                // 2. Reschedule for 10 minutes later
                ReminderScheduler.scheduleSnooze(context, taskId, 10 * 60 * 1000L)
            }
        }
    }

    private fun showDoneCelebration(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        taskTitle: String,
        category: String
    ) {
        val safeTitle = NotificationCopy.sanitizeTitle(taskTitle)
        val categoryColor = NotificationIconHelper.getCategoryColor(category)
        val largeIcon = NotificationIconHelper.createCategoryLargeIcon(context, category)

        val celebration = NotificationCompat.Builder(context, ReminderReceiver.CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setLargeIcon(largeIcon)
            .setColor(categoryColor)
            .setContentTitle("Nice work! 🎉")
            .setContentText("$safeTitle is done")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, celebration)

        // Short auto-dismiss after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                notificationManager.cancel(notificationId)
            } catch (_: Exception) {}
        }, 5000L)
    }

    private fun showSnoozedConfirmation(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        taskTitle: String,
        category: String
    ) {
        val safeTitle = NotificationCopy.sanitizeTitle(taskTitle)
        val categoryColor = NotificationIconHelper.getCategoryColor(category)
        val largeIcon = NotificationIconHelper.createCategoryLargeIcon(context, category)

        val snoozed = NotificationCompat.Builder(context, ReminderReceiver.CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setLargeIcon(largeIcon)
            .setColor(categoryColor)
            .setContentTitle("Snoozed for 10 min ⏰")
            .setContentText("$safeTitle will remind you again in 10 minutes")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, snoozed)
    }

    companion object {
        const val ACTION_MARK_DONE = "com.musab.tiko.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.musab.tiko.action.SNOOZE"
    }
}
