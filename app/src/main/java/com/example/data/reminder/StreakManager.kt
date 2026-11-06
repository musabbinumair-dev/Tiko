package com.example.data.reminder

import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object StreakManager {

    fun getTodayDateKey(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(nowMillis))
    }

    /**
     * Streak calculation matching Tiko's rules:
     * Distinct completion dates among completed, non-deleted tasks.
     * If there are completed tasks, streak is at least 1.
     */
    fun computeCurrentStreak(tasks: List<TaskEntity>): Int {
        val doneCount = tasks.count { it.done && !it.deleted }
        if (doneCount == 0) return 0
        val completedDates = tasks.filter { it.done && !it.deleted && it.dueDate.isNotBlank() }
            .map { it.dueDate }
            .distinct()
        return completedDates.size.coerceAtLeast(1)
    }

    /**
     * Checks if the user has completed a task today using the date key.
     * A task counts as completed today if it is done, not deleted, and:
     * 1) its dueDate matches todayKey, or
     * 2) its completedAt timestamp falls on todayKey.
     */
    fun hasCompletedTaskToday(
        tasks: List<TaskEntity>,
        todayKey: String = getTodayDateKey(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        return tasks.any { task ->
            if (!task.done || task.deleted) return@any false
            if (task.dueDate == todayKey) return@any true
            val completedAt = task.completedAt
            if (completedAt != null && completedAt > 0) {
                val completedDateKey = getTodayDateKey(completedAt, timeZone)
                if (completedDateKey == todayKey) return@any true
            }
            false
        }
    }
}
