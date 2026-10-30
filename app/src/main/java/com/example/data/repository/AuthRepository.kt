package com.example.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.FirebaseInitializer
import com.musab.tiko.R
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

sealed class GoogleAuthResult {
    data class Success(val displayName: String = "", val email: String = "", val uid: String = "") : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
}

class AuthRepository(private val context: Context? = null) {
    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            context?.let { FirebaseInitializer.init(it) }
            try {
                FirebaseAuth.getInstance()
            } catch (_: Throwable) {
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
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        val auth = getAuth() ?: return GoogleAuthResult.Error("Authentication service is unavailable. Please check your internet connection.")
        return try {
            val resId = activityContext.resources.getIdentifier("default_web_client_id", "string", activityContext.packageName)
            val webClientId = if (resId != 0) {
                try {
                    activityContext.getString(resId)
                } catch (_: Exception) {
                    ""
                }
            } else {
                ""
            }

            if (webClientId.isBlank()) {
                return GoogleAuthResult.Error("Google sign-in is not configured on this build.")
            }

            val credentialManager = CredentialManager.create(activityContext)
            
            // 1. Prefer GetSignInWithGoogleOption for standard interactive account selection
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
            } catch (_: NoCredentialException) {
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(
                    request = fallbackRequest,
                    context = activityContext
                )
            }

            val credential = result.credential
            val idToken: String? = try {
                when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        googleIdTokenCredential.idToken
                    }
                    credential is CustomCredential -> {
                        credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                            ?: credential.data.getString("id_token")
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }

            if (idToken.isNullOrEmpty()) {
                return GoogleAuthResult.Error("Google sign-in failed. Please try again.")
            }

            try {
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user

                val displayName = user?.displayName?.ifBlank { null }
                    ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "User"
                val email = user?.email ?: ""
                GoogleAuthResult.Success(displayName = displayName, email = email, uid = user?.uid ?: "")
            } catch (e: Exception) {
                GoogleAuthResult.Error(mapFirebaseError(e))
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: Exception) {
            if (e.message?.contains("cancel", ignoreCase = true) == true) {
                GoogleAuthResult.Cancelled
            } else {
                GoogleAuthResult.Error(mapFirebaseError(e))
            }
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        val auth = getAuth() ?: return AuthResult.Error("Authentication service is unavailable. Please check internet connection.")
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success()
        } catch (e: Exception) {
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
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            e is FirebaseAuthUserCollisionException ||
            message.contains("email address is already in use", ignoreCase = true) ||
            message.contains("The email address is already in use by another account", ignoreCase = true) ||
            message.contains("account-exists-with-different-credential", ignoreCase = true) ->
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
            e is NoCredentialException ||
            message.contains("NoCredentialException", ignoreCase = true) ||
            message.contains("Cannot find a matching credential", ignoreCase = true) ->
                "No Google account found on this device."
            else -> "Something went wrong. Please try again."
        }
    }
}
