package com.example.data.streak

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.UserPreferences
import com.example.data.reminder.ReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object StreakReminderScheduler {

    private const val TAG = "TikoStreakReminder"
    const val ACTION_STREAK_REMINDER = "com.example.tiko.ACTION_STREAK_REMINDER"
    private const val STREAK_REMINDER_REQUEST_CODE = 999991

    /**
     * Schedules the evening streak reminder if eligible.
     * Eligible when:
     * - streakRemindersEnabled is true
     * - currentStreak >= 2
     * - user has NOT completed a task today
     * - time is before the reminder time today (or if past, skips until tomorrow or next eligible check)
     */
    fun schedule(
        context: Context,
        preferences: UserPreferences,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val enabled = preferences.streakRemindersEnabled.value
        val currentStreak = preferences.currentStreak.value
        val lastCompleted = preferences.lastCompletedDay.value
        val todayKey = StreakManager.todayDateKey(zoneId)

        // 1. Check eligibility
        if (!enabled || currentStreak < 2 || lastCompleted == todayKey) {
            cancel(context)
            Log.d(TAG, "Streak reminder not needed (enabled=$enabled, streak=$currentStreak, lastCompleted=$lastCompleted, today=$todayKey)")
            return
        }

        // 2. Parse target reminder time (default 8:00 PM)
        val timeStr = preferences.streakReminderTime.value
        val reminderLocalTime = parseReminderTime(timeStr)

        val now = LocalDateTime.now(zoneId)
        var targetDateTime = LocalDateTime.of(LocalDate.now(zoneId), reminderLocalTime)

        // If target time today has already passed, schedule for tomorrow
        if (targetDateTime.isBefore(now.plusSeconds(5))) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerEpochMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled streak reminder for ${targetDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} (epoch=$triggerEpochMillis) for streak=$currentStreak")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule streak reminder: ${e.message}")
        }
    }

    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = buildPendingIntent(context)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled streak reminder")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling streak reminder: ${e.message}")
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_STREAK_REMINDER
            data = Uri.parse("tiko://reminder/streak")
        }
        return PendingIntent.getBroadcast(
            context,
            STREAK_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun parseReminderTime(timeStr: String): LocalTime {
        return try {
            val upper = timeStr.trim().uppercase(Locale.US)
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            LocalTime.parse(upper, formatter)
        } catch (_: Exception) {
            try {
                val formatter2 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
                LocalTime.parse(timeStr.trim().uppercase(Locale.US), formatter2)
            } catch (_: Exception) {
                LocalTime.of(20, 0) // 8:00 PM default
            }
        }
    }
}
