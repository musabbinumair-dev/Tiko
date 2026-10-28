package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.FirebaseInitializer
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val displayName: String = "", val email: String = "") : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val context: Context? = null) {
    private val TAG = "AuthRepository"
    private val WEB_CLIENT_ID = "542946903408-jumrf8l5mantirmc7j6hnlcqpc6788tq.apps.googleusercontent.com"

    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            context?.let { FirebaseInitializer.init(it) }
            try {
                FirebaseAuth.getInstance()
            } catch (e2: Throwable) {
                Log.e(TAG, "Failed to obtain FirebaseAuth", e2)
                null
            }
        }
    }

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = getAuth()
        if (auth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (e: Throwable) {
            trySend(null)
        }
        awaitClose {
            try {
                auth.removeAuthStateListener(listener)
            } catch (e: Throwable) {
                // ignore
            }
        }
    }

    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            )?.await()
            AuthResult.Success(displayName = name, email = email)
        } catch (e: Exception) {
            Log.e(TAG, "signUp error", e)
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            val name = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
            AuthResult.Success(displayName = name, email = user?.email ?: email)
        } catch (e: Exception) {
            Log.e(TAG, "signIn error", e)
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check your internet connection.")
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            } catch (e: NoCredentialException) {
                Log.w(TAG, "GetGoogleIdOption threw NoCredentialException [${e.javaClass.name}]: ${e.message}. Falling back to GetSignInWithGoogleOption.", e)
                val fallbackOption = GetSignInWithGoogleOption.Builder(serverClientId = WEB_CLIENT_ID)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(
                    request = fallbackRequest,
                    context = activityContext
                )
            } catch (e: GetCredentialException) {
                val msg = e.message ?: ""
                if (msg.contains("no credential", ignoreCase = true) || msg.contains("no account", ignoreCase = true) || msg.contains("No accounts", ignoreCase = true)) {
                    Log.w(TAG, "GetGoogleIdOption threw no credentials error [${e.javaClass.name}]: ${e.message}. Falling back to GetSignInWithGoogleOption.", e)
                    val fallbackOption = GetSignInWithGoogleOption.Builder(serverClientId = WEB_CLIENT_ID)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    credentialManager.getCredential(
                        request = fallbackRequest,
                        context = activityContext
                    )
                } else {
                    throw e
                }
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                try {
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    val displayName = user?.displayName?.ifBlank { null }
                        ?: googleIdTokenCredential.displayName?.ifBlank { null }
                        ?: googleIdTokenCredential.givenName?.ifBlank { null }
                        ?: "User"
                    val email = user?.email?.ifBlank { null }
                        ?: googleIdTokenCredential.id

                    if (user?.displayName.isNullOrBlank() && displayName.isNotBlank()) {
                        user?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                        )?.await()
                    }
                    AuthResult.Success(displayName = displayName, email = email)
                } catch (collisionEx: FirebaseAuthUserCollisionException) {
                    Log.w(TAG, "Account collision detected during Google Sign-In: [${collisionEx.javaClass.name}] ${collisionEx.message}", collisionEx)
                    val email = collisionEx.email ?: ""
                    val msg = if (email.isNotBlank()) {
                        "An account already exists with $email using password. Please log in with your email and password."
                    } else {
                        "An account already exists with this email. Please log in with your email and password."
                    }
                    AuthResult.Error(msg)
                }
            } else {
                Log.e(TAG, "Unable to retrieve Google credentials: credential class=${credential.javaClass.name}, type=${(credential as? CustomCredential)?.type}")
                AuthResult.Error("Unable to retrieve Google credentials.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled: [${e.javaClass.name}] ${e.message}", e)
            AuthResult.Error("Google Sign-In was cancelled.")
        } catch (e: NoCredentialException) {
            Log.e(TAG, "Google Sign-In no account: [${e.javaClass.name}] ${e.message}", e)
            AuthResult.Error("No Google account selected or available on device.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In GetCredentialException: [${e.javaClass.name}] ${e.message}", e)
            val msg = e.message ?: ""
            if (msg.contains("cancel", ignoreCase = true) || msg.contains("16:", ignoreCase = true)) {
                AuthResult.Error("Google Sign-In was cancelled.")
            } else if (msg.contains("no credential", ignoreCase = true) || msg.contains("no account", ignoreCase = true) || msg.contains("No accounts", ignoreCase = true)) {
                AuthResult.Error("No Google account selected or available on device.")
            } else {
                AuthResult.Error("Google Sign-In failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In exception: [${e.javaClass.name}] ${e.message}", e)
            val msg = e.message ?: ""
            if (msg.contains("cancel", ignoreCase = true) || msg.contains("16:", ignoreCase = true)) {
                AuthResult.Error("Google Sign-In was cancelled.")
            } else if (msg.contains("no credential", ignoreCase = true) || msg.contains("no account", ignoreCase = true)) {
                AuthResult.Error("No Google account selected or available on device.")
            } else {
                AuthResult.Error(mapFirebaseError(e))
            }
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success()
        } catch (e: Exception) {
            Log.e(TAG, "sendPasswordReset error", e)
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Throwable) {
            // ignore
        }
    }

    suspend fun deleteAccount(): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            auth.currentUser?.delete()?.await()
            AuthResult.Success()
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount error", e)
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun reauthenticate(password: String): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            val user = auth.currentUser ?: return AuthResult.Error("Not signed in")
            val email = user.email ?: return AuthResult.Error("Email not found")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            AuthResult.Success()
        } catch (e: Exception) {
            Log.e(TAG, "reauthenticate error", e)
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("email address is already in use", ignoreCase = true) ||
            message.contains("The email address is already in use by another account", ignoreCase = true) ->
                "This email already has an account. Try logging in."
            message.contains("password is invalid", ignoreCase = true) ||
            message.contains("no user record", ignoreCase = true) ||
            message.contains("wrong password", ignoreCase = true) ||
            message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Wrong email or password."
            message.contains("weak password", ignoreCase = true) ||
            message.contains("at least 6 characters", ignoreCase = true) ->
                "Use at least 8 characters."
            message.contains("network error", ignoreCase = true) ||
            message.contains("internet", ignoreCase = true) ||
            message.contains("A network error", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            message.contains("too many requests", ignoreCase = true) ||
            message.contains("temporarily disabled", ignoreCase = true) ->
                "Too many attempts. Try again later."
            message.contains("recent login", ignoreCase = true) ||
            message.contains("requires-recent-login", ignoreCase = true) ->
                "Please sign in again to continue."
            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Email/Password authentication provider is not enabled in Firebase Console."
            else -> e.localizedMessage ?: "Something went wrong. Please try again."
        }
    }
}
