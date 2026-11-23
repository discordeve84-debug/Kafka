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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.GeneratedArtifact
import com.example.model.OutputFormat
import com.example.ui.theme.KafkaVerifiedGreen

@Composable
fun DocStudioScreen(
    viewModel: KafkaViewModel,
    onOpenSession: (String) -> Unit = {},
) {
    val docState by viewModel.docStudioState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()

    var topicInput by remember { mutableStateOf("") }
    var verifyBeforeExport by remember { mutableStateOf(true) }

    LaunchedEffect(profileState) {
        val profile = (profileState as? UiState.Success)?.data
        if (profile != null) {
            verifyBeforeExport = profile.autoVerify
        }
    }

    val artifacts = (artifactsState as? UiState.Success)?.data ?: emptyList()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Document & Deck Generator",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Text(
                            text = "Pilih format output (Word .docx, PDF, PowerPoint .pptx, Markdown, CSV). Kafka akan meriset sumber terkini secara opsional dan menyusun file asli di perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutputFormat.exportableFormats.forEach { format ->
                                FilterChip(
                                    selected = docState.activeFormat == format,
                                    onClick = { viewModel.setDocFormat(format) },
                                    label = { Text(format.displayLabel) },
                                    modifier = Modifier.testTag("format_chip_${format.name.lowercase()}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Topik atau Materi Dokumen") },
                            placeholder = { Text("Contoh: Laporan Strategi Adopsi AI Enterprise 2026 lengkap dengan studi kasus...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doc_topic_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = verifyBeforeExport,
                                onCheckedChange = { verifyBeforeExport = it }
                            )
                            Text(
                                text = "Riset Web & Jurnal + Verifikasi sebelum ekspor",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (docState.error != null) {
                            Text(
                                text = docState.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.generateDocument(
                                    topicOrContent = topicInput,
                                    format = docState.activeFormat,
                                    verifyBeforeExport = verifyBeforeExport
                                ) {
                                    topicInput = ""
                                }
                            },
                            enabled = topicInput.isNotBlank() && !docState.isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_doc_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (docState.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Menyusun & Mengompilasi ${docState.activeFormat.extension.uppercase()}...")
                            } else {
                                Text("Buat ${docState.activeFormat.displayLabel}")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ARSIP DOKUMEN (${artifacts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (artifacts.isEmpty()) {
                item {
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
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Belum ada dokumen dihasilkan",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Buat Word, PDF, atau PowerPoint dari form di atas atau langsung dari jawaban Agent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(artifacts, key = { it.id.ifBlank { it.fileName } }) { art ->
                ArtifactCard(
                    artifact = art,
                    onOpen = { viewModel.openArtifact(art) },
                    onShare = { viewModel.shareArtifact(art) },
                    onOpenSession = if (art.sessionId.isNotBlank()) {
                        { onOpenSession(art.sessionId) }
                    } else null,
                    onDelete = { viewModel.deleteArtifact(art.id) }
                )
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    artifact: GeneratedArtifact,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onOpenSession: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val format = OutputFormat.fromRaw(artifact.format)
    val icon = when (format) {
        OutputFormat.PDF -> Icons.Rounded.PictureAsPdf
        OutputFormat.PPTX -> Icons.Rounded.Slideshow
        OutputFormat.CSV -> Icons.Rounded.TableChart
        else -> Icons.Rounded.Description
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = artifact.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${format.displayLabel} • ${artifact.wordCount} kata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (artifact.verified) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Terverifikasi",
                        tint = KafkaVerifiedGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (artifact.summary.isNotBlank()) {
                Text(
                    text = artifact.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buka")
                }
                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share_file),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Bagikan")
                }
                if (onOpenSession != null) {
                    IconButton(onClick = onOpenSession, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = "Buka sesi riset asal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus dokumen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
