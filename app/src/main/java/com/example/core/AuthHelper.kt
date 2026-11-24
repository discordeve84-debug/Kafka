package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    trySend(currentUser)
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private fun extractGoogleIdToken(credential: Credential): String? {
    return when {
        credential is GoogleIdTokenCredential -> credential.idToken
        credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
            try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse GoogleIdTokenCredential from CustomCredential", e)
                null
            }
        }
        else -> {
            Log.w(TAG, "Unexpected credential type: ${credential::class.simpleName} (${credential.type})")
            null
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    auth: FirebaseAuth = Firebase.auth,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
): Job? {
    val existing = auth.currentUser
    if (existing != null) {
        onAuthSuccess(existing)
        return null
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return null
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return scope.launch {
        try {
            val hostContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(hostContext, request).credential
            val token = extractGoogleIdToken(cred)
            if (!token.isNullOrBlank()) {
                val signedUser = withContext(NonCancellable) {
                    val result = auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    result.user ?: auth.currentUser
                }
                withContext(Dispatchers.Main.immediate) {
                    if (signedUser != null) {
                        onAuthSuccess(signedUser)
                    } else {
                        onUnauthenticated()
                    }
                }
            } else {
                withContext(Dispatchers.Main.immediate) { onUnauthenticated() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            withContext(Dispatchers.Main.immediate) { onUnauthenticated() }
        } catch (e: Exception) {
            Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
            withContext(Dispatchers.Main.immediate) { onUnauthenticated() }
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    auth: FirebaseAuth = Firebase.auth,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
) {
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Konfigurasi Google Sign-In tidak ditemukan (R.string.default_web_client_id).")
        return
    }

    scope.launch {
        try {
            val activityContext = context.findActivity()
                ?: throw IllegalStateException("Activity context tidak ditemukan untuk Google Sign-In.")

            Log.d(TAG, "Starting interactive Google Sign-In via CredentialManager")
            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                )
                .build()

            val credential = try {
                credentialManager.getCredential(activityContext, primaryRequest).credential
            } catch (cancel: GetCredentialCancellationException) {
                throw cancel
            } catch (primaryErr: GetCredentialException) {
                Log.w(
                    TAG,
                    "Primary GetSignInWithGoogleOption failed (${primaryErr::class.simpleName}): ${primaryErr.message}; retrying single GetGoogleIdOption",
                    primaryErr
                )
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(clientId)
                            .setAutoSelectEnabled(false)
                            .build()
                    )
                    .build()
                credentialManager.getCredential(activityContext, fallbackRequest).credential
            }

            val token = extractGoogleIdToken(credential)
            if (token.isNullOrBlank()) {
                withContext(Dispatchers.Main.immediate) {
                    onAuthError("Token Google ID tidak ditemukan pada kredensial (${credential.type}).")
                }
                return@launch
            }

            Log.d(TAG, "Google ID token received, exchanging with Firebase Auth")
            val signedUser = withContext(NonCancellable) {
                val authResult = auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                val user = authResult.user ?: auth.currentUser
                if (user != null) {
                    runCatching { user.getIdToken(false).await() }
                }
                user ?: auth.currentUser
            }

            withContext(Dispatchers.Main.immediate) {
                if (signedUser != null) {
                    Log.d(TAG, "Firebase Google Sign-In succeeded for uid=${signedUser.uid}")
                    onAuthSuccess(signedUser)
                } else {
                    onAuthError("Akun Google terpilih, tetapi sesi Firebase Auth tidak aktif. Coba ulangi.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            withContext(Dispatchers.Main.immediate) {
                onAuthCancelled()
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential available: ${e.message}", e)
            withContext(Dispatchers.Main.immediate) {
                onAuthError("Belum ada akun Google yang aktif/siap digunakan di perangkat ini.")
            }
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "FirebaseAuthException during Google Sign-In: ${e.errorCode}", e)
            val detail = when (e.errorCode) {
                "ERROR_INVALID_CREDENTIAL" ->
                    "Kredensial Google ditolak Firebase (${e.errorCode}). Pastikan Web Client ID & SHA-1/SHA-256 project sudah sinkron."
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Provider Google Sign-In belum diaktifkan di Firebase Console (${e.errorCode})."
                else -> e.localizedMessage ?: "Gagal autentikasi Firebase (${e.errorCode})."
            }
            withContext(Dispatchers.Main.immediate) {
                onAuthError(detail)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val raw = e.localizedMessage.orEmpty()
            val hint = when {
                raw.contains("10:") || raw.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                    "SHA-1/SHA-256 keystore APK tidak cocok dengan konfigurasi Firebase (DEVELOPER_ERROR)."
                raw.contains("network", ignoreCase = true) ->
                    "Koneksi internet bermasalah saat menghubungi Firebase Auth. Periksa jaringan lalu coba lagi."
                else -> raw.ifBlank { "Gagal masuk dengan Google (${e::class.simpleName})." }
            }
            withContext(Dispatchers.Main.immediate) {
                onAuthError(hint)
            }
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    auth: FirebaseAuth = Firebase.auth,
    onSignOutComplete: () -> Unit = {},
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
                withContext(Dispatchers.Main.immediate) {
                    onSignOutComplete()
                }
            }
        }
    }
}

private fun resolveDefaultWebClientId(context: Context): String? {
    return try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read R.string.default_web_client_id", e)
        null
    }
}
