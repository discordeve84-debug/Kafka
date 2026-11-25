package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowForward
import androidx.compose.material.icons.automirrored.sharp.TrendingUp
import androidx.compose.material.icons.sharp.AccountTree
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.Bookmarks
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.Forum
import androidx.compose.material.icons.sharp.Hub
import androidx.compose.material.icons.sharp.Mail
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Public
import androidx.compose.material.icons.sharp.School
import androidx.compose.material.icons.sharp.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AgentMode
import com.example.model.AgentSession
import com.example.model.TrendingResearchTopic
import com.example.ui.theme.KafkaAmber
import com.example.ui.theme.KafkaVerifiedGreen

@Composable
fun HomeScreen(
    viewModel: KafkaViewModel,
    userDisplayName: String,
    onOpenChat: (AgentMode?) -> Unit,
    onOpenResearch: (Int) -> Unit,
    onOpenSkills: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenWorkspaceDashboard: () -> Unit,
    onOpenWorkspaceLibrary: () -> Unit,
    workspaceConnected: Boolean = false,
    onOpenGoogleWorkspace: (Int) -> Unit = {},
) {
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val skillsState by viewModel.skillsState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val libraryState by viewModel.researchLibraryState.collectAsStateWithLifecycle()
    val trendingTopics by viewModel.trendingTopics.collectAsStateWithLifecycle()
    val execution by viewModel.agentExecution.collectAsStateWithLifecycle()

    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
    val skills = (skillsState as? UiState.Success)?.data ?: emptyList()
    val artifacts = (artifactsState as? UiState.Success)?.data ?: emptyList()
    val snapshots = (libraryState as? UiState.Success)?.data ?: emptyList()

    val activeSkillCount = skills.count { it.enabled }
    val firstName = userDisplayName.trim().substringBefore(" ").ifBlank { "Peneliti" }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val floatTransition = rememberInfiniteTransition(label = "anime_mascot_float")
    val mascotFloatY by floatTransition.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Anime Girl Companion Hero Card
            item {
                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn(tween(320)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    ) { -24 }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawSuccubusCornerSigils(
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.tertiary,
                                accentLength = 12.dp
                            )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Atmospheric anime atelier background
                            Image(
                                painter = painterResource(id = R.drawable.img_kafka_anime_hero),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = 0.36f }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                            AnimeSparkleParticles(modifier = Modifier.matchParentSize())

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SuccubusBatWingCrest(size = 12.dp)
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (execution.isRunning) {
                                                            MaterialTheme.colorScheme.secondary
                                                        } else {
                                                            KafkaVerifiedGreen
                                                        }
                                                    )
                                            )
                                            Text(
                                                text = if (execution.isRunning) "KAFKA SEDANG RISET..." else "SUCCUBUS • READY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Selamat datang, $firstName",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        SuccubusFourPointStar(
                                            size = 14.dp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    Text(
                                        text = "Demon • Dark Fantasy • Partner riset AI siap mencari web live, jurnal DOI, verifikasi & menyusun dokumen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Button(
                                        onClick = { onOpenChat(null) },
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("home_start_chat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Sharp.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Buka Workspace Chat", maxLines = 1)
                                    }
                                }

                                // Floating Kafka Succubus Full-Body Portrait
                                Box(
                                    modifier = Modifier
                                        .width(106.dp)
                                        .height(162.dp)
                                        .offset(y = mascotFloatY.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        border = BorderStroke(
                                            1.5.dp,
                                            Brush.verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_kafka_anime_mascot),
                                                contentDescription = stringResource(R.string.cd_anime_mascot),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            SuccubusFourPointStar(
                                                size = 13.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Compact Status Tiles (Sessions / Active Skills / Research Library)
            item {
                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn(tween(420, delayMillis = 80)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    ) { 20 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeStatPill(
                            icon = Icons.Sharp.Hub,
                            value = "${sessions.size}",
                            label = "Proyek",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onOpenWorkspaceDashboard,
                            modifier = Modifier.weight(1f)
                        )
                        HomeStatPill(
                            icon = Icons.Sharp.AccountTree,
                            value = "$activeSkillCount",
                            label = "Skill Aktif",
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onOpenSkills,
                            modifier = Modifier.weight(1f)
                        )
                        HomeStatPill(
                            icon = Icons.Sharp.Bookmarks,
                            value = "${snapshots.size}",
                            label = "Library",
                            accent = MaterialTheme.colorScheme.tertiary,
                            onClick = onOpenWorkspaceLibrary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Quick Launch Modules (2x2 Grid with Bouncy Spring Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuccubusHornCrest(size = 14.dp)
                        Text(
                            text = "STUDIO KEMAMPUAN KAFKA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeFeatureCard(
                            title = "Live Web Search",
                            subtitle = "Cari & audit web real-time",
                            icon = Icons.Sharp.Public,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onOpenResearch(0) },
                            modifier = Modifier.weight(1f),
                            testTag = "home_card_web"
                        )
                        HomeFeatureCard(
                            title = "Jurnal & Verifikasi",
                            subtitle = "OpenAlex, arXiv & cek fakta",
                            icon = Icons.Sharp.School,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = { onOpenResearch(1) },
                            modifier = Modifier.weight(1f),
                            testTag = "home_card_journal"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeFeatureCard(
                            title = "Skill Tree & GitHub",
                            subtitle = "${skills.size} node & agen dari 0",
                            icon = Icons.Sharp.AccountTree,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = onOpenSkills,
                            modifier = Modifier.weight(1f),
                            testTag = "home_card_skills"
                        )
                        HomeFeatureCard(
                            title = "Word, PDF & PPTX",
                            subtitle = "${artifacts.size} dokumen tersimpan",
                            icon = Icons.Sharp.Description,
                            accentColor = KafkaAmber,
                            onClick = onOpenStudio,
                            modifier = Modifier.weight(1f),
                            testTag = "home_card_studio"
                        )
                    }

                    HomeFeatureCard(
                        title = "Verifikasi Klaim, Jurnal & Tautan",
                        subtitle = "Cek keabsahan isi, DOI, & status HTTP web secara real-time",
                        icon = Icons.Sharp.VerifiedUser,
                        accentColor = KafkaVerifiedGreen,
                        onClick = { onOpenResearch(2) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "home_card_verify"
                    )

                    HomeFeatureCard(
                        title = if (workspaceConnected) "Google Workspace Hub (Terhubung)" else "Google Workspace Hub",
                        subtitle = "Gmail, Drive, Docs, Sheets, Calendar & Contacts terintegrasi",
                        icon = Icons.Sharp.Mail,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onOpenGoogleWorkspace(0) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "home_card_google_workspace"
                    )
                }
            }

            // 4. Trending Live-Web Topics Carousel
            if (trendingTopics.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Sharp.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "TREN RISET SAAT INI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = onOpenWorkspaceDashboard) {
                                Text("Semua →")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            trendingTopics.forEach { topic ->
                                HomeTrendingPillCard(
                                    topic = topic,
                                    onClick = {
                                        onOpenChat(AgentMode.WEB_SEARCH)
                                        viewModel.sendAgentPrompt(topic.suggestedQuery, overrideMode = AgentMode.WEB_SEARCH)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recent Research Projects (Quick Resume)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROYEK RISET TERAKHIR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                viewModel.createNewSession("Sesi Riset Baru")
                                onOpenChat(AgentMode.AGENT)
                            }
                        ) {
                            Text("+ Sesi Baru")
                        }
                    }

                    if (sessions.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Belum ada sesi aktif. Mulai percakapan riset pertama bersama Kafka!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { onOpenChat(AgentMode.AGENT) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Mulai Sesi Pertama")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sessions.take(3).forEach { sess ->
                                HomeSessionQuickCard(
                                    session = sess,
                                    onResume = {
                                        viewModel.selectSession(sess.id)
                                        onOpenChat(null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HomeStatPill(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawSuccubusCornerSigils(
                primaryColor = accentColor,
                secondaryColor = MaterialTheme.colorScheme.tertiary,
                accentLength = 8.dp
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SuccubusFourPointStar(
                        size = 10.dp,
                        color = accentColor.copy(alpha = 0.85f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Sharp.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeTrendingPillCard(
    topic: TrendingResearchTopic,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.48f)),
        modifier = Modifier.width(216.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = topic.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
                Text(
                    text = topic.momentumLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = topic.topic,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeSessionQuickCard(
    session: AgentSession,
    onResume: () -> Unit,
) {
    Surface(
        onClick = onResume,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Sharp.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Sharp.VerifiedUser,
                        contentDescription = null,
                        tint = KafkaVerifiedGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${AgentMode.fromRaw(session.mode).label} • Trust ${session.verifiedScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Sharp.PlayArrow,
                        contentDescription = "Buka sesi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
