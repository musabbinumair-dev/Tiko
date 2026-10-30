package com.example.data.sync

import android.content.Context
import android.util.Log
import com.musab.tiko.BuildConfig
import com.example.data.FirebaseInitializer
import com.example.data.local.TikoDatabase
import com.example.data.local.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncCoordinator(
    private val context: Context,
    val syncManager: SyncManager,
    private val preferences: UserPreferences,
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            FirebaseInitializer.init(context)
            try {
                FirebaseAuth.getInstance()
            } catch (_: Throwable) {
                null
            }
        }
    },
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var activeUserId: String? = null

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val syncStatus: StateFlow<String> = syncManager.syncStatus

    fun startListening() {
        if (authListener != null) return

        val auth = authProvider()
        if (auth == null) {
            if (BuildConfig.DEBUG) {
                Log.w("TikoSync", "FirebaseAuth not available to start listening")
            }
            return
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            handleAuthStateChanged(user)
        }
        authListener = listener
        try {
            auth.addAuthStateListener(listener)
            if (BuildConfig.DEBUG) {
                Log.d("TikoSync", "SyncCoordinator registered AuthStateListener")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("TikoSync", "Failed to add AuthStateListener: ${e.message}")
            }
        }
    }

    fun stopListening() {
        authListener?.let {
            try {
                authProvider()?.removeAuthStateListener(it)
            } catch (_: Exception) {}
        }
        authListener = null
    }

    fun handleAuthStateChanged(user: FirebaseUser?) {
        if (user != null) {
            val provider = user.providerData
                .mapNotNull { it.providerId }
                .firstOrNull { it != "firebase" && it.isNotBlank() }
                ?: (if (!user.providerId.isNullOrBlank()) user.providerId else "password")
            handleUserSignedIn(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                provider = provider
            )
        } else {
            handleUserSignedOut()
        }
    }

    fun handleUserSignedIn(
        uid: String,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        provider: String? = null
    ) {
        if (BuildConfig.DEBUG) {
            Log.d("TikoSync", "AuthState changed: user signed in")
        }

        // Signing in on any path clears guest mode
        preferences.setGuestMode(false)

        // Start sync idempotently
        if (activeUserId != uid || !syncManager.isSyncActive()) {
            activeUserId = uid
            syncManager.start(uid)
        }

        // Write users/{uid} profile document
        scope.launch {
            writeProfileDocument(uid, displayName, email, photoUrl, provider)
        }
    }

    fun handleUserSignedOut() {
        if (BuildConfig.DEBUG) {
            Log.d("TikoSync", "AuthState changed: user signed out")
        }
        activeUserId = null
        syncManager.stop()
    }

    suspend fun writeProfileDocument(
        uid: String,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        provider: String? = null
    ) {
        val firestore = firestoreProvider() ?: return
        if (uid.isBlank()) return

        try {
            val userDocRef = firestore.collection("users").document(uid)
            val snapshot = try {
                userDocRef.get().await()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w("TikoSync", "Could not fetch existing profile doc: ${e.message}")
                }
                null
            }

            val profileData = mutableMapOf<String, Any>()

            // name: keep existing or use non-blank displayName
            val safeName = displayName?.ifBlank { null }
            if (!safeName.isNullOrBlank()) {
                profileData["name"] = safeName
            } else if (snapshot == null || !snapshot.contains("name")) {
                val fallbackName = email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
                profileData["name"] = fallbackName
            }

            // email: only non-blank
            val safeEmail = email?.ifBlank { null }
            if (!safeEmail.isNullOrBlank()) {
                profileData["email"] = safeEmail
            }

            // photoUrl: only non-blank
            val safePhotoUrl = photoUrl?.ifBlank { null }
            if (!safePhotoUrl.isNullOrBlank()) {
                profileData["photoUrl"] = safePhotoUrl
            }

            // provider
            val safeProvider = provider?.ifBlank { null } ?: "password"
            if (safeProvider.isNotBlank()) {
                profileData["provider"] = safeProvider
            }

            // lastLoginAt
            profileData["lastLoginAt"] = FieldValue.serverTimestamp()

            // createdAt: only write if document doesn't exist or missing createdAt
            if (snapshot == null || !snapshot.exists() || !snapshot.contains("createdAt")) {
                profileData["createdAt"] = FieldValue.serverTimestamp()
            }

            if (BuildConfig.DEBUG) {
                Log.d("TikoSync", "Writing profile doc with keys: ${profileData.keys}")
            }

            userDocRef.set(profileData, SetOptions.merge()).await()
            if (BuildConfig.DEBUG) {
                Log.d("TikoSync", "Profile doc written successfully")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("TikoSync", "Failed to write profile document: ${e.message}")
            }
        }
    }

    suspend fun flushPendingWrites(timeoutMs: Long = 5000L): Boolean {
        return syncManager.flushPendingWrites(timeoutMs)
    }

    suspend fun deleteUserData(userId: String) {
        syncManager.deleteAllUserTasks(userId)
        try {
            firestoreProvider()?.collection("users")?.document(userId)?.delete()?.await()
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: SyncCoordinator? = null

        fun getInstance(context: Context): SyncCoordinator {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appCtx = context.applicationContext
                    val db = TikoDatabase.getInstance(appCtx)
                    val syncMgr = SyncManager(appCtx, db.taskDao())
                    val prefs = UserPreferences(appCtx)
                    SyncCoordinator(appCtx, syncMgr, prefs).also { instance = it }
                }
            }
        }

        fun setInstanceForTesting(coordinator: SyncCoordinator?) {
            instance = coordinator
        }
    }
}
