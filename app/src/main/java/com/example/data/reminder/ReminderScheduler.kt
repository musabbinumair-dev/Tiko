package com.example.data.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReminderScheduler {
    private const val TAG = "TikoReminder"
    private const val PREFS_NAME = "tiko_settings_prefs"
    private const val KEY_LAST_DELAY = "last_reminder_delay_ms"

    @Volatile
    var lastReminderDelayMs: Long? = null

    fun recordFiredDelay(context: Context, delayMs: Long) {
        lastReminderDelayMs = delayMs
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_DELAY, delayMs)
                .apply()
        } catch (_: Exception) {}
    }

    fun getLastFiredDelay(context: Context): Long? {
        if (lastReminderDelayMs != null) return lastReminderDelayMs
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_LAST_DELAY)) prefs.getLong(KEY_LAST_DELAY, 0L) else null
        } catch (_: Exception) {
            null
        }
    }

    fun createReminderIntent(context: Context, taskId: String): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.parse("tiko://reminder/$taskId")
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
    }

    fun schedule(context: Context, task: TaskEntity) {
        if (task.done || task.deleted || task.dueDate.isBlank()) {
            cancel(context, task.id)
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifEnabled = prefs.getBoolean("notif_enabled", false)
        val hasPermission = NotificationPermissionHelper.hasNotificationPermission(context)
        if (!notifEnabled || !hasPermission) {
            cancel(context, task.id)
            return
        }

        val now = System.currentTimeMillis()
        val triggerTime = computeTriggerTime(task.dueDate, task.dueTime, task.reminder, now)
        if (triggerTime == null) {
            cancel(context, task.id)
            return
        }

        setAlarm(context, task, triggerTime, now)
    }

    fun cancel(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = createReminderIntent(context, taskId)
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
        if (notificationManager != null) {
            notificationManager.cancel(taskId.hashCode())
            NotificationGroupingHelper.onTaskDismissedOrDone(context, taskId, notificationManager)
        }
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
        val fakeTask = TaskEntity(
            id = taskId,
            title = "Task Reminder",
            dueDate = "today",
            dueTime = "",
            reminder = "At time"
        )
        setAlarm(context, fakeTask, triggerTime, System.currentTimeMillis())
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

    private fun setAlarm(context: Context, task: TaskEntity, triggerTime: Long, now: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = createReminderIntent(context, task.id).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(ReminderReceiver.EXTRA_TASK_CATEGORY, task.category)
            putExtra(ReminderReceiver.EXTRA_TASK_NOTES, task.notes)
            putExtra(ReminderReceiver.EXTRA_TASK_DUE_TIME, task.dueTime)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, triggerTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canSchedule = NotificationPermissionHelper.canScheduleExactAlarms(context)
        val hasNotifPermission = NotificationPermissionHelper.hasNotificationPermission(context)
        var apiUsed = "None"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canSchedule) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    apiUsed = "setExactAndAllowWhileIdle"
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    apiUsed = "setAndAllowWhileIdle (canScheduleExactAlarms=false)"
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                apiUsed = "setExactAndAllowWhileIdle"
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                apiUsed = "setExact"
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    apiUsed = "fallback:setAndAllowWhileIdle"
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    apiUsed = "fallback:set"
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule alarm: ${e.message}")
        }

        Log.d(
            TAG,
            "Scheduling decision: taskId=${task.id}, dueTime=${task.dueTime}, offset=${task.reminder}, computedTrigger=$triggerTime, now=$now, api=$apiUsed, canScheduleExactAlarms=$canSchedule, notifPermission=$hasNotifPermission"
        )
    }

    /**
     * Computes the trigger timestamp based on the updated timing rules:
     * - Normal: trigger = due - offset.
     * - If trigger is in the past (or within 1 second) but the due time is still in the future: trigger = due time.
     * - If the due time is within the current minute (due is no more than 60 seconds in the past): fire in about 2 seconds, so the user still gets the reminder.
     * - If the due time is more than 60 seconds in the past: no reminder.
     * - "None" offset: no reminder, as before.
     */
    fun computeTriggerTime(
        dueDate: String,
        dueTime: String,
        reminder: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        val offsetMillis = parseReminderOffsetMillis(reminder) ?: return null
        val dueEpochMillis = parseDueTimestamp(dueDate, dueTime, nowMillis) ?: return null

        // If the due time is more than 60 seconds in the past: no reminder
        if (nowMillis - dueEpochMillis > 60_000L) {
            return null
        }

        // If the due time is within the current minute (due is no more than 60 seconds in the past):
        // fire in about 2 seconds, so the user still gets the reminder.
        if (dueEpochMillis <= nowMillis && (nowMillis - dueEpochMillis) <= 60_000L) {
            return nowMillis + 2000L
        }

        // Due time is in the future (> nowMillis)
        val triggerTime = dueEpochMillis - offsetMillis

        // If trigger is in the past (or within 1 second) but the due time is still in the future:
        // trigger = due time
        return if (triggerTime <= nowMillis + 1000L) {
            dueEpochMillis
        } else {
            triggerTime
        }
    }

    fun parseDueTimestamp(
        dueDate: String,
        dueTime: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (dueDate.isBlank()) return null
        val rawDate = dueDate.trim()
        val timeZone = TimeZone.getDefault()
        val effectiveDate = if (rawDate.equals("today", ignoreCase = true)) {
            val sdfToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                this.timeZone = timeZone
            }
            sdfToday.format(Date(nowMillis))
        } else rawDate

        val rawTime = if (dueTime.isBlank()) TaskEntity.DEFAULT_DUE_TIME else dueTime.trim()
        val effectiveTime = rawTime.replace('\u00A0', ' ').replace('\u202F', ' ').trim()

        val patterns = listOf(
            "yyyy-MM-dd h:mm a",
            "yyyy-MM-dd hh:mm a",
            "yyyy-MM-dd H:mm",
            "yyyy-MM-dd HH:mm"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    this.timeZone = timeZone
                    this.isLenient = false
                }
                val date = sdf.parse("$effectiveDate $effectiveTime")
                if (date != null) {
                    val cal = Calendar.getInstance(timeZone).apply {
                        time = date
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun parseReminderOffsetMillis(reminder: String): Long? {
        val normalized = reminder.trim().lowercase(Locale.US)
        return when (normalized) {
            "", "none" -> null
            "15 min before", "15 minutes before" -> 15 * 60 * 1000L
            "30 min before", "30 minutes before" -> 30 * 60 * 1000L
            "1 hour before", "1 hr before", "60 min before" -> 60 * 60 * 1000L
            "at time", "at time of task", "at time of event", "0 min before", "at due time" -> 0L
            else -> {
                Log.w(TAG, "Unrecognized reminder value '$reminder', treating as None")
                null
            }
        }
    }
}
