package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository {
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Throwable) {
        null
    }

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val fbAuth = auth
        if (fbAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            fbAuth.addAuthStateListener(listener)
        } catch (e: Throwable) {
            trySend(null)
        }
        awaitClose {
            try {
                fbAuth.removeAuthStateListener(listener)
            } catch (e: Throwable) {
                // ignore
            }
        }
    }

    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        val fbAuth = auth ?: return AuthResult.Error("Authentication is currently unavailable.")
        return try {
            val result = fbAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            )?.await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val fbAuth = auth ?: return AuthResult.Error("Authentication is currently unavailable.")
        return try {
            fbAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        val fbAuth = auth ?: return AuthResult.Error("Authentication is currently unavailable.")
        return try {
            fbAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            // ignore
        }
    }

    suspend fun deleteAccount(): AuthResult {
        val fbAuth = auth ?: return AuthResult.Error("Authentication is currently unavailable.")
        return try {
            fbAuth.currentUser?.delete()?.await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun reauthenticate(password: String): AuthResult {
        val fbAuth = auth ?: return AuthResult.Error("Authentication is currently unavailable.")
        return try {
            val user = fbAuth.currentUser ?: return AuthResult.Error("Not signed in")
            val email = user.email ?: return AuthResult.Error("Email not found")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("email address is already in use", ignoreCase = true) ->
                "This email already has an account. Try logging in."
            message.contains("password is invalid", ignoreCase = true) ||
            message.contains("no user record", ignoreCase = true) ||
            message.contains("wrong password", ignoreCase = true) ->
                "Wrong email or password."
            message.contains("weak password", ignoreCase = true) ||
            message.contains("at least 6 characters", ignoreCase = true) ->
                "Use at least 8 characters."
            message.contains("network", ignoreCase = true) ||
            message.contains("internet", ignoreCase = true) ->
                "No internet connection. Try again."
            message.contains("too many requests", ignoreCase = true) ||
            message.contains("temporarily disabled", ignoreCase = true) ->
                "Too many attempts. Try again later."
            message.contains("recent login", ignoreCase = true) ||
            message.contains("requires-recent-login", ignoreCase = true) ->
                "Please sign in again to continue."
            else -> "Something went wrong. Please try again."
        }
    }
}
