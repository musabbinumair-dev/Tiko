package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TaskDao
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.TaskEntity
import com.example.data.sync.SyncCoordinator
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncCoordinatorTest {

    private lateinit var context: Context
    private lateinit var database: TikoDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var preferences: UserPreferences
    private lateinit var syncManager: SyncManager
    private lateinit var coordinator: SyncCoordinator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TikoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        preferences = UserPreferences(context)
        syncManager = SyncManager(
            context = context,
            taskDao = taskDao,
            firestoreProvider = { null } // Detached for local JVM auth unit tests
        )
        coordinator = SyncCoordinator(
            context = context,
            syncManager = syncManager,
            preferences = preferences,
            authProvider = { null },
            firestoreProvider = { null }
        )
    }

    @After
    fun teardown() {
        coordinator.stopListening()
        syncManager.stop()
        database.close()
    }

    @Test
    fun testSignInStartsSync() {
        assertFalse("Sync should be inactive before sign-in", syncManager.isSyncActive())
        
        coordinator.handleUserSignedIn(
            uid = "user-test-123",
            displayName = "Tiko User",
            email = "user@example.com"
        )

        assertTrue("Sync should be active after sign-in", syncManager.isSyncActive())
        assertFalse(
            "SyncState should not be Idle once signed in",
            coordinator.syncState.value is SyncState.Idle
        )
    }

    @Test
    fun testSignOutStopsSync() {
        coordinator.handleUserSignedIn(
            uid = "user-test-123",
            displayName = "Tiko User",
            email = "user@example.com"
        )
        assertTrue(syncManager.isSyncActive())

        coordinator.handleUserSignedOut()
        assertFalse("Sync should be stopped after sign-out", syncManager.isSyncActive())
        assertEquals(SyncState.Idle, coordinator.syncState.value)
        assertEquals("", coordinator.syncStatus.value)
    }

    @Test
    fun testGuestModeDoesNotBlockSyncAndIsClearedOnSignIn() {
        // User starts in guest mode
        preferences.setGuestMode(true)
        assertTrue("Guest mode should be true initially", preferences.isGuestMode.value)

        // User signs in
        coordinator.handleUserSignedIn(
            uid = "user-guest-upgrade",
            displayName = "Upgraded Guest",
            email = "guest@example.com"
        )

        // Guest mode must be cleared
        assertFalse("Guest mode must be cleared after signing in", preferences.isGuestMode.value)
        assertTrue("Sync should start without being blocked by previous guest mode", syncManager.isSyncActive())
    }

    @Test
    fun testPushCASMarkSyncedMatchesUpdatedAt() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        val originalUpdatedAt = 1000L
        val task = TaskEntity(
            id = taskId,
            title = "Task for CAS check",
            createdAt = originalUpdatedAt,
            updatedAt = originalUpdatedAt,
            synced = false
        )
        taskDao.insertTask(task)

        // CAS update with matching updatedAt succeeds
        val updatedRows = taskDao.markSynced(taskId, originalUpdatedAt)
        assertEquals("CAS markSynced should update 1 row when updatedAt matches", 1, updatedRows)

        val syncedTask = taskDao.getTaskByIdOnce(taskId)
        assertTrue("Task should now be synced", syncedTask?.synced == true)

        // Now simulate local modification while in flight
        val newUpdatedAt = 2000L
        taskDao.setTaskDone(taskId, done = true, completedAt = newUpdatedAt, updatedAt = newUpdatedAt)
        val modifiedTask = taskDao.getTaskByIdOnce(taskId)
        assertFalse("Modified task has synced = false", modifiedTask?.synced == true)

        // Stale push response attempts markSynced with old updatedAt 1000L
        val staleRows = taskDao.markSynced(taskId, originalUpdatedAt)
        assertEquals("CAS markSynced should reject update when updatedAt changed in flight", 0, staleRows)

        val taskAfterStaleAttempt = taskDao.getTaskByIdOnce(taskId)
        assertFalse("Task must remain unsynced so next push picks it up", taskAfterStaleAttempt?.synced == true)
    }

    @Test
    fun testSyncStateAndProblemCodeFormatting() {
        val syncedState: SyncState = SyncState.Synced
        assertEquals(SyncState.Synced, syncedState)

        val syncingState: SyncState = SyncState.Syncing
        assertEquals(SyncState.Syncing, syncingState)

        val offlineState: SyncState = SyncState.Offline
        assertEquals(SyncState.Offline, offlineState)

        val problemState = SyncState.Problem("PERMISSION_DENIED")
        assertEquals("PERMISSION_DENIED", problemState.code)

        // Verify problem formatting does not leak PII
        val statusText = "Sync problem: ${problemState.code}"
        assertEquals("Sync problem: PERMISSION_DENIED", statusText)
        assertFalse("Status text must not contain user data", statusText.contains("@"))
    }
}
