package com.example.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Auth wrapper: email/password sign-up/sign-in plus Google Sign-In
 * through Credential Manager. Works without a configured Firebase project too:
 * every operation then fails with [NOT_CONFIGURED_MESSAGE] instead of crashing.
 */
class AuthRepository(private val appContext: Context) {

    companion object {
        const val NOT_CONFIGURED_MESSAGE =
            "Firebase is not configured yet. Add google-services.json from the " +
                "Firebase console (see README) and rebuild."
    }

    private val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (e: IllegalStateException) {
        null
    }

    val currentUser: Flow<FirebaseUser?> = if (auth != null) {
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    } else {
        MutableStateFlow<FirebaseUser?>(null).asStateFlow()
    }

    val isFirebaseConfigured: Boolean get() = auth != null

    val isGoogleSignInConfigured: Boolean
        get() = isFirebaseConfigured && AuthConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException(NOT_CONFIGURED_MESSAGE))
        return runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).awaitTask().user!!
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException(NOT_CONFIGURED_MESSAGE))
        return runCatching {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).awaitTask().user!!
        }
    }

    /**
     * Google Sign-In via Credential Manager. [activityContext] must be an Activity
     * context because the credential UI is launched on top of it.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException(NOT_CONFIGURED_MESSAGE))
        val serverClientId = AuthConfig.GOOGLE_WEB_CLIENT_ID
        if (serverClientId.isBlank()) {
            return Result.failure(
                IllegalStateException("Google Sign-In is not configured: set GOOGLE_WEB_CLIENT_ID and rebuild.")
            )
        }
        return runCatching {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credentialManager = CredentialManager.create(appContext)
            val result = credentialManager.getCredential(activityContext, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential).awaitTask().user!!
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    fun friendlyErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is GetCredentialCancellationException -> "Sign-in cancelled."
            is NoCredentialException -> "No Google account found on this device."
            else -> throwable.localizedMessage ?: "Sign-in failed. Please try again."
        }
    }
}

/** Await a Play Services [Task] without adding kotlinx-coroutines-play-services. */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
