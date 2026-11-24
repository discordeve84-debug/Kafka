package com.example.research.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.research.JournalPaper
import com.example.research.JournalSummaryVerification
import com.example.research.LiveResearchModule
import com.example.research.VerificationReport
import com.example.research.VerificationStatus
import com.example.research.WebSearchResult
import kotlinx.coroutines.launch

private val VerifiedAccent = Color(0xFF22C55E)
private val PartialAccent = Color(0xFFF59E0B)
private val UnverifiedAccent = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchModuleDialog(
    initialTab: Int = 0,
    researchModule: LiveResearchModule = remember { LiveResearchModule() },
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var subTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var webQuery by rememberSaveable { mutableStateOf("") }
    var isSearchingWeb by remember { mutableStateOf(false) }
    var webResults by remember { mutableStateOf<List<WebSearchResult>>(emptyList()) }
    var webError by remember { mutableStateOf<String?>(null) }

    var journalQuery by rememberSaveable { mutableStateOf("") }
    var isSearchingJournals by remember { mutableStateOf(false) }
    var journalResults by remember { mutableStateOf<List<JournalPaper>>(emptyList()) }
    var journalError by remember { mutableStateOf<String?>(null) }
    var verifyingPaperKey by remember { mutableStateOf<String?>(null) }
    val journalVerifications = remember { mutableStateMapOf<String, JournalSummaryVerification>() }

    var verifyInput by rememberSaveable { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationReport by remember { mutableStateOf<VerificationReport?>(null) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    fun executeWebSearch(q: String = webQuery) {
        val clean = q.trim()
        if (clean.isBlank() || isSearchingWeb) return
        webQuery = clean
        isSearchingWeb = true
        webError = null
        scope.launch {
            runCatching { researchModule.searchLiveWeb(clean, limit = 12) }
                .onSuccess { list ->
                    webResults = list
                    webError = if (list.isEmpty()) "Tidak ada hasil web ditemukan." else null
                }
                .onFailure { err ->
                    webError = err.localizedMessage ?: "Gagal melakukan live web search."
                }
            isSearchingWeb = false
        }
    }

    fun executeJournalSearch(q: String = journalQuery, githubOnly: Boolean = false) {
        val clean = q.trim().ifBlank { if (githubOnly) "papers-we-love machine learning" else "" }
        if (clean.isBlank() || isSearchingJournals) return
        journalQuery = clean
        isSearchingJournals = true
        journalError = null
        scope.launch {
            runCatching {
                if (githubOnly) {
                    researchModule.searchGitHubJournalCollections(clean, limit = 14)
                } else {
                    researchModule.searchAcademicJournals(clean, limit = 16)
                }
            }.onSuccess { list ->
                journalResults = list
                journalError = if (list.isEmpty()) "Tidak ada jurnal ditemukan." else null
            }.onFailure { err ->
                journalError = err.localizedMessage ?: "Gagal mencari jurnal."
            }
            isSearchingJournals = false
        }
    }

    fun executeVerification(text: String = verifyInput) {
        val clean = text.trim()
        if (clean.isBlank() || isVerifying) return
        verifyInput = clean
        isVerifying = true
        verifyError = null
        scope.launch {
            runCatching { researchModule.verifyContentMultiSource(clean) }
                .onSuccess { report -> verificationReport = report }
                .onFailure { err -> verifyError = err.localizedMessage ?: "Verifikasi gagal." }
            isVerifying = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Modul Live Search, Jurnal & Verifikasi", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "OpenAlex • Europe PMC • DOAJ • arXiv • GitHub • Live Web",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf("Live Web", "Jurnal", "Verifikasi")
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = subTab == idx,
                            onClick = { subTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("standalone_research_tab_$idx")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                when (subTab) {
                    0 -> {
                        ModuleSearchRow(
                            value = webQuery,
                            onValueChange = { webQuery = it },
                            placeholder = "Cari berita, topik, atau repo di web live...",
                            isLoading = isSearchingWeb,
                            onSearch = { executeWebSearch() }
                        )
                        if (webError != null) {
                            Text(webError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (webResults.isEmpty() && !isSearchingWeb) {
                                item {
                                    ModuleEmptyCard(
                                        icon = Icons.Rounded.Public,
                                        title = "Live Web Search Real-Time",
                                        subtitle = "Menelusuri DuckDuckGo Live, Wikipedia EN/ID, Algolia Tech Index, dan GitHub tanpa data palsu."
                                    )
                                }
                            }
                            items(webResults) { item ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "${item.sourceEngine} • ${item.domain}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = item.snippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { runCatching { uriHandler.openUri(item.url) } },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Buka Web")
                                            }
                                            Button(
                                                onClick = {
                                                    subTab = 2
                                                    executeVerification("${item.title}. ${item.snippet} (${item.url})")
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Verifikasi")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        ModuleSearchRow(
                            value = journalQuery,
                            onValueChange = { journalQuery = it },
                            placeholder = "Cari jurnal DOI, OpenAlex, arXiv PDF, atau GitHub...",
                            isLoading = isSearchingJournals,
                            onSearch = { executeJournalSearch() }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { executeJournalSearch(githubOnly = true) },
                                label = { Text("Repo GitHub Banyak Jurnal") }
                            )
                            AssistChip(
                                onClick = { executeJournalSearch(journalQuery.ifBlank { "artificial intelligence" }) },
                                label = { Text("OpenAlex + DOAJ + arXiv") }
                            )
                            AssistChip(
                                onClick = { executeJournalSearch("awesome-llm-papers arxiv", githubOnly = true) },
                                label = { Text("GitHub LLM Papers") }
                            )
                        }
                        if (journalError != null) {
                            Text(journalError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (journalResults.isEmpty() && !isSearchingJournals) {
                                item {
                                    ModuleEmptyCard(
                                        icon = Icons.Rounded.School,
                                        title = "Pencarian Jurnal Terbuka & GitHub",
                                        subtitle = "Cari jurnal ilmiah dari OpenAlex, Europe PMC, DOAJ, arXiv, Crossref, dan koleksi paper GitHub."
                                    )
                                }
                            }
                            items(journalResults) { paper ->
                                val key = paper.url.ifBlank { paper.title }
                                val summary = journalVerifications[key]
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${paper.sourceProvider} • ${if (paper.year > 0) paper.year else "Live"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (paper.isOpenAccess || !paper.pdfUrl.isNullOrBlank()) {
                                                Text(
                                                    text = "OPEN ACCESS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VerifiedAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(paper.title, style = MaterialTheme.typography.titleMedium)
                                        if (paper.authors.isNotEmpty()) {
                                            Text(
                                                text = "${paper.authors.joinToString()} — ${paper.venue}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = paper.abstractText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (summary != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "${summary.accuracyScore}% ${summary.accuracyStatus.label}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (summary.accuracyStatus == VerificationStatus.VERIFIED) VerifiedAccent else PartialAccent
                                                    )
                                                    Text(summary.accuracyNotes, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (verifyingPaperKey == null) {
                                                        verifyingPaperKey = key
                                                        scope.launch {
                                                            runCatching { researchModule.verifyJournalQuick(paper) }
                                                                .onSuccess { journalVerifications[key] = it }
                                                            verifyingPaperKey = null
                                                        }
                                                    }
                                                },
                                                enabled = verifyingPaperKey != key,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(if (verifyingPaperKey == key) "Memverifikasi..." else "Verifikasi Jurnal")
                                            }
                                            val pdf = paper.pdfUrl?.takeIf { it.startsWith("http") }
                                            if (pdf != null) {
                                                OutlinedButton(
                                                    onClick = { runCatching { uriHandler.openUri(pdf) } },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(5.dp))
                                                    Text("Buka PDF")
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val target = paper.url.ifBlank { pdf.orEmpty() }
                                                    if (target.startsWith("http")) runCatching { uriHandler.openUri(target) }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Buka Jurnal")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        OutlinedTextField(
                            value = verifyInput,
                            onValueChange = { verifyInput = it },
                            label = { Text("Teks, klaim, atau URL untuk diverifikasi") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = { executeVerification() },
                            enabled = verifyInput.isNotBlank() && !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Memverifikasi Lintas Web & Jurnal...")
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Jalankan Verifikasi Multi-Sumber")
                            }
                        }
                        if (verifyError != null) {
                            Text(verifyError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val report = verificationReport
                            if (report == null && !isVerifying) {
                                item {
                                    ModuleEmptyCard(
                                        icon = Icons.Rounded.VerifiedUser,
                                        title = "Audit Verifikasi Tautan & Klaim",
                                        subtitle = "Memeriksa keterjangkauan URL HTTP serta mencocokkan klaim dengan sumber web & jurnal ilmiah."
                                    )
                                }
                            } else if (report != null) {
                                item {
                                    val accent = when (report.status) {
                                        VerificationStatus.VERIFIED -> VerifiedAccent
                                        VerificationStatus.PARTIAL -> PartialAccent
                                        else -> UnverifiedAccent
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(report.status.label.uppercase(), color = accent, fontWeight = FontWeight.Bold)
                                                Text("${report.overallScore} / 100", color = accent, style = MaterialTheme.typography.titleLarge)
                                            }
                                            LinearProgressIndicator(
                                                progress = { report.overallScore / 100f },
                                                color = accent,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                            )
                                            Text(report.summary, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "${report.sourcesChecked} sumber diperiksa • ${report.verifiedLinks} tautan aktif",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                items(report.claims) { claim ->
                                    val color = when (claim.verdict) {
                                        "SUPPORTED" -> VerifiedAccent
                                        "PARTIAL" -> PartialAccent
                                        else -> UnverifiedAccent
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = if (claim.verdict == "SUPPORTED") Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(claim.verdict, color = color, style = MaterialTheme.typography.labelMedium)
                                            }
                                            Text(claim.claim, style = MaterialTheme.typography.titleSmall)
                                            Text(claim.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (claim.evidenceUrl.startsWith("http")) {
                                                OutlinedButton(
                                                    onClick = { runCatching { uriHandler.openUri(claim.evidenceUrl) } },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(claim.evidenceSource, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleSearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isLoading: Boolean,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Button(
            onClick = onSearch,
            enabled = value.isNotBlank() && !isLoading,
            modifier = Modifier.height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Rounded.Search, contentDescription = "Cari")
            }
        }
    }
}

@Composable
private fun ModuleEmptyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
