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
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
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

