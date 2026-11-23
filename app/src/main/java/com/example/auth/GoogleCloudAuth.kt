package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.CloudSyncPrefsStore
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun safeFirebaseAuth(): FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()

tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

suspend fun <T> Task<T>.awaitGmsTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            val ex = task.exception ?: IllegalStateException("Task failed without exception")
            cont.resumeWithException(ex)
        }
    }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    runCatching { addAuthStateListener(listener) }
    trySend(runCatching { currentUser }.getOrNull())
    awaitClose { runCatching { removeAuthStateListener(listener) } }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = safeFirebaseAuth()
    if (auth == null || credentialManager == null) {
        onUnauthenticated()
        return
    }
    if (runCatching { auth.currentUser }.getOrNull() != null) {
        CloudSyncPrefsStore(context).setExplicitGoogleSignOut(false)
        onAuthSuccess()
        return
    }
    if (CloudSyncPrefsStore(context).isExplicitGoogleSignOut()) {
        onUnauthenticated()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Throwable) {
        onUnauthenticated()
        return
    }
    if (clientId.isBlank()) {
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val activityOrContext = context.findActivityOrNull() ?: context
            val cred = credentialManager.getCredential(activityOrContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).awaitGmsTask()
                CloudSyncPrefsStore(context).setExplicitGoogleSignOut(false)
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.d("Auth", "Silent auto-sign-in skipped or unavailable: ${t.message}")
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val auth = safeFirebaseAuth()
    if (auth == null || credentialManager == null) {
        onAuthError("Google Sign-In service is not available on this device.")
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Throwable) {
        onAuthError("Google Sign-In configuration is missing.")
        return
    }
    if (clientId.isBlank()) {
        onAuthError("Google Sign-In client ID is missing.")
        return
    }

    scope.launch {
        val activityOrContext = context.findActivityOrNull() ?: context
        try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val cred = credentialManager.getCredential(activityOrContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).awaitGmsTask()
                CloudSyncPrefsStore(context).setExplicitGoogleSignOut(false)
                onAuthSuccess()
                return@launch
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "GetSignInWithGoogleOption cancelled: ${e.message}", e)
            onAuthCancelled()
            return@launch
        } catch (t: Throwable) {
            Log.w("Auth", "GetSignInWithGoogleOption unavailable, falling back to GetGoogleIdOption: ${t.message}", t)
        }

        try {
            val allAccountsOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(allAccountsOption)
                .build()
            val cred = credentialManager.getCredential(activityOrContext, fallbackRequest).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).awaitGmsTask()
                CloudSyncPrefsStore(context).setExplicitGoogleSignOut(false)
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account found in CredentialManager: ${e.message}", e)
            onAuthError("No Gmail account was found on this device. Add an account in Settings > Accounts and try again.")
        } catch (t: Throwable) {
            Log.e("Auth", "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }
}

fun switchGoogleCloudAccount(
    context: Context,
    credentialManager: CredentialManager?,
    onBeforeSignOut: () -> Unit = {},
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            runCatching { onBeforeSignOut() }
            runCatching { credentialManager?.clearCredentialState(ClearCredentialStateRequest()) }
            runCatching { safeFirebaseAuth()?.signOut() }
        }
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onAuthError = onAuthError,
            scope = scope,
            onAuthCancelled = onAuthCancelled
        )
    }
}

fun signOutFromGoogleCloud(
    credentialManager: CredentialManager?,
    scope: CoroutineScope,
    context: Context? = null,
    onBeforeSignOut: () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    context?.let { CloudSyncPrefsStore(it).setExplicitGoogleSignOut(true) }
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeSignOut()
            } catch (t: Throwable) {
                Log.w("Auth", "Pre-sign-out listener teardown warning: ${t.message}")
            }
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (t: Throwable) {
                Log.w("Auth", "Failed to clear credential state: ${t.message}")
            } finally {
                runCatching { safeFirebaseAuth()?.signOut() }
                onSignOutComplete()
            }
        }
    }
}
