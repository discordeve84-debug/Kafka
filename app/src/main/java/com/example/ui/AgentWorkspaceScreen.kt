package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AgentMessage
import com.example.model.AgentMode
import com.example.model.AgentSession
import com.example.model.AgentSkill
import com.example.model.CitationItem
import com.example.model.OutputFormat
import com.example.model.VerificationStatus
import com.example.ui.theme.KafkaAmber
import com.example.ui.theme.KafkaErrorRed
import com.example.ui.theme.KafkaVerifiedGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentWorkspaceScreen(
    viewModel: KafkaViewModel,
    workspaceSubView: Int = 0,
    onWorkspaceSubViewChange: (Int) -> Unit = {},
    onNavigateToDocStudio: () -> Unit,
    onNavigateToVerify: (String) -> Unit,
) {
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val skillsState by viewModel.skillsState.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val execution by viewModel.agentExecution.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceInputState.collectAsStateWithLifecycle()
    val previewState by viewModel.documentPreviewState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val libraryState by viewModel.researchLibraryState.collectAsStateWithLifecycle()
    val trendingTopics by viewModel.trendingTopics.collectAsStateWithLifecycle()
    val isRefreshingTrends by viewModel.isRefreshingTrends.collectAsStateWithLifecycle()
    val isSavingSnapshot by viewModel.isSavingSnapshot.collectAsStateWithLifecycle()
    val autoSpeakEnabled by viewModel.autoSpeakEnabled.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val workspaceView = workspaceSubView
    var promptInput by remember { mutableStateOf("") }
    var showSessionsSheet by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var selectedCitation by remember { mutableStateOf<CitationItem?>(null) }
    var micPermissionError by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionError = false
            viewModel.startVoiceRecording()
        } else {
            micPermissionError = true
        }
    }

    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
    val activeSession = sessions.firstOrNull { it.id == selectedSessionId }
    val allSkills = (skillsState as? UiState.Success)?.data ?: emptyList()
    val enabledSkills = if (activeSession != null && activeSession.activeSkillIds.isNotEmpty()) {
        allSkills.filter { it.id in activeSession.activeSkillIds || it.slug in activeSession.activeSkillIds }
    } else {
        allSkills.filter { it.enabled }
    }
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val artifacts = (artifactsState as? UiState.Success)?.data ?: emptyList()
    val snapshots = (libraryState as? UiState.Success)?.data ?: emptyList()

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, execution.isRunning, execution.streamingText.length, execution.error) {
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hermes Studio Sub-Header (Session + Active Skills)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = { showSessionsSheet = true },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("session_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = activeSession?.title ?: "Workspace Kafka",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = "Ganti sesi",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${enabledSkills.size} SKILL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleAutoSpeak() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("toggle_auto_speak_button")
                            ) {
                                Icon(
                                    imageVector = if (autoSpeakEnabled || isSpeaking) {
                                        Icons.Rounded.VolumeUp
                                    } else {
                                        Icons.Rounded.VolumeOff
                                    },
                                    contentDescription = "Mode percakapan suara (TTS)",
                                    tint = if (autoSpeakEnabled || isSpeaking) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = { showSnapshotDialog = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("snapshot_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = stringResource(R.string.cd_save_snapshot),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.createNewSession() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("new_session_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.cd_new_session),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Workspace Sub-View Switcher (Chat | Research Dashboard | Research Library)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = workspaceView == 0,
                            onClick = { onWorkspaceSubViewChange(0) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Chat", style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.testTag("workspace_tab_chat")
                        )
                        FilterChip(
                            selected = workspaceView == 1,
                            onClick = { onWorkspaceSubViewChange(1) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = "Dashboard (${sessions.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("workspace_tab_dashboard")
                        )
                        FilterChip(
                            selected = workspaceView == 2,
                            onClick = { onWorkspaceSubViewChange(2) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Bookmarks, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = "Library (${snapshots.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("workspace_tab_library")
                        )
                    }

                    if (workspaceView == 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Mode selector chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AgentMode.entries.forEach { mode ->
                                val selected = activeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setMode(mode) },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Main Workspace Body (Animated transition between Chat | Research Dashboard | Research Library)
            AnimatedContent(
                targetState = workspaceView,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                },
                label = "workspace_subview_anim",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { targetView ->
                when (targetView) {
                    1 -> {
                        ResearchDashboardPanel(
                            sessions = sessions,
                            selectedSessionId = selectedSessionId,
                            skills = allSkills,
                            artifacts = artifacts,
                            snapshotsCount = snapshots.size,
                            trendingTopics = trendingTopics,
                            isRefreshingTrends = isRefreshingTrends,
                            onRefreshTrends = { viewModel.refreshTrendingTopics() },
                            onSelectSessionAndOpenChat = { sid ->
                                viewModel.selectSession(sid)
                                onWorkspaceSubViewChange(0)
                            },
                            onLaunchTopicResearch = { query ->
                                onWorkspaceSubViewChange(0)
                                viewModel.sendAgentPrompt(query, overrideMode = AgentMode.WEB_SEARCH)
                            },
                            onCreateNewProject = {
                                viewModel.createNewSession("Proyek Riset Baru")
                                onWorkspaceSubViewChange(0)
                            }
                        )
                    }

                    2 -> {
                        ResearchLibraryPanel(
                            libraryState = libraryState,
                            onTakeSnapshotClick = { showSnapshotDialog = true },
                            onRestoreSnapshot = { snap, openPreview ->
                                viewModel.restoreSnapshotToWorkspace(snap, openPreview) {
                                    onWorkspaceSubViewChange(0)
                                }
                            },
                            onInspectCitation = { selectedCitation = it },
                            onDeleteSnapshot = { viewModel.deleteSnapshot(it) }
                        )
                    }

                    else -> {
                        // Message Feed
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Inline Compact Research Dashboard & Trending Strip
                                if (sessions.isNotEmpty() || trendingTopics.isNotEmpty()) {
                                    item {
                                        Surface(
                                            onClick = { onWorkspaceSubViewChange(1) },
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("inline_dashboard_banner")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.TrendingUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                val topTrend = trendingTopics.firstOrNull()?.topic
                                                Text(
                                                    text = if (!topTrend.isNullOrBlank()) {
                                                        "${sessions.size} Proyek • Tren: $topTrend"
                                                    } else {
                                                        "${sessions.size} Proyek Riset Aktif"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Dashboard →",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            if (messages.isEmpty() && !execution.isRunning) {
                                item {
                                    HermesWelcomeCard(
                                        enabledSkills = enabledSkills,
                                        onQuickPrompt = { starter ->
                                            promptInput = starter
                                            viewModel.sendAgentPrompt(starter)
                                            promptInput = ""
                                        }
                                    )
                                }
                            }

                            items(messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                                if (msg.role == "USER") {
                                    UserPromptBubble(msg.content)
                                } else {
                                    AgentResponseCard(
                                        message = msg,
                                        isSpeaking = isSpeaking,
                                        onSpeak = { viewModel.speakAgentMessage(msg.content) },
                                        onCitationClick = { selectedCitation = it },
                                        onPreviewFormat = { format ->
                                            viewModel.openDocumentPreview(
                                                topicOrContent = msg.content,
                                                format = format,
                                                citations = msg.parsedCitations()
                                            )
                                        },
                                        onVerifyAgain = {
                                            viewModel.runContentVerification(msg.content)
                                            onNavigateToVerify(msg.content)
                                        }
                                    )
                                }
                            }

                            if (execution.isRunning || execution.error != null) {
                                item {
                                    LiveHermesExecutionCard(execution)
                                }
                            }
                        }
                    }
                }
                }
            }

            // Live AudioRecord Hands-Free Voice HUD
            AnimatedVisibility(
                visible = voiceState.isRecording || voiceState.isTranscribing || micPermissionError
            ) {
                VoiceRecordingHudBanner(
                    voiceState = voiceState,
                    micPermissionError = micPermissionError,
                    onStopAndSend = { viewModel.stopVoiceRecordingAndProcess(autoSend = true) },
                    onStopToDraft = {
                        viewModel.stopVoiceRecordingAndProcess(autoSend = false) { transcript ->
                            promptInput = transcript
                        }
                    },
                    onCancel = {
                        micPermissionError = false
                        viewModel.cancelVoiceRecording()
                    }
                )
            }

            // Bottom Hermes Command Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                text = "Ketik atau bicara untuk riset, jurnal, verifikasi, dokumen...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("agent_prompt_input"),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (promptInput.isNotBlank() && !execution.isRunning) {
                                    viewModel.sendAgentPrompt(promptInput)
                                    promptInput = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )

                    // Hands-Free AudioRecord Microphone Button
                    Surface(
                        onClick = {
                            if (voiceState.isRecording) {
                                viewModel.stopVoiceRecordingAndProcess(autoSend = true)
                            } else if (!voiceState.isTranscribing) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    micPermissionError = false
                                    viewModel.startVoiceRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        enabled = !execution.isRunning && !voiceState.isTranscribing,
                        color = if (voiceState.isRecording) {
                            KafkaErrorRed
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (voiceState.isRecording) KafkaErrorRed else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_input_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (voiceState.isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (voiceState.isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = stringResource(
                                        if (voiceState.isRecording) R.string.cd_stop_voice_input else R.string.cd_voice_input
                                    ),
                                    tint = if (voiceState.isRecording) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                    }

                    // Persistent Send Button
                    Surface(
                        onClick = {
                            if (promptInput.isNotBlank() && !execution.isRunning) {
                                viewModel.sendAgentPrompt(promptInput)
                                promptInput = ""
                            }
                        },
                        enabled = promptInput.isNotBlank() && !execution.isRunning,
                        color = if (promptInput.isNotBlank() && !execution.isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_prompt_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (execution.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = stringResource(R.string.cd_send_prompt),
                                    tint = if (promptInput.isNotBlank()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Read-Only Word / PDF / PPTX Preview Overlay over the chat screen
        DocumentPreviewOverlay(
            state = previewState,
            onSwitchFormat = { viewModel.switchPreviewFormat(it) },
            onSaveToDevice = {
                viewModel.confirmSavePreview(shareAfterSave = false) {
                    onNavigateToDocStudio()
                }
            },
            onSaveAndShare = {
                viewModel.confirmSavePreview(shareAfterSave = true)
            },
            onSaveSnapshot = { showSnapshotDialog = true },
            onDismiss = { viewModel.dismissDocumentPreview() }
        )
    }

    if (showSnapshotDialog) {
        val citationCount = remember(messages, previewState.draft) {
            messages.flatMap { it.parsedCitations() }.distinctBy { it.url.ifBlank { it.title } }.size
        }
        SaveSnapshotDialog(
            defaultTitle = activeSession?.title ?: "Snapshot Riset Kafka",
            messageCount = messages.size,
            citationCount = citationCount,
            hasActivePreview = previewState.draft != null,
            previewFormatLabel = previewState.draft?.format?.displayLabel.orEmpty(),
            isSaving = isSavingSnapshot,
            onConfirm = { title, notes ->
                viewModel.saveCurrentSnapshot(customTitle = title, notes = notes)
                showSnapshotDialog = false
            },
            onDismiss = { showSnapshotDialog = false }
        )
    }

    if (showSessionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSessionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sesi Workspace Kafka",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = {
                            viewModel.createNewSession()
                            showSessionsSheet = false
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sesi Baru")
                    }
                }

                sessions.forEach { sess ->
                    SessionListRow(
                        session = sess,
                        selected = sess.id == selectedSessionId,
                        onSelect = {
                            viewModel.selectSession(sess.id)
                            showSessionsSheet = false
                        },
                        onDelete = { viewModel.deleteSession(sess.id) }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Clickable Citation Detail Bottom Sheet
    val activeCitation = selectedCitation
    if (activeCitation != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCitation = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CitationDetailBottomSheetContent(
                citation = activeCitation,
                onCopyUrl = {
                    val target = activeCitation.url.ifBlank { activeCitation.doi }
                    if (target.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(target))
                    }
                },
                onOpenUrl = { url ->
                    if (url.startsWith("http")) {
                        runCatching { uriHandler.openUri(url) }
                    }
                },
                onVerifySource = {
                    val claimQuery = "${activeCitation.title} — ${activeCitation.snippet}".trim()
                    selectedCitation = null
                    viewModel.runContentVerification(claimQuery)
                    onNavigateToVerify(claimQuery)
                }
            )
        }
    }
}

@Composable
private fun VoiceRecordingHudBanner(
    voiceState: VoiceInputState,
    micPermissionError: Boolean,
    onStopAndSend: () -> Unit,
    onStopToDraft: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (micPermissionError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_recording_hud")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                micPermissionError -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.mic_permission_denied),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onCancel) { Text("Tutup") }
                    }
                }

                voiceState.isTranscribing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Mentranskripsi suara ke kueri riset via Gemini...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                voiceState.isRecording -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(KafkaErrorRed)
                            )
                            Text(
                                text = "Merekam AudioRecord • ${voiceState.elapsedSeconds}d",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        // Real-time amplitude meter bars from AudioRecord RMS
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(22.dp)
                        ) {
                            val multipliers = listOf(0.45f, 0.75f, 1.0f, 0.85f, 0.6f, 0.95f, 0.55f, 0.8f)
                            multipliers.forEach { m ->
                                val barHeight = (4f + 18f * (voiceState.amplitude * m).coerceIn(0.05f, 1f)).dp
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Batal")
                        }
                        OutlinedButton(
                            onClick = onStopToDraft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Isi ke Kolom Teks")
                        }
                        Button(
                            onClick = onStopAndSend,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("voice_send_now_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kirim Suara")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HermesWelcomeCard(
    enabledSkills: List<AgentSkill>,
    onQuickPrompt: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimePulseAvatar(
                    drawableRes = R.drawable.img_kafka_anime_icon,
                    contentDescription = stringResource(R.string.cd_anime_mascot),
                    isActive = true,
                    size = 48.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kafka-Chan Siap Membantu!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${enabledSkills.size} skill aktif • Suara Mikrofon + Live Web + Jurnal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Mulai dengan salah satu alur kerja otonom di bawah, ketik perintah, atau tekan ikon mikrofon:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val starters = listOf(
                "Cari jurnal ilmiah terbaru tentang LLM Reasoning 2024-2025, verifikasi sitasinya, dan rangkum untuk laporan Word",
                "Lakukan live web search tentang perkembangan semikonduktor AI global dan verifikasi klaim utamanya",
                "Rancang struktur presentasi PPTX tentang transisi energi terbarukan di Indonesia beserta referensinya"
            )
            starters.forEachIndexed { idx, prompt ->
                Surface(
                    onClick = { onQuickPrompt(prompt) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("starter_prompt_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPromptBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }
    }
}

@Composable
private fun AgentResponseCard(
    message: AgentMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onCitationClick: (CitationItem) -> Unit,
    onPreviewFormat: (OutputFormat) -> Unit,
    onVerifyAgain: () -> Unit,
) {
    var traceExpanded by remember { mutableStateOf(false) }
    val status = VerificationStatus.fromRaw(message.verificationStatus)
    val citations = remember(message.citations) { message.parsedCitations() }

    val badgeColor = when (status) {
        VerificationStatus.VERIFIED -> KafkaVerifiedGreen
        VerificationStatus.PARTIAL -> KafkaAmber
        VerificationStatus.UNVERIFIED -> KafkaErrorRed
        VerificationStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedBody = remember(message.content, citations, linkColor) {
        buildClickableCitationAnnotatedText(
            rawText = message.content,
            citations = citations,
            linkColor = linkColor,
            onCitationClick = onCitationClick
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Agent Header + Trust Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kafka_anime_icon),
                        contentDescription = stringResource(R.string.cd_anime_mascot),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "KAFKA-CHAN",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (status != VerificationStatus.NONE) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (status == VerificationStatus.VERIFIED) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.WarningAmber
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${message.verificationScore}% ${status.label.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            // Hermes Reasoning Trace Drawer
            if (message.thoughtSteps.isNotEmpty()) {
                Surface(
                    onClick = { traceExpanded = !traceExpanded },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Hermes Trace (${message.thoughtSteps.size} langkah)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Icon(
                                imageVector = if (traceExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        AnimatedVisibility(visible = traceExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                message.thoughtSteps.forEach { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Agent Answer Body with Clickable Inline Citation Tags ([J1], [W1], etc.)
            Text(
                text = annotatedBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (message.verificationSummary.isNotBlank()) {
                Text(
                    text = "Audit: ${message.verificationSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Clickable Citations Horizontal Strip
            if (citations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SITASI & REFERENSI KLIKABLE (${citations.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        citations.forEachIndexed { idx, cit ->
                            CitationChipCard(
                                index = idx + 1,
                                citation = cit,
                                onClick = { onCitationClick(cit) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Read-Only Preview & Export Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickExportPill(
                    icon = Icons.Rounded.Description,
                    label = "Pratinjau Word",
                    onClick = { onPreviewFormat(OutputFormat.DOCX) },
                    testTag = "preview_word_pill"
                )
                QuickExportPill(
                    icon = Icons.Rounded.PictureAsPdf,
                    label = "Pratinjau PDF",
                    onClick = { onPreviewFormat(OutputFormat.PDF) },
                    testTag = "preview_pdf_pill"
                )
                QuickExportPill(
                    icon = Icons.Rounded.Slideshow,
                    label = "Pratinjau PPTX",
                    onClick = { onPreviewFormat(OutputFormat.PPTX) },
                    testTag = "preview_pptx_pill"
                )
                QuickExportPill(
                    icon = Icons.Rounded.VerifiedUser,
                    label = "Verifikasi",
                    onClick = onVerifyAgain,
                    testTag = "verify_again_pill"
                )
                QuickExportPill(
                    icon = if (isSpeaking) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    label = if (isSpeaking) "Hentikan Suara" else "Bacakan",
                    onClick = onSpeak,
                    testTag = "speak_response_pill"
                )
            }
        }
    }
}

private fun buildClickableCitationAnnotatedText(
    rawText: String,
    citations: List<CitationItem>,
    linkColor: Color,
    onCitationClick: (CitationItem) -> Unit
): AnnotatedString {
    if (citations.isEmpty()) return AnnotatedString(rawText)
    val regex = Regex("\\[(J\\d+|W\\d+|\\d+)\\]")
    return buildAnnotatedString {
        var cursor = 0
        for (match in regex.findAll(rawText)) {
            if (match.range.first > cursor) {
                append(rawText.substring(cursor, match.range.first))
            }
            val token = match.value
            val tag = match.groupValues[1]
            val resolved = citations.firstOrNull {
                it.refTag.equals(tag, ignoreCase = true)
            } ?: tag.toIntOrNull()?.let { idx -> citations.getOrNull(idx - 1) }

            if (resolved != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "cit_$tag",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                background = linkColor.copy(alpha = 0.14f)
                            )
                        ),
                        linkInteractionListener = { onCitationClick(resolved) }
                    )
                ) {
                    append(token)
                }
            } else {
                append(token)
            }
            cursor = match.range.last + 1
        }
        if (cursor < rawText.length) {
            append(rawText.substring(cursor))
        }
    }
}

@Composable
private fun CitationDetailBottomSheetContent(
    citation: CitationItem,
    onCopyUrl: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onVerifySource: () -> Unit,
) {
    val isJournal = citation.sourceType == "JOURNAL" || citation.doi.isNotBlank() || citation.year > 0
    val displayTag = citation.refTag.ifBlank { "REF" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("citation_detail_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isJournal) Icons.AutoMirrored.Rounded.MenuBook else Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "[$displayTag] ${if (isJournal) "METADATA JURNAL ILMIAH" else "DETAIL SUMBER WEB"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                color = if (citation.verified) {
                    KafkaVerifiedGreen.copy(alpha = 0.14f)
                } else {
                    KafkaAmber.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (citation.verified) "TERVERIFIKASI" else "SUMBER",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (citation.verified) KafkaVerifiedGreen else KafkaAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = citation.title.ifBlank { "Sumber Referensi" },
            style = MaterialTheme.typography.titleLarge
        )

        if (isJournal) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (citation.authorsText.isNotBlank()) {
                        MetadataKeyValueRow("Penulis", citation.authorsText)
                    }
                    if (citation.venue.isNotBlank() || citation.year > 0) {
                        val pub = listOfNotNull(
                            citation.venue.takeIf { it.isNotBlank() },
                            citation.year.takeIf { it > 0 }?.toString()
                        ).joinToString(" • ")
                        MetadataKeyValueRow("Publikasi", pub)
                    }
                    if (citation.provider.isNotBlank()) {
                        MetadataKeyValueRow("Indeks", citation.provider)
                    }
                    if (citation.citedByCount > 0) {
                        MetadataKeyValueRow("Sitasi", "${citation.citedByCount} kutipan ilmiah")
                    }
                    if (citation.doi.isNotBlank()) {
                        MetadataKeyValueRow("DOI", citation.doi)
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataKeyValueRow("Mesin / Sumber", citation.source.ifBlank { citation.provider })
                    if (citation.venue.isNotBlank()) {
                        MetadataKeyValueRow("Domain", citation.venue)
                    }
                    MetadataKeyValueRow(
                        "Status HTTP",
                        if (citation.verified) "Tautan aktif & dapat diakses" else "Perlu pengecekan manual"
                    )
                }
            }
        }

        if (citation.snippet.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isJournal) "KUTIPAN ABSTRAK" else "CUPLIKAN KONTEKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = citation.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (citation.url.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = citation.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopyUrl,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin URL",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onVerifySource,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Verifikasi")
            }
            if (citation.url.startsWith("http")) {
                Button(
                    onClick = { onOpenUrl(citation.url) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka Sumber")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MetadataKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CitationChipCard(
    index: Int,
    citation: CitationItem,
    onClick: () -> Unit
) {
    val displayTag = citation.refTag.ifBlank { "$index" }
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(216.dp)
            .testTag("citation_chip_$displayTag")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[$displayTag] ${if (citation.verified) "TERVERIFIKASI" else "SUMBER"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (citation.verified) KafkaVerifiedGreen else MaterialTheme.colorScheme.tertiary
                )
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = citation.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = citation.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickExportPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LiveHermesExecutionCard(execution: AgentExecutionState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (execution.isRunning) {
                    AnimePulseAvatar(
                        drawableRes = R.drawable.img_kafka_anime_icon,
                        contentDescription = null,
                        isActive = true,
                        size = 28.dp
                    )
                    Text(
                        text = "KAFKA SEDANG BERPIKIR...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AnimeThinkingIndicator(dotSize = 6.dp)
                } else {
                    Text(
                        text = "EKSEKUSI TERHENTI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (execution.thoughtSteps.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        execution.thoughtSteps.forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            if (execution.streamingText.isNotBlank()) {
                Text(
                    text = execution.streamingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (execution.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = execution.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionListRow(
    session: AgentSession,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${AgentMode.fromRaw(session.mode).label} • Skor ${session.verifiedScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus sesi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
