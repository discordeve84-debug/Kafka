package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.OpenInNew
import androidx.compose.material.icons.automirrored.sharp.Send
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.CalendarMonth
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.CloudUpload
import androidx.compose.material.icons.sharp.Contacts
import androidx.compose.material.icons.sharp.Description
import androidx.compose.material.icons.sharp.FolderOpen
import androidx.compose.material.icons.sharp.LinkOff
import androidx.compose.material.icons.sharp.Mail
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.authorizeGoogleWorkspaceScopes
import com.example.model.GeneratedArtifact
import com.example.model.GmailMessageItem
import com.example.model.GoogleWorkspaceState
import com.example.ui.theme.KafkaAmber
import com.example.ui.theme.KafkaVerifiedGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoogleWorkspaceHubDialog(
    workspaceViewModel: WorkspaceViewModel,
    artifacts: List<GeneratedArtifact>,
    latestResearchSummary: String,
    initialTab: Int = 0,
    prefillSendSubject: String = "",
    prefillSendBody: String = "",
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by workspaceViewModel.state.collectAsStateWithLifecycle()
    val emailSummaries by workspaceViewModel.emailSummaries.collectAsStateWithLifecycle()
    val summarizingEmailId by workspaceViewModel.summarizingEmailId.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var composeTo by remember { mutableStateOf("") }
    var composeSubject by remember(prefillSendSubject) { mutableStateOf(prefillSendSubject) }
    var composeBody by remember(prefillSendBody) { mutableStateOf(prefillSendBody) }
    var showComposeCard by remember(prefillSendSubject, prefillSendBody) {
        mutableStateOf(prefillSendSubject.isNotBlank() || prefillSendBody.isNotBlank())
    }

    var docTitle by remember { mutableStateOf("Laporan Riset Kafka") }
    var docContent by remember(latestResearchSummary) {
        mutableStateOf(latestResearchSummary.ifBlank { "Ringkasan riset dan referensi terverifikasi dari Kafka AI." })
    }

    var eventTitle by remember { mutableStateOf("Review Riset Kafka") }
    var eventNotes by remember { mutableStateOf("Tindak lanjut temuan jurnal & dokumen riset.") }
    var eventHoursAhead by remember { mutableIntStateOf(24) }
    var contactFilter by remember { mutableStateOf("") }

    fun triggerWorkspaceConnect() {
        workspaceViewModel.setConnecting(true)
        authorizeGoogleWorkspaceScopes(
            context = context,
            auth = Firebase.auth,
            onTokenReady = { token -> workspaceViewModel.onAccessTokenGranted(token) },
            onError = { msg -> workspaceViewModel.onConnectionError(msg) },
            scope = scope
        )
    }

    fun openExternalUrl(url: String) {
        if (url.isBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuccubusBatWingCrest(size = 16.dp)
                                Column {
                                    Text(
                                        text = "Google Workspace Hub",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (state.isConnected) {
                                            "Gmail • Drive • Docs • Sheets • Calendar • Contacts"
                                        } else {
                                            "Belum terhubung — Ketuk Hubungkan"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isConnected) KafkaVerifiedGreen else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (state.isConnected) {
                                IconButton(
                                    onClick = { workspaceViewModel.refreshAll() },
                                    enabled = !state.isLoadingData,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Sharp.Refresh, contentDescription = "Sinkronkan ulang")
                                }
                                IconButton(
                                    onClick = { workspaceViewModel.disconnectWorkspace() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Sharp.LinkOff, contentDescription = "Putuskan koneksi")
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_workspace_hub")
                            ) {
                                Icon(Icons.Sharp.Close, contentDescription = "Tutup")
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!state.isConnected) {
                    WorkspaceConnectPrompt(
                        state = state,
                        onConnect = ::triggerWorkspaceConnect
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 780.dp)
                    ) {
                        // Status or error banner
                        if (state.error != null || state.statusMessage != null) {
                            Surface(
                                color = if (state.error != null) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(
                                    1.dp,
                                    if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = state.error ?: state.statusMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.error != null) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { workspaceViewModel.clearMessages() }) {
                                        Text("Tutup")
                                    }
                                }
                            }
                        }

                        // Sub-navigation chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(0, "Gmail (${state.emails.size})", Icons.Sharp.Mail),
                                Triple(1, "Drive & Docs (${state.driveFiles.size})", Icons.Sharp.FolderOpen),
                                Triple(2, "Calendar (${state.calendarEvents.size})", Icons.Sharp.CalendarMonth),
                                Triple(3, "Contacts (${state.contacts.size})", Icons.Sharp.Contacts),
                            ).forEach { (idx, label, icon) ->
                                FilterChip(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("workspace_tab_$idx")
                                )
                            }
                        }

                        if (state.isLoadingData) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Menyinkronkan data Google Workspace...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        when (selectedTab) {
                            0 -> GmailWorkspaceTab(
                                state = state,
                                showComposeCard = showComposeCard,
                                onToggleCompose = { showComposeCard = !showComposeCard },
                                composeTo = composeTo,
                                onToChange = { composeTo = it },
                                composeSubject = composeSubject,
                                onSubjectChange = { composeSubject = it },
                                composeBody = composeBody,
                                onBodyChange = { composeBody = it },
                                latestResearchSummary = latestResearchSummary,
                                emailSummaries = emailSummaries,
                                summarizingEmailId = summarizingEmailId,
                                onSendEmail = {
                                    workspaceViewModel.sendGmail(composeTo, composeSubject, composeBody) {
                                        composeSubject = ""
                                        composeBody = ""
                                        showComposeCard = false
                                    }
                                },
                                onSummarizeEmail = { workspaceViewModel.summarizeEmailWithKafka(it) },
                                onPickContact = { email ->
                                    composeTo = email
                                    showComposeCard = true
                                }
                            )

                            1 -> DriveDocsSheetsTab(
                                state = state,
                                artifacts = artifacts,
                                docTitle = docTitle,
                                onDocTitleChange = { docTitle = it },
                                docContent = docContent,
                                onDocContentChange = { docContent = it },
                                onCreateGoogleDoc = {
                                    workspaceViewModel.createGoogleDoc(docTitle, docContent)
                                },
                                onCreateGoogleSheet = {
                                    workspaceViewModel.createGoogleSheetFromArtifacts(
                                        "Indeks Riset Kafka",
                                        artifacts
                                    )
                                },
                                onUploadArtifact = { workspaceViewModel.uploadArtifactToDrive(it) },
                                onOpenUrl = ::openExternalUrl
                            )

                            2 -> CalendarWorkspaceTab(
                                state = state,
                                eventTitle = eventTitle,
                                onTitleChange = { eventTitle = it },
                                eventNotes = eventNotes,
                                onNotesChange = { eventNotes = it },
                                eventHoursAhead = eventHoursAhead,
                                onHoursChange = { eventHoursAhead = it },
                                onCreateEvent = {
                                    workspaceViewModel.createCalendarReminder(
                                        summary = eventTitle,
                                        description = eventNotes,
                                        hoursFromNow = eventHoursAhead
                                    )
                                },
                                onOpenUrl = ::openExternalUrl
                            )

                            else -> ContactsWorkspaceTab(
                                state = state,
                                filter = contactFilter,
                                onFilterChange = { contactFilter = it },
                                onComposeToContact = { email ->
                                    composeTo = email
                                    showComposeCard = true
                                    selectedTab = 0
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceConnectPrompt(
    state: GoogleWorkspaceState,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .drawSuccubusCornerSigils(
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.tertiary
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuccubusHornCrest(size = 16.dp)
                    Text(
                        text = "INTEGRASI GOOGLE WORKSPACE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                SuccubusGothicDivider()
                Text(
                    text = "Hubungkan akun Google Anda sekali untuk membuka akses penuh Gmail, Drive, Docs, Sheets, Calendar, dan Contacts langsung dari Kafka.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkspaceServiceChip(Icons.Sharp.Mail, "Gmail Inbox & Kirim")
                    WorkspaceServiceChip(Icons.Sharp.FolderOpen, "Google Drive Upload")
                    WorkspaceServiceChip(Icons.Sharp.Description, "Buat Google Docs")
                    WorkspaceServiceChip(Icons.Sharp.TableChart, "Ekspor Google Sheets")
                    WorkspaceServiceChip(Icons.Sharp.CalendarMonth, "Google Calendar")
                    WorkspaceServiceChip(Icons.Sharp.Contacts, "Google Contacts")
                }

                if (state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = onConnect,
                    enabled = !state.isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("connect_google_workspace_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Menghubungkan Google OAuth...")
                    } else {
                        Icon(Icons.Sharp.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hubungkan Gmail & Workspace")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceServiceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun GmailWorkspaceTab(
    state: GoogleWorkspaceState,
    showComposeCard: Boolean,
    onToggleCompose: () -> Unit,
    composeTo: String,
    onToChange: (String) -> Unit,
    composeSubject: String,
    onSubjectChange: (String) -> Unit,
    composeBody: String,
    onBodyChange: (String) -> Unit,
    latestResearchSummary: String,
    emailSummaries: Map<String, String>,
    summarizingEmailId: String?,
    onSendEmail: () -> Unit,
    onSummarizeEmail: (GmailMessageItem) -> Unit,
    onPickContact: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KOTAK MASUK GMAIL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onToggleCompose,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Sharp.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (showComposeCard) "Tutup Form" else "Tulis Email")
                }
            }
        }

        if (showComposeCard) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Kirim Email via Gmail", style = MaterialTheme.typography.titleMedium)

                        if (state.contacts.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.contacts.filter { it.email.isNotBlank() }.take(8).forEach { contact ->
                                    FilterChip(
                                        selected = composeTo.equals(contact.email, ignoreCase = true),
                                        onClick = { onPickContact(contact.email) },
                                        label = { Text(contact.displayName.take(20)) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = composeTo,
                            onValueChange = onToChange,
                            label = { Text("Kepada (Email)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = composeSubject,
                            onValueChange = onSubjectChange,
                            label = { Text("Subjek") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = composeBody,
                            onValueChange = onBodyChange,
                            label = { Text("Isi Pesan") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (latestResearchSummary.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    if (composeSubject.isBlank()) onSubjectChange("Ringkasan Riset Kafka")
                                    onBodyChange(latestResearchSummary)
                                }
                            ) {
                                Text("Isi dari Ringkasan Riset Terakhir")
                            }
                        }
                        Button(
                            onClick = onSendEmail,
                            enabled = !state.isPerformingAction && composeTo.isNotBlank() && composeSubject.isNotBlank() && composeBody.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (state.isPerformingAction) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Kirim Sekarang")
                            }
                        }
                    }
                }
            }
        }

        if (state.emails.isEmpty() && !state.isLoadingData) {
            item {
                Text(
                    text = "Belum ada email ditemukan atau izin Gmail belum aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.emails, key = { it.id }) { email ->
            val aiSummary = emailSummaries[email.id]
            val isSummarizing = summarizingEmailId == email.id
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (email.from.isNotBlank()) {
                        Text(
                            text = email.from,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (email.snippet.isNotBlank()) {
                        Text(
                            text = email.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!aiSummary.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSummarizeEmail(email) },
                            enabled = !isSummarizing,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(38.dp)
                        ) {
                            if (isSummarizing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Sharp.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ringkas AI")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveDocsSheetsTab(
    state: GoogleWorkspaceState,
    artifacts: List<GeneratedArtifact>,
    docTitle: String,
    onDocTitleChange: (String) -> Unit,
    docContent: String,
    onDocContentChange: (String) -> Unit,
    onCreateGoogleDoc: () -> Unit,
    onCreateGoogleSheet: () -> Unit,
    onUploadArtifact: (GeneratedArtifact) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Buat Google Docs & Sheets Baru", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = onDocTitleChange,
                        label = { Text("Judul Google Docs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docContent,
                        onValueChange = onDocContentChange,
                        label = { Text("Isi Dokumen") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCreateGoogleDoc,
                            enabled = !state.isPerformingAction && docTitle.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Sharp.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buat Docs")
                        }
                        OutlinedButton(
                            onClick = onCreateGoogleSheet,
                            enabled = !state.isPerformingAction,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Sharp.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buat Sheets")
                        }
                    }
                }
            }
        }

        if (artifacts.isNotEmpty()) {
            item {
                Text(
                    text = "UNGGAH DOKUMEN KAFKA KE DRIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(artifacts.take(4), key = { "upload_${it.id}_${it.fileName}" }) { art ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(art.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(art.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { onUploadArtifact(art) },
                            enabled = !state.isPerformingAction,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Sharp.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Upload")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "FILE GOOGLE DRIVE TERBARU",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(state.driveFiles, key = { it.id }) { file ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl(file.webViewLink) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = file.mimeType.substringAfterLast('.').ifBlank { file.mimeType },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Sharp.OpenInNew,
                        contentDescription = "Buka di Drive",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWorkspaceTab(
    state: GoogleWorkspaceState,
    eventTitle: String,
    onTitleChange: (String) -> Unit,
    eventNotes: String,
    onNotesChange: (String) -> Unit,
    eventHoursAhead: Int,
    onHoursChange: (Int) -> Unit,
    onCreateEvent: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tambah Pengingat Riset ke Calendar", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = onTitleChange,
                        label = { Text("Judul Agenda") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventNotes,
                        onValueChange = onNotesChange,
                        label = { Text("Catatan") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "+1 Jam", 24 to "+24 Jam", 72 to "+3 Hari").forEach { (hrs, label) ->
                            FilterChip(
                                selected = eventHoursAhead == hrs,
                                onClick = { onHoursChange(hrs) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Button(
                        onClick = onCreateEvent,
                        enabled = !state.isPerformingAction && eventTitle.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Sharp.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan ke Google Calendar")
                    }
                }
            }
        }

        item {
            Text(
                text = "AGENDA MENDATANG",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(state.calendarEvents, key = { it.id }) { ev ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl(ev.htmlLink) }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ev.summary, style = MaterialTheme.typography.titleSmall)
                    if (ev.startTime.isNotBlank()) {
                        Text(ev.startTime, style = MaterialTheme.typography.labelSmall, color = KafkaAmber)
                    }
                    if (ev.description.isNotBlank()) {
                        Text(
                            text = ev.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsWorkspaceTab(
    state: GoogleWorkspaceState,
    filter: String,
    onFilterChange: (String) -> Unit,
    onComposeToContact: (String) -> Unit,
) {
    val filtered = remember(state.contacts, filter) {
        val q = filter.trim().lowercase()
        if (q.isBlank()) state.contacts
        else state.contacts.filter {
            it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = filter,
                onValueChange = onFilterChange,
                label = { Text("Cari Kontak Google") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.resourceName }) { contact ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.displayName, style = MaterialTheme.typography.titleSmall)
                        if (contact.email.isNotBlank()) {
                            Text(
                                text = contact.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (contact.email.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onComposeToContact(contact.email) },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Sharp.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Email")
                        }
                    }
                }
            }
        }
    }
}
