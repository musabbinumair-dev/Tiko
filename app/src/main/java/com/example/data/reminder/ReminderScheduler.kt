package com.example.data.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.TaskEntity
import com.musab.tiko.R
import java.text.SimpleDateFormat
import java.util.Locale

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
        val taskNotes = intent.getStringExtra("task_notes") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "tiko_task_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(taskTitle)
            .setContentText(taskNotes.ifBlank { "You have a task due soon." })
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun rescheduleAllFutureReminders(context: Context, tasks: List<TaskEntity>) {
        for (task in tasks) {
            if (!task.done && !task.deleted) {
                scheduleReminder(context, task)
            } else {
                cancelReminder(context, task.id)
            }
        }
    }

    fun scheduleReminder(context: Context, task: TaskEntity) {
        if (task.reminder.equals("None", ignoreCase = true) || task.dueDate.isBlank()) {
            cancelReminder(context, task.id)
            return
        }

        val triggerTime = computeTriggerTime(task.dueDate, task.dueTime, task.reminder) ?: return
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_notes", task.notes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule exact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun computeTriggerTime(dueDate: String, dueTime: String, reminder: String): Long? {
        val dueTimestamp = parseDueTimestamp(dueDate, dueTime) ?: return null
        val offset = when (reminder.trim().lowercase()) {
            "15 min before" -> 15 * 60 * 1000L
            "30 min before" -> 30 * 60 * 1000L
            "1 hour before" -> 60 * 60 * 1000L
            "none" -> return null
            else -> 30 * 60 * 1000L
        }
        return dueTimestamp - offset
    }

    private fun parseDueTimestamp(dueDate: String, dueTime: String): Long? {
        val formats = listOf("yyyy-MM-dd hh:mm a", "yyyy-MM-dd HH:mm", "yyyy-MM-dd h:mm a")
        val timeStr = if (dueTime.isBlank()) "10:00 AM" else dueTime.trim()
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                val d = sdf.parse("$dueDate $timeStr")
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }
}
