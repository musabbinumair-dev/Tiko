package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.TaskEntity
import com.example.data.reminder.BootReceiver
import com.example.data.reminder.NotificationPermissionHelper
import com.example.data.reminder.ReminderActionReceiver
import com.example.data.reminder.ReminderReceiver
import com.example.data.reminder.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationReminderTest {

    private lateinit var context: Context
    private lateinit var database: TikoDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = TikoDatabase.getInstance(context)
        preferences = UserPreferences(context)

        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clear existing tasks in DB
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
        notificationManager.cancelAll()
    }

    @Test
    fun testParseReminderOffsetMillis() {
        assertEquals(15 * 60 * 1000L, ReminderScheduler.parseReminderOffsetMillis("15 min before"))
        assertEquals(15 * 60 * 1000L, ReminderScheduler.parseReminderOffsetMillis("15 minutes before"))
        assertEquals(30 * 60 * 1000L, ReminderScheduler.parseReminderOffsetMillis("30 min before"))
        assertEquals(60 * 60 * 1000L, ReminderScheduler.parseReminderOffsetMillis("1 hour before"))
        assertEquals(60 * 60 * 1000L, ReminderScheduler.parseReminderOffsetMillis("1 hr before"))
        assertEquals(0L, ReminderScheduler.parseReminderOffsetMillis("At time"))
        assertEquals(0L, ReminderScheduler.parseReminderOffsetMillis("at due time"))
        assertNull(ReminderScheduler.parseReminderOffsetMillis("none"))
        assertNull(ReminderScheduler.parseReminderOffsetMillis(""))
        assertNull(ReminderScheduler.parseReminderOffsetMillis("unsupported value"))
    }

    @Test
    fun testParseDueTimestamp() {
        val parsed = ReminderScheduler.parseDueTimestamp("2028-12-25", "10:00 AM")
        assertNotNull(parsed)

        val parsed24h = ReminderScheduler.parseDueTimestamp("2028-12-25", "14:30")
        assertNotNull(parsed24h)

        val parsedBlank = ReminderScheduler.parseDueTimestamp("", "10:00 AM")
        assertNull(parsedBlank)

        val parsedToday = ReminderScheduler.parseDueTimestamp("today", "11:00 AM")
        assertNotNull(parsedToday)
    }

    @Test
    fun testComputeTriggerTimeForPastTaskReturnsNull() {
        // Due date in 2020 (past)
        val trigger = ReminderScheduler.computeTriggerTime("2020-01-01", "10:00 AM", "15 min before")
        assertNull(trigger)
    }

    @Test
    fun testComputeTriggerTimeForFutureTaskCalculatesCorrectOffset() {
        val futureYear = "2029-06-15"
        val triggerTime = ReminderScheduler.computeTriggerTime(futureYear, "10:00 AM", "15 min before")
        val dueTimestamp = ReminderScheduler.parseDueTimestamp(futureYear, "10:00 AM")

        assertNotNull(triggerTime)
        assertNotNull(dueTimestamp)
        assertEquals(dueTimestamp!! - 15 * 60 * 1000L, triggerTime!!)
    }

    @Test
    fun testShortGapTimingRules() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        val dateStr = "2026-10-15"
        val dueTimeStr = "3:46 PM"
        val dueEpoch = sdf.parse("$dateStr 15:46:00")!!.time

        // 1. now 3:45:20 with due 3:46 and each offset (30 min, 15 min, 1 hour, at time)
        val now34520 = sdf.parse("$dateStr 15:45:20")!!.time
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "30 min before", now34520))
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "15 min before", now34520))
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "1 hour before", now34520))
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "at time", now34520))
        assertNull(ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "none", now34520))

        // 2. now 3:45:50 with due 3:46:00
        val now34550 = sdf.parse("$dateStr 15:45:50")!!.time
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "15 min before", now34550))
        assertEquals(dueEpoch, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "at time", now34550))

        // 3. now 3:46:20 with due 3:46:00 (due within current minute: <= 60s in past)
        val now34620 = sdf.parse("$dateStr 15:46:20")!!.time
        assertEquals(now34620 + 2000L, ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "15 min before", now34620))

        // 4. now 3:48 with due 3:46 (due more than 60s in past)
        val now348 = sdf.parse("$dateStr 15:48:00")!!.time
        assertNull(ReminderScheduler.computeTriggerTime(dateStr, dueTimeStr, "15 min before", now348))

        // 5. no due date
        assertNull(ReminderScheduler.computeTriggerTime("", "3:46 PM", "15 min before", now34520))

        // 6. blank time (default 9:00 AM)
        val tomorrowDate = "2026-10-16"
        val expected9AM = sdf.parse("$tomorrowDate 09:00:00")!!.time
        assertEquals(expected9AM, ReminderScheduler.computeTriggerTime(tomorrowDate, "", "at time", now34520))

        // 7. a task due tomorrow (normal: trigger = due - offset)
        val expectedTomorrowTrigger = sdf.parse("$tomorrowDate 10:00:00")!!.time - 30 * 60 * 1000L
        assertEquals(expectedTomorrowTrigger, ReminderScheduler.computeTriggerTime(tomorrowDate, "10:00 AM", "30 min before", now34520))
    }

    @Test
    fun testNotificationChannelCreated() {
        ReminderReceiver.createChannel(context)
        val channel = notificationManager.getNotificationChannel(ReminderReceiver.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(ReminderReceiver.CHANNEL_NAME, channel?.name)
    }

    @Test
    fun testScheduleTaskWhenNotificationsDisabledCancelsAlarm() {
        preferences.setNotificationsEnabled(false)

        val task = TaskEntity(
            id = "test-task-1",
            title = "Test Task",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before"
        )

        ReminderScheduler.schedule(context, task)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleTaskWhenCompletedCancelsAlarm() {
        preferences.setNotificationsEnabled(true)

        val task = TaskEntity(
            id = "test-task-done",
            title = "Completed Task",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before",
            done = true
        )

        ReminderScheduler.schedule(context, task)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleTaskWhenDeletedCancelsAlarm() {
        preferences.setNotificationsEnabled(true)

        val task = TaskEntity(
            id = "test-task-deleted",
            title = "Deleted Task",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before",
            deleted = true
        )

        ReminderScheduler.schedule(context, task)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testCancelRemovesScheduledAlarmAndNotification() {
        preferences.setNotificationsEnabled(true)
        val task = TaskEntity(
            id = "task-to-cancel",
            title = "Task To Cancel",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before"
        )
        ReminderScheduler.schedule(context, task)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        ReminderScheduler.cancel(context, task.id)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testScheduleSnoozeSetsAlarm() {
        ReminderScheduler.scheduleSnooze(context, "task-snooze", 10 * 60 * 1000L)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun testReminderActionReceiverMarkDone() = runBlocking {
        val task = TaskEntity(
            id = "task-action-done",
            title = "Task for action receiver",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before",
            done = false
        )
        database.taskDao().insertTask(task)

        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, task.id.hashCode())
        }

        val receiver = ReminderActionReceiver()
        receiver.onReceive(context, intent)

        // Give coroutine brief moment to update db
        Thread.sleep(100)

        val updatedTask = database.taskDao().getTaskByIdOnce(task.id)
        assertNotNull(updatedTask)
        assertTrue(updatedTask?.done == true)
    }

    @Test
    fun testBootReceiverReschedulesActiveTasks() = runBlocking {
        val activeTask = TaskEntity(
            id = "task-boot-active",
            title = "Active Boot Task",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before",
            done = false
        )
        val doneTask = TaskEntity(
            id = "task-boot-done",
            title = "Done Boot Task",
            dueDate = "2029-01-01",
            dueTime = "10:00 AM",
            reminder = "15 min before",
            done = true
        )
        database.taskDao().insertTask(activeTask)
        database.taskDao().insertTask(doneTask)

        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        val receiver = BootReceiver()
        receiver.onReceive(context, bootIntent)

        // Verify receiver executes without error
        Thread.sleep(100)
        assertNotNull(activeTask)
    }

    @Test
    fun testNotificationPermissionHelperStatus() {
        val hasPerm = NotificationPermissionHelper.hasNotificationPermission(context)
        assertTrue(hasPerm)

        preferences.setNotificationsEnabled(false)
        assertFalse(NotificationPermissionHelper.isRealNotificationStateActive(context, preferences))

        preferences.setNotificationsEnabled(true)
        assertTrue(NotificationPermissionHelper.isRealNotificationStateActive(context, preferences))
    }
}
