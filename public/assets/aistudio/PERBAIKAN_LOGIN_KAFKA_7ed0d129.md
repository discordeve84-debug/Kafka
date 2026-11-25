# Perbaikan Login Google — Kafka

## Diagnosis
Kode login (Credential Manager dan Firebase OAuth) **tidak salah secara logika**. Kedua tombol gagal karena penyebab yang sama:

- APK hasil GitHub Actions ditandatangani dengan SHA-1 `EA:20:B4:3E:35:0E:C2:4F:95:F8:E5:1D:83:FF:59:43:4F:96:FF:1C`.
- Firebase hanya mengenal SHA-1 `29:E7:DC:9E:DD:6E:AF:FF:77:40:5F:69:BE:94:27:4B:83:2C:66:41` (keystore AI Studio).
- Di zip tidak ada `debug.keystore` (di-ignore `.gitignore`) maupun `debug.keystore.base64`, dan secret `DEBUG_KEYSTORE_BASE64` belum terisi. Workflow lalu membuat keystore acak, sehingga SHA-1 berbeda dan bisa berubah di tiap build.
- Tombol "Masuk via Browser" juga memeriksa SHA-1, jadi bukan solusi. Pesan lama di kode keliru menyarankannya.

Catatan lain:
- `firebase-applet-config.json` menunjuk proyek `planning-with-ai-53624`, sedangkan `app/google-services.json` memakai `gen-lang-client-0313586556`. File itu tidak dipakai kode Kotlin. Pastikan SHA-1 didaftarkan di proyek **gen-lang-client-0313586556**.
- Teks error di APK-mu tidak persis sama dengan teks di zip, jadi APK yang kamu uji mungkin dari commit lain. Pastikan build dari commit terbaru.

## Cara memperbaiki (urut)
1. Salin 3 file di bawah ini ke proyek, lalu push ke GitHub.
2. Buka tab Actions dan jalankan build. Karena secret belum ada, ringkasan job akan menampilkan SHA-1/SHA-256 dan akan ada artifact `keystore-baru`.
3. Unduh artifact `keystore-baru`, lalu salin isi `debug.keystore.base64` ke GitHub: Settings -> Secrets and variables -> Actions -> New secret, nama `DEBUG_KEYSTORE_BASE64`.
4. Di Firebase Console (proyek **gen-lang-client-0313586556**) -> Project settings -> aplikasi `com.aistudio.kafka.xqmzkt` -> Add fingerprint. Tambahkan **SHA-1 dan SHA-256** dari ringkasan build. Unduh `google-services.json` baru dan timpa `app/google-services.json`.
5. Tambahkan SHA-1 baru itu ke `REGISTERED_SHA1` di `AuthHelper.kt` (supaya pesan diagnostik akurat).
6. Jalankan build lagi (kini keystore tetap, SHA-1 tidak berubah), **hapus aplikasi lama**, lalu pasang APK baru. Tunggu beberapa menit setelah mendaftarkan SHA-1 agar Google selesai memperbarui.

Jalan pintas tanpa mengubah apa pun: pakai APK yang diekspor langsung dari AI Studio (SHA-1 `29:E7...` sudah terdaftar).

Perubahan kode:
- `AuthHelper.kt`: pesan error kini menampilkan SHA-1 dan SHA-256 APK yang sedang berjalan dan tidak lagi menyarankan tombol browser. Daftar SHA-1 terdaftar dibuat sebagai `REGISTERED_SHA1`.
- `AuthScreen.kt`: teks error bisa ditekan lama untuk disalin.
- `build-apk.yml`: menampilkan sidik jari di ringkasan build dan menyediakan keystore baru sebagai artifact.

Catatan: kode belum saya kompilasi (tidak ada akses jaringan di sini), jadi status build **NOT VERIFIED**. Perubahannya kecil dan hanya memakai API yang sudah ada.

---

## `app/src/main/java/com/example/core/AuthHelper.kt`

````kotlin
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

/**
 * SHA-1 yang SUDAH terdaftar di Firebase Console
 * (Project settings -> Your apps -> com.aistudio.kafka.xqmzkt -> SHA certificate fingerprints).
 * Setiap kali kamu mendaftarkan sidik jari baru di Firebase, tambahkan juga di sini
 * supaya pesan diagnostik di layar login tetap akurat.
 */
private val REGISTERED_SHA1 = setOf(
    "29:E7:DC:9E:DD:6E:AF:FF:77:40:5F:69:BE:94:27:4B:83:2C:66:41", // keystore AI Studio
    // "XX:XX:XX:...", // <- keystore tetap GitHub Actions (isi setelah DEBUG_KEYSTORE_BASE64 dibuat)
)

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
            val diagnostic = if (isApkSha1Unregistered(context)) {
                signatureMismatchMessage(context)
            } else {
                "Dialog Google dibatalkan (${e.message ?: "cancelled"}). Jika kamu tidak membatalkannya sendiri, coba lagi atau gunakan tombol 'Masuk via Browser Google'."
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
                raw.contains("10:") || raw.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                    raw.contains("28444") || raw.contains("12501") ->
                    signatureMismatchMessage(context)
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
            val raw = e.localizedMessage.orEmpty()
            val message = if (isCertificateRejection(raw)) {
                signatureMismatchMessage(context)
            } else {
                raw.takeIf { it.isNotBlank() }
                    ?: "Gagal masuk via Browser Google (${e::class.simpleName})."
            }
            withContext(Dispatchers.Main.immediate) {
                onAuthError(message)
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
            val raw = e.localizedMessage.orEmpty()
            val message = if (isCertificateRejection(raw)) {
                signatureMismatchMessage(context)
            } else {
                raw.takeIf { it.isNotBlank() }
                    ?: "Gagal menghubungkan Google Workspace (${e::class.simpleName})."
            }
            withContext(Dispatchers.Main.immediate) {
                onError(message)
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

private fun isCertificateRejection(raw: String): Boolean =
    raw.contains("package certificate hash", ignoreCase = true) ||
        raw.contains("certificate hash", ignoreCase = true) ||
        raw.contains("SHA-1", ignoreCase = true) ||
        raw.contains("app is not authorized", ignoreCase = true) ||
        raw.contains("DEVELOPER_ERROR", ignoreCase = true)

/** true hanya jika SHA-1 APK terbaca DAN belum ada di REGISTERED_SHA1. */
private fun isApkSha1Unregistered(context: Context): Boolean {
    val sha1 = getApkSha1Fingerprint(context) ?: return false
    return REGISTERED_SHA1.none { it.equals(sha1, ignoreCase = true) }
}

/**
 * Pesan yang sama untuk tombol "Masuk dengan Google" dan "Masuk via Browser Google",
 * karena keduanya gagal oleh penyebab yang sama: tanda tangan (SHA) APK belum terdaftar di Firebase.
 */
private fun signatureMismatchMessage(context: Context): String {
    val sha1 = getApkSha1Fingerprint(context) ?: "tidak terbaca"
    val sha256 = getApkSha256Fingerprint(context) ?: "tidak terbaca"
    return "Login Google ditolak karena tanda tangan APK ini belum terdaftar di Firebase " +
        "(kedua tombol login ikut terdampak, jadi tombol browser bukan solusi).\n\n" +
        "SHA-1: $sha1\n" +
        "SHA-256: $sha256\n\n" +
        "Perbaikan: daftarkan kedua sidik jari di atas di Firebase Console -> Project settings -> " +
        "aplikasi com.aistudio.kafka.xqmzkt, atau build memakai keystore tetap " +
        "(secret DEBUG_KEYSTORE_BASE64 di GitHub) lalu pasang ulang APK."
}

fun getApkSha1Fingerprint(context: Context): String? = getApkFingerprint(context, "SHA-1")

fun getApkSha256Fingerprint(context: Context): String? = getApkFingerprint(context, "SHA-256")

@Suppress("DEPRECATION")
private fun getApkFingerprint(context: Context, algorithm: String): String? {
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
        val digest = MessageDigest.getInstance(algorithm).digest(certBytes)
        digest.joinToString(":") { byte -> "%02X".format(byte) }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to inspect APK $algorithm: ${e.message}")
        null
    }
}
````

## `app/src/main/java/com/example/ui/AuthScreen.kt`

````kotlin
package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowForward
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.Code
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.Language
import androidx.compose.material.icons.sharp.Mail
import androidx.compose.material.icons.sharp.Public
import androidx.compose.material.icons.sharp.School
import androidx.compose.material.icons.sharp.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.WorkspaceSessionStore
import com.example.core.checkPendingGoogleOAuthResult
import com.example.core.onGoogleSignInClicked
import com.example.core.onGoogleWebOAuthSignInClicked
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    auth: FirebaseAuth = Firebase.auth,
    authScope: CoroutineScope = rememberCoroutineScope(),
    onBeforeInteractiveSignIn: () -> Unit = {},
    onAuthSuccess: (FirebaseUser?) -> Unit,
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val heroDrawableRes = remember(context) { resolveSeductiveHeroDrawable(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auth) {
        val pending = checkPendingGoogleOAuthResult(auth)
        if (pending?.first != null) {
            WorkspaceSessionStore.setAccessToken(pending.second)
            onAuthSuccess(pending.first)
        }
    }

    val allureTransition = rememberInfiniteTransition(label = "kafka_allure_hero")
    val allureSway by allureTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val allureScale by allureTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Seductive Dark-Fantasy Asymmetrical HD Hero Showcase
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(336.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Subtle crimson aura mist behind Kafka portrait
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Crisp HD seductive leaning pose from reference sheet, positioned on the right
            Image(
                painter = painterResource(id = heroDrawableRes),
                contentDescription = stringResource(R.string.cd_anime_mascot),
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = allureScale
                        scaleY = allureScale
                        rotationZ = allureSway * 0.30f
                    }
            )

            // Horizontal feather from solid obsidian on left into crystal-clear HD portrait on right
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                            0.26f to MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                            0.44f to MaterialTheme.colorScheme.background.copy(alpha = 0.16f),
                            0.62f to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    )
            )

            // Top & bottom obsidian mist fade so portrait melts seamlessly into UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                            0.25f to Color.Transparent,
                            0.74f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
            )

            AnimeSparkleParticles(modifier = Modifier.fillMaxSize())

            // Left foreground alluring dialogue & identity
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.60f)
                    .align(Alignment.BottomStart)
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuccubusBatWingCrest(size = 13.dp)
                        Text(
                            text = "SUCCUBUS • AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "「…また、来たの？」",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kafka",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    SuccubusFourPointStar(
                        size = 18.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "…Datang lagi? Aku sudah menunggumu~ Biar aku yang temani risetmu malam ini.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SuccubusGothicDivider()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuccubusSpecBadge("Role: Succubus")
                SuccubusSpecBadge("Race: Demon")
                SuccubusSpecBadge("Theme: Dark Fantasy")
            }

            Text(
                text = "Partner riset otonom yang menelusuri web langsung, merangkum & memverifikasi jurnal ilmiah, merakit skill GitHub, serta mengekspor Word, PDF & PPTX.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Gothic Capability Matrix Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outline
                        )
                    )
                ),
                modifier = Modifier.drawSuccubusCornerSigils(
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuccubusHornCrest(size = 15.dp)
                        Text(
                            text = "KEMAMPUAN INTI KAFKA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    SuccubusGothicDivider()

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CapabilityPill(Icons.Sharp.Public, "Live Web Search")
                        CapabilityPill(Icons.Sharp.School, "Jurnal & GitHub Papers")
                        CapabilityPill(Icons.Sharp.VerifiedUser, "Verifikasi Multi-Sumber")
                        CapabilityPill(Icons.Sharp.Mail, "Gmail & Workspace")
                        CapabilityPill(Icons.Sharp.Description, "Word, PDF & PPTX")
                        CapabilityPill(Icons.Sharp.Code, "Skill Hub GitHub")
                        CapabilityPill(Icons.Sharp.AutoAwesome, "Buat Skill dari 0")
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f))
                ) {
                    // SelectionContainer: tekan lama untuk menyalin SHA-1 / SHA-256 dari pesan error.
                    SelectionContainer {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onBeforeInteractiveSignIn()
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        auth = auth,
                        onAuthSuccess = { signedUser ->
                            isLoading = false
                            onAuthSuccess(signedUser)
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = authScope,
                        onAuthCancelled = { diagnostic ->
                            isLoading = false
                            if (!diagnostic.isNullOrBlank()) {
                                errorMessage = diagnostic
                            }
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.sign_in_google),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Sharp.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onBeforeInteractiveSignIn()
                    onGoogleWebOAuthSignInClicked(
                        context = context,
                        auth = auth,
                        includeWorkspaceScopes = false,
                        onAuthSuccess = { signedUser, token ->
                            isLoading = false
                            WorkspaceSessionStore.setAccessToken(token)
                            onAuthSuccess(signedUser)
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = authScope
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_browser_oauth_button"),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Sharp.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Masuk via Browser Google (Alternatif)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Sesi riset, Gmail & Google Workspace, skill kustom, dan arsip dokumen tersinkronisasi aman ke akun Google Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccubusSpecBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            SuccubusFourPointStar(
                size = 9.dp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapabilityPill(icon: ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@SuppressLint("DiscouragedApi")
private fun resolveSeductiveHeroDrawable(context: Context): Int {
    val dynamicId = context.resources.getIdentifier(
        "img_kafka_seductive_hero",
        "drawable",
        context.packageName
    )
    return if (dynamicId != 0) dynamicId else R.drawable.img_kafka_anime_icon
}
````

## `.github/workflows/build-apk.yml`

````yaml
name: Build Android APK

on:
  push:
  pull_request:
  workflow_dispatch:
    inputs:
      build_type:
        description: "Build type (debug or release)"
        required: true
        default: "debug"
        type: choice
        options:
          - debug
          - release

jobs:
  build-apk:
    name: Build Kafka APK v1.2
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Set up Android SDK
        shell: bash
        run: |
          SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
          if [ ! -d "$SDK_DIR" ]; then
            mkdir -p "$SDK_DIR" 2>/dev/null || sudo mkdir -p "$SDK_DIR" 2>/dev/null || true
          fi
          if [ ! -w "$SDK_DIR" ] || { [ -e "$SDK_DIR/licenses" ] && [ ! -w "$SDK_DIR/licenses" ]; } || { [ -e "$SDK_DIR/platforms" ] && [ ! -w "$SDK_DIR/platforms" ]; }; then
            sudo chown -R "$(id -u)":"$(id -g)" "$SDK_DIR" 2>/dev/null || true
          fi
          if [ ! -w "$SDK_DIR" ]; then
            SDK_DIR="$HOME/android-sdk"
            mkdir -p "$SDK_DIR"
          fi
          mkdir -p "$SDK_DIR/licenses"
          if [ ! -w "$SDK_DIR/licenses" ]; then
            sudo chown -R "$(id -u)":"$(id -g)" "$SDK_DIR/licenses" 2>/dev/null || true
          fi

          echo "ANDROID_HOME=$SDK_DIR" >> "$GITHUB_ENV"
          echo "ANDROID_SDK_ROOT=$SDK_DIR" >> "$GITHUB_ENV"

          printf "\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n8933bad161af4178b1185d1a37fbf41ea5269c55\n" > "$SDK_DIR/licenses/android-sdk-license" || true
          printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n" > "$SDK_DIR/licenses/android-sdk-preview-license" || true
          printf "\n33b6a2b64607f11b759f320ef9dff4ae5c47d97a\n" > "$SDK_DIR/licenses/google-gdk-license" || true

          SDKMANAGER=""
          for candidate in \
            "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" \
            /usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager \
            $(find "$SDK_DIR" /usr/local/lib/android/sdk -path "*/cmdline-tools/*/bin/sdkmanager" -type f 2>/dev/null || true) \
            "$(command -v sdkmanager 2>/dev/null || true)"; do
            if [ -n "$candidate" ] && [ -x "$candidate" ]; then
              SDKMANAGER="$candidate"
              break
            fi
          done

          if [ -n "$SDKMANAGER" ]; then
            echo "Using sdkmanager: $SDKMANAGER"
            (yes || true) | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses || true
            "$SDKMANAGER" --sdk_root="$SDK_DIR" "platform-tools" "platforms;android-36" "build-tools;36.0.0" || true
            "$SDKMANAGER" --sdk_root="$SDK_DIR" "platforms;android-36.1" || true
          else
            echo "sdkmanager not found; Gradle will auto-download required SDK packages into $SDK_DIR"
          fi

          if [ -d "$SDK_DIR/platforms/android-36.0" ] && [ ! -e "$SDK_DIR/platforms/android-36" ]; then
            ln -s "$SDK_DIR/platforms/android-36.0" "$SDK_DIR/platforms/android-36" 2>/dev/null || true
          fi
          echo "Configured ANDROID_HOME=$SDK_DIR"
          ls -la "$SDK_DIR/platforms" 2>/dev/null || true

      - name: Set up Gradle 9.3.1
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: "9.3.1"

      - name: Prepare debug keystore & env
        id: keystore
        env:
          DEBUG_KEYSTORE_BASE64: ${{ secrets.DEBUG_KEYSTORE_BASE64 }}
          GOOGLE_SERVICES_JSON: ${{ secrets.GOOGLE_SERVICES_JSON }}
          GOOGLE_SERVICES_JSON_BASE64: ${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}
        shell: bash
        run: |
          GENERATED=false
          if [ -n "$DEBUG_KEYSTORE_BASE64" ]; then
            echo "Decoding GitHub Secret DEBUG_KEYSTORE_BASE64 -> debug.keystore"
            printf '%s' "$DEBUG_KEYSTORE_BASE64" | tr -d '[:space:]' | base64 -d > debug.keystore
          elif [ -f debug.keystore.base64 ]; then
            echo "Decoding repo debug.keystore.base64 -> debug.keystore"
            tr -d '[:space:]' < debug.keystore.base64 | base64 -d > debug.keystore
          elif [ ! -f debug.keystore ]; then
            echo "::warning::Keystore tetap tidak ditemukan. Membuat keystore SEMENTARA - SHA-1 akan BERBEDA di setiap build dan login Google akan ditolak Firebase."
            keytool -genkeypair -v \
              -keystore debug.keystore \
              -storepass android \
              -alias androiddebugkey \
              -keypass android \
              -keyalg RSA \
              -keysize 2048 \
              -validity 10000 \
              -dname "CN=Android Debug,O=Android,C=US"
            GENERATED=true
          fi
          echo "generated=$GENERATED" >> "$GITHUB_OUTPUT"

          # Tampilkan sidik jari yang HARUS didaftarkan di Firebase Console
          FP=$(keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android | grep -E "SHA1:|SHA256:" || true)
          echo "$FP"
          {
            echo "## Sidik jari keystore yang dipakai build ini"
            echo '```'
            echo "$FP"
            echo '```'
            echo "Daftarkan SHA1 dan SHA256 di Firebase Console -> Project settings -> aplikasi com.aistudio.kafka.xqmzkt."
            if [ "$GENERATED" = "true" ]; then
              echo ""
              echo "**PERINGATAN:** keystore ini dibuat otomatis dan tidak tetap. Unduh artifact \`keystore-baru\`, "
              echo "salin isi debug.keystore.base64 ke GitHub Secret \`DEBUG_KEYSTORE_BASE64\`, lalu daftarkan SHA-1 di atas ke Firebase."
            fi
          } >> "$GITHUB_STEP_SUMMARY"

          # Simpan base64 keystore sementara agar bisa dijadikan secret (hanya bila baru dibuat)
          if [ "$GENERATED" = "true" ]; then
            mkdir -p keystore-out
            base64 -w0 debug.keystore > keystore-out/debug.keystore.base64
          fi

          GS_SECRET="${GOOGLE_SERVICES_JSON:-$GOOGLE_SERVICES_JSON_BASE64}"
          if [ ! -f app/google-services.json ] && [ -n "$GS_SECRET" ]; then
            TRIMMED_GS="$(printf '%s' "$GS_SECRET" | sed -e 's/^[[:space:]]*//')"
            if [[ "$TRIMMED_GS" == \{* ]]; then
              printf '%s\n' "$GS_SECRET" > app/google-services.json
            else
              printf '%s' "$GS_SECRET" | tr -d '[:space:]' | base64 -d > app/google-services.json
            fi
          fi
          touch .env
          ls -lh debug.keystore .env
          if [ -f app/google-services.json ]; then
            ls -lh app/google-services.json
          else
            echo "Note: app/google-services.json not found in repo; Gradle will continue via MissingGoogleServicesStrategy.WARN"
          fi

      - name: Upload keystore baru (hanya jika dibuat otomatis)
        if: ${{ steps.keystore.outputs.generated == 'true' }}
        uses: actions/upload-artifact@v4
        with:
          name: keystore-baru
          path: keystore-out/debug.keystore.base64
          retention-days: 3
          if-no-files-found: error

      - name: Build Debug APK
        if: ${{ github.event_name != 'workflow_dispatch' || inputs.build_type == 'debug' }}
        run: |
          gradle --version
          BUILD_OK=false
          for attempt in 1 2 3; do
            if gradle :app:assembleDebug --no-daemon --no-configuration-cache --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8" --stacktrace; then
              BUILD_OK=true
              break
            fi
            echo "Gradle debug build attempt $attempt failed; retrying in 12s..."
            sleep 12
          done
          [ "$BUILD_OK" = "true" ] || exit 1
          mkdir -p dist-apk
          SRC_APK=$(find app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' | head -n 1)
          [ -n "$SRC_APK" ] || { echo "Debug APK not produced."; exit 1; }
          cp "$SRC_APK" "dist-apk/kafka-v1.2-debug-b${{ github.run_number }}.apk"
          ls -lh dist-apk

      - name: Build Release APK
        if: ${{ github.event_name == 'workflow_dispatch' && inputs.build_type == 'release' }}
        env:
          RELEASE_KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }}
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        shell: bash
        run: |
          if [ -n "$RELEASE_KEYSTORE_BASE64" ] && [ -n "$STORE_PASSWORD" ] && [ -n "$KEY_PASSWORD" ]; then
            echo "Using release signing keystore from GitHub Secrets."
            echo "$RELEASE_KEYSTORE_BASE64" | tr -d '[:space:]' | base64 -d > my-upload-key.jks
            export KEYSTORE_PATH="$GITHUB_WORKSPACE/my-upload-key.jks"
          elif [ -f debug.keystore ]; then
            echo "Converting debug.keystore to release upload keystore so SHA-1 stays synced with Firebase."
            rm -f my-upload-key.jks
            keytool -importkeystore -noprompt \
              -srckeystore debug.keystore -srcstorepass android -srcalias androiddebugkey -srckeypass android \
              -destkeystore my-upload-key.jks -deststorepass android -destalias upload -destkeypass android
            export KEYSTORE_PATH="$GITHUB_WORKSPACE/my-upload-key.jks"
            export STORE_PASSWORD="android"
            export KEY_PASSWORD="android"
          else
            echo "Release secrets and debug.keystore missing; generating temporary upload keystore."
            keytool -genkeypair -v \
              -keystore my-upload-key.jks \
              -storepass android \
              -alias upload \
              -keypass android \
              -keyalg RSA \
              -keysize 2048 \
              -validity 10000 \
              -dname "CN=Kafka Release,O=Kafka,C=ID"
            export KEYSTORE_PATH="$GITHUB_WORKSPACE/my-upload-key.jks"
            export STORE_PASSWORD="android"
            export KEY_PASSWORD="android"
          fi
          gradle --version
          BUILD_OK=false
          for attempt in 1 2 3; do
            if gradle :app:assembleRelease --no-daemon --no-configuration-cache --max-workers=2 -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8" --stacktrace; then
              BUILD_OK=true
              break
            fi
            echo "Gradle release build attempt $attempt failed; retrying in 12s..."
            sleep 12
          done
          [ "$BUILD_OK" = "true" ] || exit 1
          mkdir -p dist-apk
          SRC_APK=$(find app/build/outputs/apk/release -maxdepth 1 -name '*.apk' | head -n 1)
          [ -n "$SRC_APK" ] || { echo "Release APK not produced."; exit 1; }
          cp "$SRC_APK" "dist-apk/kafka-v1.2-release-b${{ github.run_number }}.apk"
          ls -lh dist-apk

      - name: Upload Debug APK Artifact
        if: ${{ github.event_name != 'workflow_dispatch' || inputs.build_type == 'debug' }}
        uses: actions/upload-artifact@v4
        with:
          name: kafka-v1.2-debug-b${{ github.run_number }}
          path: dist-apk/kafka-v1.2-debug-*.apk
          if-no-files-found: error
          retention-days: 30

      - name: Upload Release APK Artifact
        if: ${{ github.event_name == 'workflow_dispatch' && inputs.build_type == 'release' }}
        uses: actions/upload-artifact@v4
        with:
          name: kafka-v1.2-release-b${{ github.run_number }}
          path: dist-apk/kafka-v1.2-release-*.apk
          if-no-files-found: error
          retention-days: 30

      - name: Upload Debug APK ke GitHub Releases (v1.2)
        if: ${{ github.event_name == 'push' || (github.event_name == 'workflow_dispatch' && inputs.build_type == 'debug') }}
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v1.2.${{ github.run_number }}
          name: Kafka Debug APK v1.2.${{ github.run_number }}
          files: dist-apk/kafka-v1.2-debug-*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Upload Release APK ke GitHub Releases (v1.2)
        if: ${{ github.event_name == 'workflow_dispatch' && inputs.build_type == 'release' }}
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v1.2.${{ github.run_number }}
          name: Kafka Release APK v1.2.${{ github.run_number }}
          files: dist-apk/kafka-v1.2-release-*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
````

