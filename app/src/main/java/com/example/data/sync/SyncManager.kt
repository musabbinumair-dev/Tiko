package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncManager(
    private val context: Context,
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private var syncScope: CoroutineScope? = null
    private var snapshotRegistration: ListenerRegistration? = null
    private var pushJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _syncStatus = MutableStateFlow<String>("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    private val _hasPendingWrites = MutableStateFlow(false)

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
                updateStatus()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                updateStatus()
            }
        }
        networkCallback = callback
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // Ignore callback registration failure
        }
    }

    private fun updateStatus() {
        if (currentUserId == null) {
            _syncStatus.value = ""
            return
        }
        if (!_isOnline.value) {
            _syncStatus.value = "Offline"
            return
        }
        if (_hasPendingWrites.value) {
            _syncStatus.value = "Syncing..."
        } else {
            _syncStatus.value = "Synced"
        }
    }

    /**
     * Start SyncManager for the signed-in user.
     * Login or signup: run a one-time merge. Upload any local guest tasks into the user's collection, then start the listener.
     */
    fun start(userId: String, isFirstPullAfterLogin: Boolean = false) {
        if (currentUserId == userId && syncScope != null) {
            return
        }
        stop()

        currentUserId = userId
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        syncScope = scope

        _hasPendingWrites.value = true
        updateStatus()

        scope.launch {
            try {
                // One-time merge on login/signup: upload any local tasks (including guest tasks)
                val localTasks = taskDao.getAllTasksRaw()
                val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
                for (task in localTasks) {
                    if (!task.synced) {
                        tasksCollection.document(task.id).set(task.toFirestoreMap())
                    }
                }
            } catch (_: Exception) {
                // Ignore initial merge error
            }

            // Start Push observer
            startPushObserver(userId, scope)

            // Start Pull snapshot listener
            startPullListener(userId, scope, isFirstPullAfterLogin)
        }
    }

    private fun startPushObserver(userId: String, scope: CoroutineScope) {
        pushJob = scope.launch {
            val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
            taskDao.getUnsyncedTasks().collectLatest { unsyncedTasks ->
                if (unsyncedTasks.isNotEmpty()) {
                    _hasPendingWrites.value = true
                    updateStatus()
                } else {
                    _hasPendingWrites.value = false
                    updateStatus()
                }

                for (task in unsyncedTasks) {
                    // Push: write each to Firestore with set(). Do NOT await set() (it hangs offline).
                    tasksCollection.document(task.id).set(task.toFirestoreMap())
                }
            }
        }
    }

    private fun startPullListener(userId: String, scope: CoroutineScope, isFirstPullAfterLogin: Boolean) {
        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        var isFirstSnapshot = true

        snapshotRegistration = tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Check if it's an auth error (token expired or account deleted)
                val isAuthError = error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ||
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                
                // Check if it's a network error
                val isNetworkError = error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    error.message?.contains("offline", ignoreCase = true) == true ||
                    error.message?.contains("network", ignoreCase = true) == true ||
                    !_isOnline.value

                if (isAuthError) {
                    // Token expired or account deleted elsewhere - sign out cleanly
                    stop()
                    AppToastController.show(
                        "Please log in again",
                        ToastType.Info
                    )
                    // Note: The actual sign-out and navigation should be handled by the caller
                    // via observing sync status or auth state changes
                } else if (isFirstPullAfterLogin && isFirstSnapshot && !isNetworkError) {
                    // Non-network error on first sync attempt
                    AppToastController.show(
                        "Sync error. Please check your connection.",
                        ToastType.Error
                    )
                }
                // Network errors are silent - don't log users out
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener
            isFirstSnapshot = false

            val hasPending = snapshot.metadata.hasPendingWrites()
            _hasPendingWrites.value = hasPending
            updateStatus()

            scope.launch {
                for (change in snapshot.documentChanges) {
                    val doc = change.document
                    val docId = doc.id
                    val docHasPending = doc.metadata.hasPendingWrites()

                    // Mark a row synced = true when the snapshot listener reports that doc with hasPendingWrites = false
                    if (!docHasPending) {
                        taskDao.markSynced(docId, true)
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
                        // Newest updatedAt wins
                        if (remoteTask.updatedAt > localTask.updatedAt) {
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

                // Rows deleted = true and synced = true are removed from Room
                taskDao.deleteSyncedTombstones()
            }
        }
    }

    /**
     * Stop listeners and reset status.
     */
    fun stop() {
        snapshotRegistration?.remove()
        snapshotRegistration = null
        pushJob?.cancel()
        pushJob = null
        syncScope?.cancel()
        syncScope = null
        currentUserId = null
        _syncStatus.value = ""
        _hasPendingWrites.value = false
    }

    /**
     * Delete all user task documents from Firestore.
     */
    suspend fun deleteAllUserTasks(userId: String) {
        stop()
        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        val snapshot = tasksCollection.get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
