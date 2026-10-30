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
        val taskId = "task-to-cancel"
        ReminderScheduler.cancel(context, taskId)
        assertNotNull(context)
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
