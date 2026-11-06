package com.example.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        Log.d("TikoStreakReminder", "StreakReminderReceiver triggered at $now")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferences(context)
                val toggleEnabled = prefs.streakRemindersEnabled.value
                val hasPermission = NotificationPermissionHelper.hasNotificationPermission(context)
                val todayKey = StreakManager.getTodayDateKey(now)
                val lastSentKey = prefs.lastStreakReminderSentDateKey.value

                val database = TikoDatabase.getInstance(context)
                val tasks = database.taskDao().getAllTasksRaw()
                val currentStreak = StreakManager.computeCurrentStreak(tasks)
                val taskDoneToday = StreakManager.hasCompletedTaskToday(tasks, todayKey)

                // Re-check conditions at fire time
                val canPost = toggleEnabled &&
                        hasPermission &&
                        currentStreak >= 2 &&
                        !taskDoneToday &&
                        lastSentKey != todayKey

                val reason = when {
                    !hasPermission -> "Notification permission not granted"
                    !toggleEnabled -> "Streak reminders toggle is OFF"
                    currentStreak < 2 -> "Streak is $currentStreak (< 2)"
                    taskDoneToday -> "Task was completed today"
                    lastSentKey == todayKey -> "Reminder already sent today"
                    else -> "All conditions met"
                }

                if (canPost) {
                    StreakReminderScheduler.postNotification(context, currentStreak)
                    prefs.setLastStreakReminderSentDateKey(todayKey)
                    Log.d(
                        "TikoStreakReminder",
                        "Firing decision: todayKey=$todayKey, currentStreak=$currentStreak, taskDoneToday=$taskDoneToday, scheduledTime=$now, action=Posted, reason=$reason"
                    )
                } else {
                    Log.d(
                        "TikoStreakReminder",
                        "Firing decision: todayKey=$todayKey, currentStreak=$currentStreak, taskDoneToday=$taskDoneToday, scheduledTime=$now, action=Skipped, reason=$reason"
                    )
                }

                // Schedule for tomorrow
                StreakReminderScheduler.reschedule(context)
            } catch (e: Exception) {
                Log.w("TikoStreakReminder", "Error in StreakReminderReceiver: ${e.message}", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_STREAK_REMINDER = "com.musab.tiko.action.STREAK_REMINDER"
    }
}
