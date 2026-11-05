package com.example

import com.example.data.model.TaskEntity
import com.example.data.streak.StreakData
import com.example.data.streak.StreakManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakManagerTest {

    // 1. First ever completion (1)
    @Test
    fun testFirstEverCompletion() {
        val initialData = StreakData(
            currentStreak = 0,
            longestStreak = 0,
            lastCompletedDay = "",
            streakDays = emptySet()
        )
        val todayKey = "2026-11-04"
        val result = StreakManager.onTaskCompleted(initialData, completionDateKey = todayKey, todayKey = todayKey)

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
        assertEquals("2026-11-04", result.lastCompletedDay)
        assertTrue(result.streakDays.contains("2026-11-04"))
    }

    // 2. Two days in a row (2)
    @Test
    fun testTwoDaysInARow() {
        val day1Key = "2026-11-03"
        val day2Key = "2026-11-04"

        val day1Data = StreakManager.onTaskCompleted(StreakData(), day1Key, day1Key)
        assertEquals(1, day1Data.currentStreak)

        val day2Data = StreakManager.onTaskCompleted(day1Data, day2Key, day2Key)
        assertEquals(2, day2Data.currentStreak)
        assertEquals(2, day2Data.longestStreak)
        assertEquals(day2Key, day2Data.lastCompletedDay)
        assertTrue(day2Data.streakDays.containsAll(setOf(day1Key, day2Key)))
    }

    // 3. A missed day (resets to 1 when the next completion happens)
    @Test
    fun testMissedDayResetsToOne() {
        val day1Key = "2026-11-01"
        // 2026-11-02 was missed
        val day3Key = "2026-11-03"

        val day1Data = StreakManager.onTaskCompleted(StreakData(), day1Key, day1Key)
        assertEquals(1, day1Data.currentStreak)

        val day3Data = StreakManager.onTaskCompleted(day1Data, day3Key, day3Key)
        assertEquals(1, day3Data.currentStreak)
        assertEquals(1, day3Data.longestStreak)
        assertEquals(day3Key, day3Data.lastCompletedDay)
    }

    // 4. Two completions on the same day (no change)
    @Test
    fun testTwoCompletionsOnSameDayNoChange() {
        val todayKey = "2026-11-04"
        val firstCompletion = StreakManager.onTaskCompleted(StreakData(), todayKey, todayKey)
        assertEquals(1, firstCompletion.currentStreak)

        val secondCompletion = StreakManager.onTaskCompleted(firstCompletion, todayKey, todayKey)
        assertEquals(1, secondCompletion.currentStreak)
        assertEquals(1, secondCompletion.longestStreak)
        assertEquals(todayKey, secondCompletion.lastCompletedDay)
        assertEquals(1, secondCompletion.streakDays.size)
    }

    // 5. Undo the only completion today (streak goes back to yesterday's value)
    @Test
    fun testUndoOnlyCompletionToday() {
        val yesterdayKey = "2026-11-03"
        val todayKey = "2026-11-04"

        val day1Epoch = LocalDate.parse(yesterdayKey).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3600000L
        val day2Epoch = LocalDate.parse(todayKey).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3600000L

        val day1Task = TaskEntity(id = "task-1", title = "Task 1", done = true, completedAt = day1Epoch, dueDate = yesterdayKey)
        val day2Task = TaskEntity(id = "task-2", title = "Task 2", done = true, completedAt = day2Epoch, dueDate = todayKey)

        var streak = StreakManager.onTaskCompleted(StreakData(), yesterdayKey, yesterdayKey)
        streak = StreakManager.onTaskCompleted(streak, todayKey, todayKey)
        assertEquals(2, streak.currentStreak)

        // Undo task-2 today; remaining done tasks only contains day1Task
        val afterUndo = StreakManager.onTaskUndoneOrDeleted(
            currentData = streak,
            remainingDoneTasks = listOf(day1Task),
            todayKey = todayKey
        )

        assertEquals(1, afterUndo.currentStreak)
        assertEquals(yesterdayKey, afterUndo.lastCompletedDay)
        assertEquals(setOf(yesterdayKey), afterUndo.streakDays)
    }

    // 6. A restart the next day with no activity (streak shows 0 while keeping longestStreak)
    @Test
    fun testRestartNextDayWithNoActivityShowsZero() {
        val day1Key = "2026-11-01"
        val day3Key = "2026-11-03" // Day 2 (2026-11-02) missed, now day 3 with no activity

        val day1Data = StreakManager.onTaskCompleted(StreakData(), day1Key, day1Key)
        assertEquals(1, day1Data.currentStreak)
        assertEquals(1, day1Data.longestStreak)

        // App opened on day 3
        val launchCheck = StreakManager.checkAndRecalculateOnLaunch(day1Data, todayKey = day3Key)
        assertEquals(0, launchCheck.currentStreak) // Streak broken
        assertEquals(1, launchCheck.longestStreak) // Kept longest
        assertEquals(day1Key, launchCheck.lastCompletedDay)
    }

    // 7. A midnight crossing
    @Test
    fun testMidnightCrossing() {
        val day1Key = "2026-11-04"
        val day2Key = "2026-11-05"

        // User completed a task on day 1
        val beforeMidnight = StreakManager.onTaskCompleted(StreakData(), day1Key, day1Key)
        assertEquals(1, beforeMidnight.currentStreak)
        assertEquals(day1Key, beforeMidnight.lastCompletedDay)

        // Clock ticks past midnight to day 2 (no activity yet today)
        val afterMidnight = StreakManager.checkAndRecalculateOnLaunch(beforeMidnight, todayKey = day2Key)
        // Streak is preserved through yesterday so user has all of day 2 to extend it
        assertEquals(1, afterMidnight.currentStreak)
        assertEquals(day1Key, afterMidnight.lastCompletedDay)
    }

    // 8. A time zone change
    @Test
    fun testTimeZoneChange() {
        val zoneUtc = ZoneId.of("UTC")
        val zoneNy = ZoneId.of("America/New_York") // UTC-5

        // Fixed instant: 2026-11-05 02:00:00 UTC
        // In UTC, this is "2026-11-05"
        // In New York, this is 2026-11-04 21:00:00 ("2026-11-04")
        val epochMillis = 1793844000000L // arbitrary fixed epoch

        val dateKeyUtc = StreakManager.timestampToDateKey(epochMillis, zoneUtc)
        val dateKeyNy = StreakManager.timestampToDateKey(epochMillis, zoneNy)

        // Verify timezone key resolution
        val localDateUtc = Instant.ofEpochMilli(epochMillis).atZone(zoneUtc).toLocalDate()
        val localDateNy = Instant.ofEpochMilli(epochMillis).atZone(zoneNy).toLocalDate()
        assertEquals(dateKeyUtc, localDateUtc.toString())
        assertEquals(dateKeyNy, localDateNy.toString())

        // User completed task in New York on 2026-11-04
        val nyData = StreakManager.onTaskCompleted(StreakData(), "2026-11-04", "2026-11-04")
        assertEquals(1, nyData.currentStreak)

        // Then completes next task next day on 2026-11-05
        val nextDayData = StreakManager.onTaskCompleted(nyData, "2026-11-05", "2026-11-05")
        assertEquals(2, nextDayData.currentStreak)
    }

    // 9. Sync from another device with a higher longestStreak
    @Test
    fun testSyncFromAnotherDeviceWithHigherLongestStreak() {
        val todayKey = "2026-11-04"
        val localData = StreakData(
            currentStreak = 1,
            longestStreak = 3,
            lastCompletedDay = "2026-11-04",
            streakDays = setOf("2026-11-04")
        )

        val remoteData = StreakData(
            currentStreak = 2,
            longestStreak = 14,
            lastCompletedDay = "2026-11-04",
            streakDays = setOf("2026-11-03", "2026-11-04")
        )

        val merged = StreakManager.mergeRemoteStreak(localData, remoteData, todayKey)

        assertEquals(2, merged.currentStreak)
        assertEquals(14, merged.longestStreak) // Kept highest longestStreak
        assertTrue(merged.streakDays.containsAll(setOf("2026-11-03", "2026-11-04")))
    }
}
