package com.example.data.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import com.musab.tiko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class StreakReminderDecision {
    data class ScheduleToday(val triggerMillis: Long, val reason: String) : StreakReminderDecision()
    data class ScheduleTomorrow(val triggerMillis: Long, val reason: String) : StreakReminderDecision()
    data class Skip(val reason: String) : StreakReminderDecision()
}

object StreakReminderScheduler {
    const val TAG = "TikoStreakReminder"
    const val STREAK_REMINDER_REQUEST_CODE = 8801
    const val NOTIFICATION_ID_STREAK = 7001
    const val STREAK_DATA_URI = "tiko://streak-reminder"

    fun createStreakPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StreakReminderReceiver::class.java).apply {
            data = Uri.parse(STREAK_DATA_URI)
            action = StreakReminderReceiver.ACTION_STREAK_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            STREAK_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun parseReminderTimeToMillis(
        dateKey: String,
        timeStr: String,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        val rawTime = if (timeStr.isBlank()) "8:00 PM" else timeStr.trim()
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
                val date = sdf.parse("$dateKey $effectiveTime")
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

    /**
     * Testable decision function for streak reminder scheduling:
     * - Toggle must be ON
     * - currentStreak >= 2
     * - User has NOT completed a task today
     * - Reminder has NOT already been sent today
     * - If reminder time not yet passed today: schedule today
     * - If reminder time has already passed today: nothing today, schedule for tomorrow
     */
    fun evaluateDecision(
        toggleEnabled: Boolean,
        currentStreak: Int,
        hasCompletedTaskToday: Boolean,
        reminderTime: String,
        nowMillis: Long = System.currentTimeMillis(),
        lastSentDateKey: String? = null,
        todayDateKey: String = StreakManager.getTodayDateKey(nowMillis),
        timeZone: TimeZone = TimeZone.getDefault()
    ): StreakReminderDecision {
        if (!toggleEnabled) {
            return StreakReminderDecision.Skip("Streak reminders toggle is OFF")
        }
        if (currentStreak < 2) {
            return StreakReminderDecision.Skip("Streak is $currentStreak (< 2)")
        }
        if (hasCompletedTaskToday) {
            return StreakReminderDecision.Skip("Task already completed today ($todayDateKey)")
        }
        if (lastSentDateKey == todayDateKey) {
            return StreakReminderDecision.Skip("Reminder already sent today ($todayDateKey)")
        }

        val todayTrigger = parseReminderTimeToMillis(todayDateKey, reminderTime, timeZone)
            ?: return StreakReminderDecision.Skip("Unable to parse reminder time '$reminderTime'")

        return if (todayTrigger > nowMillis) {
            StreakReminderDecision.ScheduleToday(
                triggerMillis = todayTrigger,
                reason = "Eligible for today at $reminderTime"
            )
        } else {
            val tomorrowCal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowKey = StreakManager.getTodayDateKey(tomorrowCal.timeInMillis, timeZone)
            val tomorrowTrigger = parseReminderTimeToMillis(tomorrowKey, reminderTime, timeZone)
                ?: (todayTrigger + 24 * 60 * 60 * 1000L)
            StreakReminderDecision.ScheduleTomorrow(
                triggerMillis = tomorrowTrigger,
                reason = "Reminder time already passed today; scheduled for tomorrow at $reminderTime"
            )
        }
    }

    fun reschedule(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferences(context)
                val toggleEnabled = prefs.streakRemindersEnabled.value
                val reminderTime = prefs.streakReminderTime.value
                val lastSentKey = prefs.lastStreakReminderSentDateKey.value
                val now = System.currentTimeMillis()
                val todayKey = StreakManager.getTodayDateKey(now)

                val database = TikoDatabase.getInstance(context)
                val tasks = database.taskDao().getAllTasksRaw()
                val currentStreak = StreakManager.computeCurrentStreak(tasks)
                val taskDoneToday = StreakManager.hasCompletedTaskToday(tasks, todayKey)

                val decision = evaluateDecision(
                    toggleEnabled = toggleEnabled,
                    currentStreak = currentStreak,
                    hasCompletedTaskToday = taskDoneToday,
                    reminderTime = reminderTime,
                    nowMillis = now,
                    lastSentDateKey = lastSentKey,
                    todayDateKey = todayKey
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val pendingIntent = createStreakPendingIntent(context)

                when (decision) {
                    is StreakReminderDecision.ScheduleToday -> {
                        setAlarm(context, alarmManager, pendingIntent, decision.triggerMillis)
                        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(decision.triggerMillis))
                        Log.d(
                            TAG,
                            "Scheduling decision: todayKey=$todayKey, currentStreak=$currentStreak, taskDoneToday=$taskDoneToday, scheduledTime=$timeStr, action=Scheduled, reason=${decision.reason}"
                        )
                    }
                    is StreakReminderDecision.ScheduleTomorrow -> {
                        setAlarm(context, alarmManager, pendingIntent, decision.triggerMillis)
                        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(decision.triggerMillis))
                        Log.d(
                            TAG,
                            "Scheduling decision: todayKey=$todayKey, currentStreak=$currentStreak, taskDoneToday=$taskDoneToday, scheduledTime=$timeStr, action=ScheduledTomorrow, reason=${decision.reason}"
                        )
                    }
                    is StreakReminderDecision.Skip -> {
                        try {
                            alarmManager?.cancel(pendingIntent)
                            pendingIntent.cancel()
                        } catch (_: Exception) {}
                        Log.d(
                            TAG,
                            "Scheduling decision: todayKey=$todayKey, currentStreak=$currentStreak, taskDoneToday=$taskDoneToday, scheduledTime=None, action=Skipped, reason=${decision.reason}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in reschedule: ${e.message}", e)
            }
        }
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager?,
        pendingIntent: PendingIntent,
        triggerMillis: Long
    ) {
        if (alarmManager == null) return
        val canSchedule = NotificationPermissionHelper.canScheduleExactAlarms(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canSchedule) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule streak reminder: ${e.message}")
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = createStreakPendingIntent(context)
        try {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID_STREAK)
        Log.d(TAG, "Streak reminder cancelled")
    }

    fun postNotification(context: Context, streak: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        ReminderReceiver.createChannel(context)

        val title = NotificationCopy.getStreakReminderTitle(streak)
        val body = NotificationCopy.STREAK_REMINDER_BODY
        val largeIcon = NotificationIconHelper.createStreakLargeIcon(context)
        val limeColor = Color.parseColor("#BAE136")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "home")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            7002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderReceiver.CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setLargeIcon(largeIcon)
            .setColor(limeColor)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STREAK, notification)
    }

    fun sendTestReminder(context: Context) {
        val hasPermission = NotificationPermissionHelper.hasNotificationPermission(context)
        if (!hasPermission) {
            AppToastController.show("Notification permission required", ToastType.Warning)
            return
        }
        val prefs = UserPreferences(context)
        if (!prefs.streakRemindersEnabled.value) {
            AppToastController.show("Streak reminders toggle is OFF", ToastType.Info)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = TikoDatabase.getInstance(context)
                val tasks = database.taskDao().getAllTasksRaw()
                val currentStreak = StreakManager.computeCurrentStreak(tasks)
                val effectiveStreak = if (currentStreak >= 2) currentStreak else 5
                postNotification(context, effectiveStreak)
                Log.d(TAG, "Test streak reminder sent: streak=$effectiveStreak")
                AppToastController.show("Test streak reminder sent ($effectiveStreak-day)", ToastType.Success)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send test streak reminder: ${e.message}")
            }
        }
    }
}
