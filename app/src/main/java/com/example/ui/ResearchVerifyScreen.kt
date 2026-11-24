package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AgentMode
import com.example.model.ClaimCheck
import com.example.model.JournalPaper
import com.example.model.JournalSummaryVerification
import com.example.model.OutputFormat
import com.example.model.VerificationStatus
import com.example.model.WebSearchResult
import com.example.ui.theme.KafkaAmber
import com.example.ui.theme.KafkaErrorRed
import com.example.ui.theme.KafkaVerifiedGreen

@Composable
fun ResearchVerifyScreen(
    viewModel: KafkaViewModel,
    initialTab: Int = 0,
    onAskAgent: (String, AgentMode) -> Unit,
    onNavigateToDocStudio: () -> Unit,
) {
    val state by viewModel.researchState.collectAsStateWithLifecycle()
    var subTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Live Web", "Jurnal Ilmiah", "Verifikasi Isi")
                tabs.forEachIndexed { idx, title ->
                    SegmentedButton(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("research_tab_$idx")
                    ) {
                        Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }

            when (subTab) {
                0 -> {
                    // LIVE WEB SEARCH
                    SearchBarRow(
                        value = state.webQuery,
                        onValueChange = viewModel::updateWebQuery,
                        placeholder = "Cari berita, fakta, atau topik di web langsung...",
                        isLoading = state.isSearchingWeb,
                        testTag = "web_search_input",
                        buttonTag = "web_search_button",
                        onSearch = { viewModel.runWebSearch() }
                    )

                    if (state.webError != null) {
                        ErrorBanner(state.webError.orEmpty())
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.webResults.isEmpty() && !state.isSearchingWeb) {
                            item {
                                EmptyWorkbenchCard(
                                    icon = Icons.Rounded.Public,
                                    title = "Live Web Search Real-Time",
                                    subtitle = "Menelusuri DuckDuckGo, Wikipedia (EN/ID), dan Algolia Tech Index secara langsung tanpa data tiruan."
                                )
                            }
                        }
                        items(state.webResults) { item ->
                            WebResultCard(
                                item = item,
                                onOpen = { runCatching { uriHandler.openUri(item.url) } },
                                onVerify = {
                                    subTab = 2
                                    viewModel.runContentVerification("${item.title}. ${item.snippet} (${item.url})")
                                },
                                onAskKafka = {
                                    onAskAgent(
                                        "Analisis dan verifikasi sumber web ini: ${item.title} (${item.url}) — ${item.snippet}",
                                        AgentMode.WEB_SEARCH
                                    )
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // ACADEMIC JOURNAL & GITHUB OPEN-PAPER SEARCH
                    SearchBarRow(
                        value = state.journalQuery,
                        onValueChange = viewModel::updateJournalQuery,
                        placeholder = "Cari jurnal, DOI, arXiv PDF, atau koleksi GitHub...",
                        isLoading = state.isSearchingJournals,
                        testTag = "journal_search_input",
                        buttonTag = "journal_search_button",
                        onSearch = { viewModel.runJournalSearch() }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.AssistChip(
                            onClick = { viewModel.runGitHubJournalSearch(state.journalQuery.ifBlank { "papers-we-love awesome-papers" }) },
                            label = { Text("Repo GitHub Banyak Jurnal") },
                            modifier = Modifier.testTag("github_papers_chip")
                        )
                        androidx.compose.material3.AssistChip(
                            onClick = { viewModel.runJournalSearch(state.journalQuery.ifBlank { "artificial intelligence agent" }) },
                            label = { Text("OpenAlex + DOAJ + arXiv") }
                        )
                        androidx.compose.material3.AssistChip(
                            onClick = { viewModel.runGitHubJournalSearch("awesome-llm-papers arxiv") },
                            label = { Text("GitHub LLM & AI Papers") }
                        )
                    }

                    if (state.journalError != null) {
                        ErrorBanner(state.journalError.orEmpty())
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.journalResults.isEmpty() && !state.isSearchingJournals) {
                            item {
                                EmptyWorkbenchCard(
                                    icon = Icons.Rounded.School,
                                    title = "Jurnal Terbuka & Koleksi GitHub",
                                    subtitle = "Menarik paper open-access langsung dari OpenAlex, Europe PMC, DOAJ, arXiv, Crossref DOI, dan repositori GitHub berisi paper yang bisa langsung dibuka."
                                )
                            }
                        }
                        items(state.journalResults) { paper ->
                            val paperKey = paper.url.ifBlank { paper.title }
                            JournalPaperCard(
                                paper = paper,
                                aiSummary = state.journalSummaries[paperKey],
                                isSummarizing = state.summarizingPaperUrl == paperKey,
                                onSummarizeAndVerify = { viewModel.summarizeAndVerifyJournal(paper) },
                                onOpen = {
                                    val target = paper.url.ifBlank { paper.pdfUrl.orEmpty() }
                                    if (target.startsWith("http")) runCatching { uriHandler.openUri(target) }
                                },
                                onOpenPdf = paper.pdfUrl?.takeIf { it.startsWith("http") }?.let { pdfLink ->
                                    { runCatching { uriHandler.openUri(pdfLink) } }
                                },
                                onAskKafka = {
                                    onAskAgent(
                                        "Bedah metodologi, temuan utama, dan keabsahan jurnal ini: ${paper.title} (${paper.venue}, ${paper.year}). Abstrak: ${paper.abstractText}",
                                        AgentMode.JOURNAL_RESEARCH
                                    )
                                },
                                onExportDocx = {
                                    viewModel.generateDocument(
                                        topicOrContent = "Review Jurnal Ilmiah: ${paper.title}\nPenulis: ${paper.authors.joinToString()}\nPenerbit: ${paper.venue} (${paper.year})\nDOI: ${paper.doi ?: "-"}\nURL: ${paper.url}\n\nAbstrak:\n${paper.abstractText}",
                                        format = OutputFormat.DOCX,
                                        verifyBeforeExport = false
                                    ) {
                                        onNavigateToDocStudio()
                                    }
                                }
                            )
                        }
                    }
                }

                2 -> {
                    // CONTENT VERIFICATION WORKBENCH
                    OutlinedTextField(
                        value = state.verifyInput,
                        onValueChange = viewModel::updateVerifyInput,
                        label = { Text("Teks, klaim, atau artikel yang ingin diverifikasi") },
                        placeholder = { Text("Tempel paragraf berita, klaim riset, atau draf laporan di sini...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_content_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = { viewModel.runContentVerification() },
                        enabled = state.verifyInput.isNotBlank() && !state.isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_run_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Mengaudit terhadap Web & Jurnal...")
                        } else {
                            Icon(Icons.Rounded.FactCheck, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Verifikasi Isi & Klaim Sekarang")
                        }
                    }

                    if (state.verifyError != null) {
                        ErrorBanner(state.verifyError.orEmpty())
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val report = state.verificationReport
                        if (report == null && !state.isVerifying) {
                            item {
                                EmptyWorkbenchCard(
                                    icon = Icons.Rounded.VerifiedUser,
                                    title = "Audit Verifikasi Fakta Multi-Sumber",
                                    subtitle = "Kafka mengekstrak klaim dari teks Anda, memeriksa tautan web dan jurnal ilmiah secara langsung, lalu memberi skor integritas."
                                )
                            }
                        } else if (report != null) {
                            item {
                                VerificationScoreHeader(report.overallScore, report.status, report.summary, report.sourcesChecked, report.verifiedLinks)
                            }
                            items(report.claims) { claim ->
                                ClaimAuditCard(
                                    claim = claim,
                                    onOpenUrl = { url ->
                                        if (url.startsWith("http")) runCatching { uriHandler.openUri(url) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isLoading: Boolean,
    testTag: String,
    buttonTag: String,
    onSearch: () -> Unit
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
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Button(
            onClick = onSearch,
            enabled = value.isNotBlank() && !isLoading,
            modifier = Modifier
                .height(54.dp)
                .testTag(buttonTag),
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
private fun WebResultCard(
    item: WebSearchResult,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
    onAskKafka: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${item.sourceEngine} • ${item.domain}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka Web")
                }
                OutlinedButton(onClick = onVerify, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verifikasi")
                }
                Button(onClick = onAskKafka, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tanya Kafka")
                }
            }
        }
    }
}

@Composable
private fun JournalPaperCard(
    paper: JournalPaper,
    aiSummary: JournalSummaryVerification?,
    isSummarizing: Boolean,
    onSummarizeAndVerify: () -> Unit,
    onOpen: () -> Unit,
    onOpenPdf: (() -> Unit)?,
    onAskKafka: () -> Unit,
    onExportDocx: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${paper.sourceProvider} • ${if (paper.year > 0) paper.year else "Live"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (paper.isOpenAccess || onOpenPdf != null) {
                        Surface(
                            color = KafkaVerifiedGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OPEN ACCESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = KafkaVerifiedGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (paper.citedByCount > 0) {
                    Text(
                        text = "${paper.citedByCount} sitasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(text = paper.title, style = MaterialTheme.typography.titleMedium)
            if (paper.authors.isNotEmpty()) {
                Text(
                    text = "${paper.authors.joinToString(", ")} — ${paper.venue}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!paper.doi.isNullOrBlank()) {
                Text(
                    text = "DOI: ${paper.doi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = paper.abstractText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // AI Summarization & Cross-Source Verification Panel
            if (aiSummary != null) {
                val badgeColor = when (aiSummary.accuracyStatus) {
                    VerificationStatus.VERIFIED -> KafkaVerifiedGreen
                    VerificationStatus.PARTIAL -> KafkaAmber
                    else -> KafkaErrorRed
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RINGKASAN AI & VERIFIKASI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                            Text(
                                text = "${aiSummary.accuracyScore}% ${aiSummary.accuracyStatus.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor
                            )
                        }
                        Text(
                            text = "Temuan Utama",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = aiSummary.keyFindings,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Metodologi",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = aiSummary.methodology,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Verifikasi Akurasi (${aiSummary.crossCheckedSources} sumber pembanding)",
                            style = MaterialTheme.typography.labelLarge,
                            color = badgeColor
                        )
                        Text(
                            text = "${aiSummary.accuracyNotes} Batasan: ${aiSummary.limitations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSummarizeAndVerify,
                    enabled = !isSummarizing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("summarize_journal_button")
                ) {
                    if (isSummarizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Meringkas...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (aiSummary == null) "Ringkas & Verifikasi" else "Audit Ulang")
                    }
                }
                if (onOpenPdf != null) {
                    OutlinedButton(onClick = onOpenPdf, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Buka PDF")
                    }
                }
                OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Buka Jurnal")
                }
                OutlinedButton(onClick = onExportDocx, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("DOCX")
                }
                OutlinedButton(onClick = onAskKafka, shape = RoundedCornerShape(10.dp)) {
                    Text("Tanya Agent")
                }
            }
        }
    }
}

@Composable
private fun VerificationScoreHeader(
    score: Int,
    status: VerificationStatus,
    summary: String,
    sourcesChecked: Int,
    verifiedLinks: Int
) {
    val accent = when (status) {
        VerificationStatus.VERIFIED -> KafkaVerifiedGreen
        VerificationStatus.PARTIAL -> KafkaAmber
        else -> KafkaErrorRed
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.label.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = "$score / 100",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent
                )
            }
            LinearProgressIndicator(
                progress = { score / 100f },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$sourcesChecked sumber web/jurnal diperiksa • $verifiedLinks tautan aktif terverifikasi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClaimAuditCard(claim: ClaimCheck, onOpenUrl: (String) -> Unit) {
    val color = when (claim.verdict) {
        "SUPPORTED" -> KafkaVerifiedGreen
        "PARTIAL" -> KafkaAmber
        else -> KafkaErrorRed
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (claim.verdict == "SUPPORTED") Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(text = claim.verdict, style = MaterialTheme.typography.labelMedium, color = color)
            }
            Text(text = claim.claim, style = MaterialTheme.typography.titleSmall)
            Text(
                text = claim.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (claim.evidenceSource.isNotBlank()) {
                OutlinedButton(
                    onClick = { onOpenUrl(claim.evidenceUrl) },
                    enabled = claim.evidenceUrl.startsWith("http"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(claim.evidenceSource, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkbenchCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}
