package com.example.data.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.TaskEntity
import com.musab.tiko.BuildConfig
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun schedule(context: Context, task: TaskEntity) {
        if (task.done || task.deleted || task.dueDate.isBlank()) {
            cancel(context, task.id)
            return
        }

        val triggerTime = computeTriggerTime(task.dueDate, task.dueTime, task.reminder)
        if (triggerTime == null) {
            cancel(context, task.id)
            return
        }

        setAlarm(context, task.id, triggerTime)
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.hashCode())
    }

    fun cancelAll(context: Context, tasks: List<TaskEntity>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
        for (task in tasks) {
            cancel(context, task.id)
        }
    }

    fun scheduleSnooze(context: Context, taskId: String, delayMillis: Long = 10 * 60 * 1000L) {
        val triggerTime = System.currentTimeMillis() + delayMillis
        setAlarm(context, taskId, triggerTime)
    }

    fun rescheduleAllFutureReminders(context: Context, tasks: List<TaskEntity>) {
        for (task in tasks) {
            if (!task.done && !task.deleted) {
                schedule(context, task)
            } else {
                cancel(context, task.id)
            }
        }
    }

    private fun setAlarm(context: Context, taskId: String, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Exact alarm permission missing or denied; falling back to setAndAllowWhileIdle: ${e.message}")
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Fallback alarm scheduling failed: ${fallbackEx.message}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to schedule alarm: ${e.message}")
            }
        }
    }

    fun computeTriggerTime(dueDate: String, dueTime: String, reminder: String): Long? {
        val dueEpochMillis = parseDueTimestamp(dueDate, dueTime) ?: return null
        val now = System.currentTimeMillis()

        if (dueEpochMillis <= now) {
            return null
        }

        val offsetMillis = parseReminderOffsetMillis(reminder) ?: return null
        val triggerTime = dueEpochMillis - offsetMillis

        return if (triggerTime <= now) {
            dueEpochMillis
        } else {
            triggerTime
        }
    }

    fun parseDueTimestamp(dueDate: String, dueTime: String): Long? {
        if (dueDate.isBlank()) return null
        val effectiveTime = if (dueTime.isBlank()) TaskEntity.DEFAULT_DUE_TIME else dueTime.trim()
        val timeZone = TimeZone.getDefault()
        val patterns = listOf("yyyy-MM-dd h:mm a", "yyyy-MM-dd hh:mm a", "yyyy-MM-dd HH:mm")

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    this.timeZone = timeZone
                    this.isLenient = false
                }
                val date = sdf.parse("$dueDate $effectiveTime")
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun parseReminderOffsetMillis(reminder: String): Long? {
        val normalized = reminder.trim().lowercase(Locale.US)
        return when (normalized) {
            "", "none" -> null
            "15 min before" -> 15 * 60 * 1000L
            "30 min before" -> 30 * 60 * 1000L
            "1 hour before" -> 60 * 60 * 1000L
            "at time", "at time of task", "at time of event", "0 min before" -> 0L
            else -> {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Unrecognized reminder value '$reminder', treating as None")
                }
                null
            }
        }
    }
}

