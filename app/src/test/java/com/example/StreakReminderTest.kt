package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.TaskEntity
import com.example.data.reminder.NotificationCopy
import com.example.data.reminder.StreakManager
import com.example.data.reminder.StreakReminderDecision
import com.example.data.reminder.StreakReminderScheduler
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreakReminderTest {

    private lateinit var context: Context
    private lateinit var database: TikoDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = TikoDatabase.getInstance(context)
        preferences = UserPreferences(context)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)

        runBlocking {
            val tasks = database.taskDao().getAllTasksRaw()
            for (t in tasks) {
                database.taskDao().deleteTask(t)
            }
        }
    }

    @After
    fun tearDown() {
        shadowAlarmManager.scheduledAlarms.clear()
    }

    @Test
    fun testStreak1NoReminder() {
        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = 1,
            hasCompletedTaskToday = false,
            reminderTime = "8:00 PM"
        )
        assertTrue(decision is StreakReminderDecision.Skip)
        assertTrue((decision as StreakReminderDecision.Skip).reason.contains("Streak is 1"))
    }

    @Test
    fun testStreak2WithNoTaskTodaySchedulesReminder() {
        val timeZone = TimeZone.getDefault()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            this.timeZone = timeZone
        }
        val nowMillis = sdf.parse("2026-11-05 14:00:00")!!.time
        val todayKey = "2026-11-05"

        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = 2,
            hasCompletedTaskToday = false,
            reminderTime = "8:00 PM",
            nowMillis = nowMillis,
            todayDateKey = todayKey,
            timeZone = timeZone
        )

        assertTrue(decision is StreakReminderDecision.ScheduleToday)
        val expectedTrigger = sdf.parse("2026-11-05 20:00:00")!!.time
        assertEquals(expectedTrigger, (decision as StreakReminderDecision.ScheduleToday).triggerMillis)
    }

    @Test
    fun testStreak2WithTaskCompletedTodayNoReminder() {
        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = 2,
            hasCompletedTaskToday = true,
            reminderTime = "8:00 PM"
        )
        assertTrue(decision is StreakReminderDecision.Skip)
        assertTrue((decision as StreakReminderDecision.Skip).reason.contains("Task already completed today"))
    }

    @Test
    fun testToggleOffNoReminder() {
        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = false,
            currentStreak = 5,
            hasCompletedTaskToday = false,
            reminderTime = "8:00 PM"
        )
        assertTrue(decision is StreakReminderDecision.Skip)
        assertTrue((decision as StreakReminderDecision.Skip).reason.contains("toggle is OFF"))
    }

    @Test
    fun testTimeAlreadyPassedTodaySchedulesNextDay() {
        val timeZone = TimeZone.getDefault()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            this.timeZone = timeZone
        }
        val nowMillis = sdf.parse("2026-11-05 21:00:00")!!.time
        val todayKey = "2026-11-05"

        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = 3,
            hasCompletedTaskToday = false,
            reminderTime = "8:00 PM",
            nowMillis = nowMillis,
            todayDateKey = todayKey,
            timeZone = timeZone
        )

        assertTrue(decision is StreakReminderDecision.ScheduleTomorrow)
        val expectedTomorrowTrigger = sdf.parse("2026-11-06 20:00:00")!!.time
        assertEquals(expectedTomorrowTrigger, (decision as StreakReminderDecision.ScheduleTomorrow).triggerMillis)
    }

    @Test
    fun testReminderAlreadySentTodayNoSecondOne() {
        val todayKey = "2026-11-05"
        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = 4,
            hasCompletedTaskToday = false,
            reminderTime = "8:00 PM",
            lastSentDateKey = todayKey,
            todayDateKey = todayKey
        )
        assertTrue(decision is StreakReminderDecision.Skip)
        assertTrue((decision as StreakReminderDecision.Skip).reason.contains("already sent today"))
    }

    @Test
    fun testCompletionAfterSchedulingCancelsStreakReminder() = runBlocking {
        preferences.setStreakRemindersEnabled(true)
        val repository = TaskRepository(context, database.taskDao())

        val task1 = TaskEntity(
            id = "task-1",
            title = "Task Day 1",
            dueDate = "2026-11-04",
            done = true,
            completedAt = System.currentTimeMillis() - 86400000L
        )
        val task2 = TaskEntity(
            id = "task-2",
            title = "Task Day 2",
            dueDate = "2026-11-05",
            done = false
        )
        repository.insertTask(task1)
        repository.insertTask(task2)

        val tasks = database.taskDao().getAllTasksRaw()
        val streak = StreakManager.computeCurrentStreak(tasks)
        assertTrue(streak >= 1)

        // Complete task2 today
        repository.toggleTaskDone(task2)

        val updatedTasks = database.taskDao().getAllTasksRaw()
        val todayKey = StreakManager.getTodayDateKey()
        val hasCompletedToday = StreakManager.hasCompletedTaskToday(updatedTasks, todayKey)
        assertTrue(hasCompletedToday)

        val decision = StreakReminderScheduler.evaluateDecision(
            toggleEnabled = true,
            currentStreak = StreakManager.computeCurrentStreak(updatedTasks),
            hasCompletedTaskToday = hasCompletedToday,
            reminderTime = "8:00 PM"
        )
        assertTrue(decision is StreakReminderDecision.Skip)
    }

    @Test
    fun testStreakReminderCopyPool() {
        val title = NotificationCopy.getStreakReminderTitle(5)
        assertTrue(title.contains("5-day streak"))
        val allowedTitles = listOf(
            "Your 5-day streak is waiting 🔥",
            "Keep your 5-day streak alive ✨",
            "Don't lose your 5-day streak 🌟"
        )
        assertTrue(allowedTitles.contains(title))
        assertEquals("Finish one task to keep it going.", NotificationCopy.STREAK_REMINDER_BODY)
    }
}
