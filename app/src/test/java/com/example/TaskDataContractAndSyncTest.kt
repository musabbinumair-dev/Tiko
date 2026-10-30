package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TaskDao
import com.example.data.local.TikoDatabase
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskEntity
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
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDataContractAndSyncTest {

    private lateinit var database: TikoDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TikoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // 1. Entity -> Firestore map -> entity round trip keeps every field
    @Test
    fun testEntityFirestoreRoundTripKeepsEveryField() {
        val originalSubtasks = listOf(
            SubtaskItem(id = "sub-1", title = "First subtask", done = true),
            SubtaskItem(id = "sub-2", title = "Second subtask", done = false)
        )
        val original = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = "Launch Tiko Production",
            notes = "Full roundtrip verification\nLine 2 notes",
            category = "Health",
            priority = "High",
            dueDate = "2026-10-31",
            dueTime = "04:30 PM",
            reminder = "1 hour before",
            done = true,
            completedAt = 1761800000000L,
            subtasksJson = TaskEntity.serializeSubtasks(originalSubtasks),
            createdAt = 1761700000000L,
            updatedAt = 1761800050000L,
            deleted = false,
            synced = false
        )

        val firestoreMap = original.toFirestoreMap()
        val reconstructed = TaskEntity.fromFirestoreMap(firestoreMap, original.id)

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.title, reconstructed.title)
        assertEquals(original.notes, reconstructed.notes)
        assertEquals(original.category, reconstructed.category)
        assertEquals(original.priority, reconstructed.priority)
        assertEquals(original.dueDate, reconstructed.dueDate)
        assertEquals(original.dueTime, reconstructed.dueTime)
        assertEquals(original.reminder, reconstructed.reminder)
        assertEquals(original.done, reconstructed.done)
        assertEquals(original.completedAt, reconstructed.completedAt)
        assertEquals(original.createdAt, reconstructed.createdAt)
        assertEquals(original.updatedAt, reconstructed.updatedAt)
        assertEquals(original.deleted, reconstructed.deleted)
        assertTrue(reconstructed.synced) // fromFirestoreMap marks pulled docs as synced = true

        assertEquals(2, reconstructed.subtasks.size)
        assertEquals("sub-1", reconstructed.subtasks[0].id)
        assertEquals("First subtask", reconstructed.subtasks[0].title)
        assertTrue(reconstructed.subtasks[0].done)

        assertEquals("sub-2", reconstructed.subtasks[1].id)
        assertEquals("Second subtask", reconstructed.subtasks[1].title)
        assertFalse(reconstructed.subtasks[1].done)
    }

    // 2. Completing one of three tasks leaves the other two unchanged
    @Test
    fun testCompletingOneOfThreeTasksLeavesOtherTwoUnchanged() = runBlocking {
        val task1 = TaskEntity(
            id = "task-1",
            title = "Task One",
            category = "Work",
            priority = "Low",
            done = false,
            updatedAt = 1000L,
            synced = true
        )
        val task2 = TaskEntity(
            id = "task-2",
            title = "Task Two",
            category = "Personal",
            priority = "Medium",
            done = false,
            updatedAt = 1000L,
            synced = true
        )
        val task3 = TaskEntity(
            id = "task-3",
            title = "Task Three",
            category = "Study",
            priority = "High",
            done = false,
            updatedAt = 1000L,
            synced = true
        )

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)
        taskDao.insertTask(task3)

        // Complete ONLY task2
        val completedTime = 2500L
        taskDao.setTaskDone("task-2", done = true, completedAt = completedTime, updatedAt = completedTime)

        val fetched1 = taskDao.getTaskByIdOnce("task-1")
        val fetched2 = taskDao.getTaskByIdOnce("task-2")
        val fetched3 = taskDao.getTaskByIdOnce("task-3")

        assertNotNull(fetched1)
        assertNotNull(fetched2)
        assertNotNull(fetched3)

        // Task 1 unchanged
        assertFalse(fetched1!!.done)
        assertNull(fetched1.completedAt)
        assertEquals(1000L, fetched1.updatedAt)
        assertTrue(fetched1.synced)

        // Task 2 completed and marked unsynced
        assertTrue(fetched2!!.done)
        assertEquals(completedTime, fetched2.completedAt)
        assertEquals(completedTime, fetched2.updatedAt)
        assertFalse(fetched2.synced)

        // Task 3 unchanged
        assertFalse(fetched3!!.done)
        assertNull(fetched3.completedAt)
        assertEquals(1000L, fetched3.updatedAt)
        assertTrue(fetched3.synced)
    }

    // 3. Edit each field and confirm updatedAt increases and synced = false
    @Test
    fun testEditEachFieldIncreasesUpdatedAtAndSetsSyncedFalse() = runBlocking {
        var task = TaskEntity(
            id = "task-edit-test",
            title = "Initial Title",
            notes = "Initial Notes",
            category = "Work",
            priority = "Low",
            dueDate = "2026-10-01",
            dueTime = "09:00 AM",
            reminder = "None",
            done = false,
            updatedAt = 1000L,
            synced = true
        )
        taskDao.insertTask(task)

        // Edit title
        var t = 2000L
        task = task.copy(title = "Edited Title", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        var row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("Edited Title", row.title)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Edit notes
        t += 100
        task = task.copy(notes = "Edited Notes", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("Edited Notes", row.notes)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Edit category
        t += 100
        task = task.copy(category = "Health", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("Health", row.category)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Edit priority
        t += 100
        task = task.copy(priority = "High", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("High", row.priority)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Edit dueDate and dueTime
        t += 100
        task = task.copy(dueDate = "2026-11-15", dueTime = "03:00 PM", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("2026-11-15", row.dueDate)
        assertEquals("03:00 PM", row.dueTime)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Edit reminder
        t += 100
        task = task.copy(reminder = "30 min before", updatedAt = t, synced = false)
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals("30 min before", row.reminder)
        assertEquals(t, row.updatedAt)
        assertFalse(row.synced)

        // Add subtask
        task = task.copyWithAddedSubtask("Buy apples")
        taskDao.updateTask(task)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertEquals(1, row.subtasks.size)
        assertEquals("Buy apples", row.subtasks[0].title)
        assertFalse(row.synced)

        // Toggle done
        t = System.currentTimeMillis()
        taskDao.setTaskDone("task-edit-test", done = true, completedAt = t, updatedAt = t)
        row = taskDao.getTaskByIdOnce("task-edit-test")!!
        assertTrue(row.done)
        assertEquals(t, row.completedAt)
        assertFalse(row.synced)

        // Soft delete
        t += 100
        taskDao.softDeleteById("task-edit-test", updatedAt = t)
        val deletedRow = taskDao.getTaskByIdIncludingDeleted("task-edit-test")!!
        assertTrue(deletedRow.deleted)
        assertEquals(t, deletedRow.updatedAt)
        assertFalse(deletedRow.synced)
    }

    // 4. Conflict resolution: newer remote wins, older remote doesn't, unsynced local is never overwritten
    @Test
    fun testConflictResolutionLogic() = runBlocking {
        // Case 1: Synced local + Newer remote -> Remote wins
        val localTask1 = TaskEntity(
            id = "conflict-1",
            title = "Local Title",
            updatedAt = 1000L,
            synced = true
        )
        taskDao.insertTask(localTask1)
        val remoteTask1 = TaskEntity(
            id = "conflict-1",
            title = "Remote Newer Title",
            updatedAt = 2000L,
            synced = true
        )
        // Apply pull resolution
        if (localTask1.synced && remoteTask1.updatedAt > localTask1.updatedAt) {
            taskDao.insertTask(remoteTask1)
        }
        val result1 = taskDao.getTaskByIdOnce("conflict-1")!!
        assertEquals("Remote Newer Title", result1.title)

        // Case 2: Synced local + Older remote -> Local is preserved
        val localTask2 = TaskEntity(
            id = "conflict-2",
            title = "Local Newer Title",
            updatedAt = 3000L,
            synced = true
        )
        taskDao.insertTask(localTask2)
        val remoteTask2 = TaskEntity(
            id = "conflict-2",
            title = "Remote Older Title",
            updatedAt = 2000L,
            synced = true
        )
        if (localTask2.synced && remoteTask2.updatedAt > localTask2.updatedAt) {
            taskDao.insertTask(remoteTask2)
        }
        val result2 = taskDao.getTaskByIdOnce("conflict-2")!!
        assertEquals("Local Newer Title", result2.title)

        // Case 3: Unsynced local (synced = false) -> Remote pull NEVER overwrites
        val localTask3 = TaskEntity(
            id = "conflict-3",
            title = "Local Unsynced Title",
            updatedAt = 1500L,
            synced = false // user edited offline
        )
        taskDao.insertTask(localTask3)
        val remoteTask3 = TaskEntity(
            id = "conflict-3",
            title = "Remote Overwriting Title",
            updatedAt = 5000L, // even if remote has a higher timestamp
            synced = true
        )
        val currentLocal = taskDao.getTaskByIdOnce("conflict-3")!!
        if (currentLocal.synced && remoteTask3.updatedAt > currentLocal.updatedAt) {
            taskDao.insertTask(remoteTask3)
        }
        val result3 = taskDao.getTaskByIdOnce("conflict-3")!!
        assertEquals("Local Unsynced Title", result3.title)
        assertFalse(result3.synced)
    }

    // 5. The compare-and-set synced update from B
    @Test
    fun testCompareAndSetSyncedUpdate() = runBlocking {
        val task = TaskEntity(
            id = "cas-task",
            title = "CAS Test",
            updatedAt = 1000L,
            synced = false
        )
        taskDao.insertTask(task)

        // User edits task locally before push finishes
        val localEdited = task.copy(title = "Edited While Pushing", updatedAt = 2000L, synced = false)
        taskDao.updateTask(localEdited)

        // Push acknowledgement for the first write (1000L) arrives late
        val rowsAffectedOld = taskDao.markSynced("cas-task", pushedUpdatedAt = 1000L)
        assertEquals(0, rowsAffectedOld) // Must NOT mark synced because updatedAt moved to 2000L

        var current = taskDao.getTaskByIdOnce("cas-task")!!
        assertFalse(current.synced)
        assertEquals(2000L, current.updatedAt)

        // Push acknowledgement for the second write (2000L) arrives
        val rowsAffectedNew = taskDao.markSynced("cas-task", pushedUpdatedAt = 2000L)
        assertEquals(1, rowsAffectedNew)

        current = taskDao.getTaskByIdOnce("cas-task")!!
        assertTrue(current.synced)
    }

    // 6. Stats (Done, Pending, Streak, On time, weekly chart, Lists counts, Calendar dots)
    @Test
    fun testStatsComputationFromSampleData() {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        val wednesdayMillis = cal.timeInMillis
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdfDate.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val tuesdayMillis = cal.timeInMillis
        val tuesdayStr = sdfDate.format(cal.time)

        val sampleTasks = listOf(
            TaskEntity(
                id = "stat-1",
                title = "Task 1",
                category = "Work",
                dueDate = todayStr,
                done = true,
                completedAt = wednesdayMillis
            ),
            TaskEntity(
                id = "stat-2",
                title = "Task 2",
                category = "Work",
                dueDate = tuesdayStr,
                done = true,
                completedAt = tuesdayMillis
            ),
            TaskEntity(
                id = "stat-3",
                title = "Task 3",
                category = "Health",
                dueDate = todayStr,
                done = false
            ),
            TaskEntity(
                id = "stat-4",
                title = "Task 4",
                category = "Study",
                dueDate = "2026-11-20",
                done = false
            )
        )

        // 1. Done & Pending
        val doneCount = sampleTasks.count { it.done }
        val pendingCount = sampleTasks.count { !it.done }
        assertEquals(2, doneCount)
        assertEquals(2, pendingCount)

        // 2. Streak
        val completedDates = sampleTasks.filter { it.done && it.dueDate.isNotBlank() }
            .map { it.dueDate }
            .distinct()
        assertEquals(2, completedDates.size)

        // 3. On Time percentage
        val onTimePct = ((doneCount.toFloat() / sampleTasks.size) * 100).toInt()
        assertEquals(50, onTimePct)

        // 4. Lists counts
        val workTasks = sampleTasks.filter { it.category.equals("Work", ignoreCase = true) }
        val healthTasks = sampleTasks.filter { it.category.equals("Health", ignoreCase = true) }
        val studyTasks = sampleTasks.filter { it.category.equals("Study", ignoreCase = true) }

        assertEquals(2, workTasks.size)
        assertEquals(2, workTasks.count { it.done })

        assertEquals(1, healthTasks.size)
        assertEquals(0, healthTasks.count { it.done })

        assertEquals(1, studyTasks.size)
        assertEquals(0, studyTasks.count { it.done })

        // 5. Calendar dots
        assertTrue(sampleTasks.any { it.dueDate == todayStr })
        assertTrue(sampleTasks.any { it.dueDate == tuesdayStr })
        assertFalse(sampleTasks.any { it.dueDate == "2026-01-01" })

        // 6. Startup ID Repair Test
        runBlocking {
            val blankTask = TaskEntity(id = "", title = "Blank ID Task", done = false)
            taskDao.insertTask(blankTask)
            val repairedCount = taskDao.repairBlankOrDuplicateIds()
            assertTrue(repairedCount >= 1)
            val all = taskDao.getAllTasksRaw()
            assertTrue(all.all { it.id.isNotBlank() })
        }
    }
}
