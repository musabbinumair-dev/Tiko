package com.example.data.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.TikoDatabase
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderReceiver.EXTRA_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(
            ReminderReceiver.EXTRA_NOTIFICATION_ID,
            taskId.hashCode()
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)

        when (intent.action) {
            ACTION_MARK_DONE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = TikoDatabase.getInstance(context)
                        val repository = TaskRepository(context, database.taskDao())
                        repository.completeTask(taskId)
                        ReminderScheduler.cancel(context, taskId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                ReminderScheduler.scheduleSnooze(context, taskId, 10 * 60 * 1000L)
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.musab.tiko.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.musab.tiko.action.SNOOZE"
    }
}

