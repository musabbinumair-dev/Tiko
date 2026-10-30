package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.musab.tiko.BuildConfig
import com.example.data.FirebaseInitializer
import com.example.data.local.TaskDao
import com.example.data.model.TaskEntity
import com.example.data.reminder.ReminderScheduler
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SyncState {
    data object Idle : SyncState()
    data object Synced : SyncState()
    data object Syncing : SyncState()
    data object Offline : SyncState()
    data class Problem(val code: String) : SyncState()
}

class SyncManager(
    private val context: Context,
    private val taskDao: TaskDao,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            FirebaseInitializer.init(context)
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Throwable) {
                null
            }
        }
    }
) {
    constructor(context: Context, taskDao: TaskDao, firestore: FirebaseFirestore) : this(
        context = context,
        taskDao = taskDao,
        firestoreProvider = { firestore }
    )

    private var syncScope: CoroutineScope? = null
    private var snapshotRegistration: ListenerRegistration? = null
    private var pushJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    private val _hasPendingWrites = MutableStateFlow(false)
    private val _lastErrorCode = MutableStateFlow<String?>(null)

    private var currentUserId: String? = null

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val initiallyOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = initiallyOnline

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                updateSyncState()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                updateSyncState()
            }
        }
        networkCallback = callback
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // Ignore callback registration failure
        }
    }

    private fun updateSyncState() {
        val userId = currentUserId
        if (userId == null) {
            _syncState.value = SyncState.Idle
            _syncStatus.value = ""
            return
        }
        val errorCode = _lastErrorCode.value
        if (errorCode != null) {
            _syncState.value = SyncState.Problem(errorCode)
            _syncStatus.value = "Sync problem: $errorCode"
            return
        }
        if (!_isOnline.value) {
            _syncState.value = SyncState.Offline
            _syncStatus.value = "Offline"
            return
        }
        if (_hasPendingWrites.value) {
            _syncState.value = SyncState.Syncing
            _syncStatus.value = "Syncing..."
            return
        }
        _syncState.value = SyncState.Synced
        _syncStatus.value = "Synced"
    }

    fun isSyncActive(): Boolean = currentUserId != null && syncScope != null

    /**
     * Start SyncManager for the signed-in user.
     * Starts push observer and pull snapshot listener.
     */
    fun start(userId: String, isFirstPullAfterLogin: Boolean = false) {
        if (currentUserId == userId && syncScope != null) {
            if (BuildConfig.DEBUG) {
                Log.d("TikoSync", "Sync already active for user; keeping existing session")
            }
            return
        }
        stop()

        if (BuildConfig.DEBUG) {
            Log.d("TikoSync", "Starting sync session")
        }

        currentUserId = userId
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        syncScope = scope

        _lastErrorCode.value = null
        _hasPendingWrites.value = true
        updateSyncState()

        // Start Push observer
        startPushObserver(userId, scope)

        // Start Pull snapshot listener
        startPullListener(userId, scope, isFirstPullAfterLogin)
    }

    private fun startPushObserver(userId: String, scope: CoroutineScope) {
        pushJob = scope.launch {
            val firestore = firestoreProvider()
            if (firestore == null) {
                if (BuildConfig.DEBUG) {
                    Log.e("TikoSync", "Firestore unavailable in push observer")
                }
                _lastErrorCode.value = "UNAVAILABLE"
                updateSyncState()
                return@launch
            }
            val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
            taskDao.getUnsyncedTasks().collect { unsyncedTasks ->
                if (unsyncedTasks.isNotEmpty()) {
                    _hasPendingWrites.value = true
                    updateSyncState()
                } else {
                    _hasPendingWrites.value = false
                    updateSyncState()
                }

                if (BuildConfig.DEBUG && unsyncedTasks.isNotEmpty()) {
                    Log.d("TikoSync", "Push observer processing ${unsyncedTasks.size} unsynced tasks")
                }

                for (task in unsyncedTasks) {
                    val pushedUpdatedAt = task.updatedAt
                    val taskId = task.id
                    tasksCollection.document(taskId).set(task.toFirestoreMap())
                        .addOnSuccessListener {
                            if (BuildConfig.DEBUG) {
                                Log.d("TikoSync", "Push write succeeded for task, performing CAS markSynced")
                            }
                            scope.launch {
                                val rows = taskDao.markSynced(taskId, pushedUpdatedAt)
                                if (BuildConfig.DEBUG) {
                                    Log.d("TikoSync", "CAS markSynced updated rows: $rows")
                                }
                                val remaining = taskDao.getUnsyncedTasksOnce()
                                _hasPendingWrites.value = remaining.isNotEmpty()
                                if (remaining.isEmpty()) {
                                    _lastErrorCode.value = null
                                }
                                updateSyncState()
                            }
                        }
                        .addOnFailureListener { e ->
                            val code = (e as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
                            if (BuildConfig.DEBUG) {
                                Log.e("TikoSync", "Push write failed for task: code=$code")
                            }
                            _lastErrorCode.value = code
                            updateSyncState()
                        }
                }
            }
        }
    }

    private fun startPullListener(userId: String, scope: CoroutineScope, isFirstPullAfterLogin: Boolean) {
        val firestore = firestoreProvider()
        if (firestore == null) {
            _lastErrorCode.value = "UNAVAILABLE"
            updateSyncState()
            return
        }
        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        var isFirstSnapshot = true

        snapshotRegistration = tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val code = error.code.name
                if (BuildConfig.DEBUG) {
                    Log.e("TikoSync", "Pull snapshotListener error: $code")
                }
                val isAuthError = error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ||
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                
                val isNetworkError = error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    error.message?.contains("offline", ignoreCase = true) == true ||
                    error.message?.contains("network", ignoreCase = true) == true ||
                    !_isOnline.value

                if (isAuthError) {
                    _lastErrorCode.value = code
                    updateSyncState()
                    stop()
                    AppToastController.show(
                        "Please log in again",
                        ToastType.Info
                    )
                } else if (isNetworkError) {
                    _isOnline.value = false
                    updateSyncState()
                } else {
                    _lastErrorCode.value = code
                    updateSyncState()
                    if (isFirstPullAfterLogin && isFirstSnapshot) {
                        AppToastController.show(
                            "Sync error. Please check your connection.",
                            ToastType.Error
                        )
                    }
                }
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener
            isFirstSnapshot = false

            val hasPending = snapshot.metadata.hasPendingWrites()
            _hasPendingWrites.value = hasPending
            if (!hasPending && _lastErrorCode.value == null) {
                updateSyncState()
            }

            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    val docId = doc.id
                    val docHasPending = doc.metadata.hasPendingWrites()

                    // Mark a row synced = true when reported with hasPendingWrites = false, matching doc's updatedAt
                    if (!docHasPending) {
                        val remoteUpdatedAt = (doc.data?.get("updatedAt") as? Number)?.toLong()
                        if (remoteUpdatedAt != null) {
                            taskDao.markSynced(docId, remoteUpdatedAt)
                        }
                    }

                    // Ignore docs with hasPendingWrites (our own echo) to avoid loops
                    if (docHasPending) {
                        continue
                    }

                    if (change.type == DocumentChange.Type.REMOVED) {
                        taskDao.hardDeleteById(docId)
                        ReminderScheduler.cancel(context, docId)
                        continue
                    }

                    val remoteData = doc.data
                    val remoteTask = TaskEntity.fromFirestoreMap(remoteData, docId)
                    val localTask = taskDao.getTaskByIdIncludingDeleted(docId)

                    if (localTask == null) {
                        if (!remoteTask.deleted) {
                            val toInsert = remoteTask.copy(synced = true)
                            taskDao.insertTask(toInsert)
                            if (!toInsert.done) {
                                ReminderScheduler.schedule(context, toInsert)
                            } else {
                                ReminderScheduler.cancel(context, docId)
                            }
                        }
                    } else {
                        // Conflict resolution: Remote replaces local ONLY when remote.updatedAt is strictly
                        // greater, and NEVER touches a row with synced = false.
                        if (localTask.synced && remoteTask.updatedAt > localTask.updatedAt) {
                            if (remoteTask.deleted) {
                                taskDao.hardDeleteById(docId)
                                ReminderScheduler.cancel(context, docId)
                            } else {
                                val toUpdate = remoteTask.copy(synced = true)
                                taskDao.insertTask(toUpdate)
                                if (!toUpdate.done) {
                                    ReminderScheduler.schedule(context, toUpdate)
                                } else {
                                    ReminderScheduler.cancel(context, docId)
                                }
                            }
                        }
                    }
                }

                // Clean up synced tombstones
                taskDao.deleteSyncedTombstones()
            }
        }
    }

    /**
     * Flush all pending local changes to Firestore before logout or process shutdown.
     * Waits for all rows with synced = false to be pushed and acknowledged.
     * Returns true if all changes have synced, false on timeout or failure.
     */
    suspend fun flushPendingWrites(timeoutMs: Long = 5000L): Boolean {
        val uid = currentUserId ?: return true
        val unsynced = taskDao.getUnsyncedTasksOnce()
        if (unsynced.isEmpty() && !_hasPendingWrites.value) {
            return true
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                val firestore = firestoreProvider() ?: return@withTimeoutOrNull false
                val tasksCollection = firestore.collection("users").document(uid).collection("tasks")
                val pending = taskDao.getUnsyncedTasksOnce()
                for (task in pending) {
                    tasksCollection.document(task.id).set(task.toFirestoreMap()).await()
                    taskDao.markSynced(task.id, task.updatedAt)
                }
                try {
                    firestore.waitForPendingWrites().await()
                } catch (_: Exception) {}
                taskDao.getUnsyncedTasksOnce().isEmpty()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Stop listeners and reset status.
     */
    fun stop() {
        if (BuildConfig.DEBUG) {
            Log.d("TikoSync", "Stopping sync session")
        }
        snapshotRegistration?.remove()
        snapshotRegistration = null
        pushJob?.cancel()
        pushJob = null
        syncScope?.cancel()
        syncScope = null
        currentUserId = null
        _syncState.value = SyncState.Idle
        _syncStatus.value = ""
        _hasPendingWrites.value = false
        _lastErrorCode.value = null
    }

    /**
     * Delete all user task documents from Firestore.
     */
    suspend fun deleteAllUserTasks(userId: String) {
        stop()
        val firestore = firestoreProvider() ?: return
        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        val snapshot = tasksCollection.get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
