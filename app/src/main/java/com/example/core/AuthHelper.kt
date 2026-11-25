package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
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
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
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
import java.security.MessageDigest

private const val TAG = "Auth"
private const val EXPECTED_DEBUG_SHA1 = "29:E7:DC:9E:DD:6E:AF:FF:77:40:5F:69:BE:94:27:4B:83:2C:66:41"

val GOOGLE_WORKSPACE_SCOPES = listOf(
    "https://www.googleapis.com/auth/gmail.readonly",
    "https://www.googleapis.com/auth/gmail.send",
    "https://www.googleapis.com/auth/drive.file",
    "https://www.googleapis.com/auth/documents",
    "https://www.googleapis.com/auth/spreadsheets",
    "https://www.googleapis.com/auth/calendar.events",
    "https://www.googleapis.com/auth/contacts.readonly",
)

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
    onAuthCancelled: (String?) -> Unit = {},
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
                    onAuthError("Akun Google terpilih, tetapi sesi Firebase Auth tidak aktif. Coba gunakan tombol Masuk via Browser Google di bawah.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            val currentSha1 = getApkSha1Fingerprint(context)
            val diagnostic = if (currentSha1 != null && !currentSha1.equals(EXPECTED_DEBUG_SHA1, ignoreCase = true)) {
                "Login Play Services dibatalkan karena SHA-1 APK ($currentSha1) berbeda dari Firebase ($EXPECTED_DEBUG_SHA1). Gunakan tombol 'Masuk via Browser Google' di bawah untuk masuk langsung."
            } else {
                "Dialog Google dibatalkan setelah pemilihan akun (${e.message ?: "cancelled"}). Gunakan tombol 'Masuk via Browser Google' di bawah agar langsung masuk."
            }
            withContext(Dispatchers.Main.immediate) {
                onAuthCancelled(diagnostic)
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential available: ${e.message}", e)
            withContext(Dispatchers.Main.immediate) {
                onAuthError("Kredensial Google Play Services belum siap. Gunakan tombol 'Masuk via Browser Google' di bawah.")
            }
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "FirebaseAuthException during Google Sign-In: ${e.errorCode}", e)
            val detail = when (e.errorCode) {
                "ERROR_INVALID_CREDENTIAL" ->
                    "Kredensial Google ditolak Firebase (${e.errorCode}). Coba tombol 'Masuk via Browser Google' di bawah."
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
                    "SHA-1 APK tidak cocok dengan Firebase (DEVELOPER_ERROR). Gunakan tombol 'Masuk via Browser Google' di bawah."
                raw.contains("network", ignoreCase = true) ->
                    "Koneksi internet bermasalah saat menghubungi Firebase Auth. Periksa jaringan lalu coba lagi."
                else -> raw.ifBlank { "Gagal masuk dengan Google (${e::class.simpleName}). Coba tombol 'Masuk via Browser Google' di bawah." }
            }
            withContext(Dispatchers.Main.immediate) {
                onAuthError(hint)
            }
        }
    }
}

/**
 * Fallback & Browser Google OAuth login via Firebase Auth Web OAuth handler.
 * Works across all APK signatures (including GitHub Actions Release/Debug builds)
 * and Android devices where Credential Manager / Play Services bottom sheet rejects SHA-1.
 */
fun onGoogleWebOAuthSignInClicked(
    context: Context,
    auth: FirebaseAuth = Firebase.auth,
    includeWorkspaceScopes: Boolean = false,
    onAuthSuccess: (FirebaseUser?, String?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Activity context tidak ditemukan untuk OAuth browser.")
        return
    }
    scope.launch {
        try {
            val scopes = if (includeWorkspaceScopes) {
                listOf("email", "profile") + GOOGLE_WORKSPACE_SCOPES
            } else {
                listOf("email", "profile")
            }
            val provider = OAuthProvider.newBuilder("google.com", auth)
                .setScopes(scopes)
                .addCustomParameter("prompt", "select_account")
                .build()

            val authResult = withContext(NonCancellable) {
                val pendingTask = auth.pendingAuthResult
                if (pendingTask != null) {
                    pendingTask.await()
                } else {
                    auth.startActivityForSignInWithProvider(activity, provider).await()
                }
            }
            val user = authResult.user ?: auth.currentUser
            val accessToken = (authResult.credential as? OAuthCredential)?.accessToken
            withContext(Dispatchers.Main.immediate) {
                if (user != null) {
                    Log.d(TAG, "Browser Google OAuth succeeded for uid=${user.uid}")
                    onAuthSuccess(user, accessToken)
                } else {
                    onAuthError("Autentikasi browser selesai tetapi user Firebase tidak ditemukan.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Browser Google OAuth failed", e)
            withContext(Dispatchers.Main.immediate) {
                onAuthError(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Gagal masuk via Browser Google (${e::class.simpleName})."
                )
            }
        }
    }
}

/**
 * Checks whether a Chrome Custom Tab OAuth flow completed while the Activity was recreated.
 */
suspend fun checkPendingGoogleOAuthResult(
    auth: FirebaseAuth = Firebase.auth,
): Pair<FirebaseUser?, String?>? {
    val pending = auth.pendingAuthResult ?: return null
    return try {
        val result: AuthResult = withContext(NonCancellable) { pending.await() }
        val token = (result.credential as? OAuthCredential)?.accessToken
        (result.user ?: auth.currentUser) to token
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Pending OAuth result failed: ${e.message}")
        null
    }
}

/**
 * Authorizes Google Workspace OAuth scopes (Gmail, Drive, Docs, Sheets, Calendar, Contacts)
 * for the currently signed-in user and returns a short-lived OAuth2 access token in memory.
 */
fun authorizeGoogleWorkspaceScopes(
    context: Context,
    auth: FirebaseAuth = Firebase.auth,
    onTokenReady: (String) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val activity = context.findActivity()
    if (activity == null) {
        onError("Activity context tidak ditemukan untuk otorisasi Google Workspace.")
        return
    }
    scope.launch {
        try {
            val user = auth.currentUser
            val provider = OAuthProvider.newBuilder("google.com", auth)
                .setScopes(listOf("email", "profile") + GOOGLE_WORKSPACE_SCOPES)
                .addCustomParameter("prompt", "consent")
                .build()

            val result = withContext(NonCancellable) {
                val pending = auth.pendingAuthResult
                when {
                    pending != null -> pending.await()
                    user != null -> user.startActivityForReauthenticateWithProvider(activity, provider).await()
                    else -> auth.startActivityForSignInWithProvider(activity, provider).await()
                }
            }
            val accessToken = (result.credential as? OAuthCredential)?.accessToken
            withContext(Dispatchers.Main.immediate) {
                if (!accessToken.isNullOrBlank()) {
                    onTokenReady(accessToken)
                } else {
                    onError("Izin Google selesai tetapi Access Token tidak dikembalikan oleh OAuth provider.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Workspace OAuth authorization failed", e)
            withContext(Dispatchers.Main.immediate) {
                onError(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Gagal menghubungkan Google Workspace (${e::class.simpleName})."
                )
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
                WorkspaceSessionStore.clear()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                WorkspaceSessionStore.clear()
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

@Suppress("DEPRECATION")
fun getApkSha1Fingerprint(context: Context): String? {
    return try {
        val packageManager = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo?.apkContentsSigners
        } else {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString(":") { byte -> "%02X".format(byte) }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to inspect APK SHA-1: ${e.message}")
        null
    }
}
