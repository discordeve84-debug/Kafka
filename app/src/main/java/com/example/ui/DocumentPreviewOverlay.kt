package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DocumentBlueprint
import com.example.model.DocumentPreviewDraft
import com.example.model.OutputFormat
import com.example.ui.theme.KafkaVerifiedGreen

@Composable
fun DocumentPreviewOverlay(
    state: DocumentPreviewState,
    onSwitchFormat: (OutputFormat) -> Unit,
    onSaveToDevice: () -> Unit,
    onSaveAndShare: () -> Unit,
    onSaveSnapshot: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (!state.isVisible) return

    BackHandler(enabled = state.isVisible) {
        if (!state.isExporting) onDismiss()
    }

    val draft = state.draft
    val activeFormat = draft?.format ?: state.requestedFormat

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (!state.isExporting) onDismiss() }
            )
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("document_preview_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 780.dp)
                .fillMaxHeight(0.94f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.preview_badge_readonly),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (draft?.verified == true) {
                                Surface(
                                    color = KafkaVerifiedGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = null,
                                            tint = KafkaVerifiedGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "TERVERIFIKASI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KafkaVerifiedGreen
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onSaveSnapshot != null && draft != null) {
                                IconButton(
                                    onClick = onSaveSnapshot,
                                    enabled = !state.isExporting,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("preview_snapshot_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkAdd,
                                        contentDescription = stringResource(R.string.cd_save_snapshot),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                enabled = !state.isExporting,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_preview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_close_preview)
                                )
                            }
                        }
                    }

                    // Format Switcher Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(OutputFormat.DOCX, OutputFormat.PDF, OutputFormat.PPTX).forEach { fmt ->
                            val icon = when (fmt) {
                                OutputFormat.PDF -> Icons.Rounded.PictureAsPdf
                                OutputFormat.PPTX -> Icons.Rounded.Slideshow
                                else -> Icons.Rounded.Description
                            }
                            FilterChip(
                                selected = activeFormat == fmt,
                                enabled = draft != null && !state.isLoading && !state.isExporting,
                                onClick = { onSwitchFormat(fmt) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(fmt.displayLabel) },
                                modifier = Modifier.testTag("preview_format_${fmt.name.lowercase()}")
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Read-Only Document Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    when {
                        state.isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Menyusun pratinjau ${activeFormat.displayLabel}...",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Mengatur struktur judul, ringkasan, bagian, dan referensi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        state.error != null && draft == null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        draft != null -> {
                            when (draft.format) {
                                OutputFormat.PPTX -> PptxSlidesPreviewCanvas(draft.blueprint)
                                OutputFormat.PDF -> PdfPagesPreviewCanvas(draft.blueprint)
                                else -> WordDocumentPreviewCanvas(draft)
                            }
                        }
                    }
                }

                if (state.error != null && draft != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sticky Export Actions Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSaveToDevice,
                        enabled = draft != null && !state.isLoading && !state.isExporting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("preview_save_button")
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Simpan", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Button(
                        onClick = onSaveAndShare,
                        enabled = draft != null && !state.isLoading && !state.isExporting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("preview_export_share_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ekspor & Bagikan", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordDocumentPreviewCanvas(draft: DocumentPreviewDraft) {
    val bp = draft.blueprint
    val paperBg = Color(0xFFFDFEFE)
    val inkDark = Color(0xFF0F172A)
    val inkMuted = Color(0xFF475569)
    val accentTeal = Color(0xFF0F766E)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = paperBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DOKUMEN MICROSOFT WORD (.DOCX)",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentTeal
                    )
                    Text(
                        text = bp.title,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentTeal
                    )
                    if (bp.subtitle.isNotBlank()) {
                        Text(
                            text = bp.subtitle,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = inkMuted
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(
                        text = "Ringkasan Eksekutif",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkDark
                    )
                    Surface(
                        color = Color(0xFFF0FDFA),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF99F6E4))
                    ) {
                        Text(
                            text = bp.executiveSummary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = inkDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    bp.sections.forEachIndexed { idx, sec ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${idx + 1}. ${sec.heading}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkDark
                        )
                        if (sec.body.isNotBlank()) {
                            Text(
                                text = sec.body,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = inkDark
                            )
                        }
                        sec.bullets.forEach { b ->
                            Row(
                                modifier = Modifier.padding(start = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", color = accentTeal, fontWeight = FontWeight.Bold)
                                Text(
                                    text = b,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = inkDark
                                )
                            }
                        }
                    }

                    if (bp.references.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(
                            text = "Referensi & Sumber Terverifikasi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkDark
                        )
                        bp.references.forEachIndexed { idx, ref ->
                            Text(
                                text = "[${idx + 1}] $ref",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = inkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPagesPreviewCanvas(bp: DocumentBlueprint) {
    val paperBg = Color(0xFFFFFFFF)
    val inkDark = Color(0xFF1E293B)
    val inkMuted = Color(0xFF64748B)
    val hermesDark = Color(0xFF0B0E14)
    val hermesTeal = Color(0xFF2DD4BF)
    val headingTeal = Color(0xFF0F766E)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Page 1 Cover + Executive Summary
        item {
            Surface(
                color = paperBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(hermesDark)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bp.title,
                            fontSize = 19.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = bp.subtitle.ifBlank { "Dossier Terverifikasi oleh Kafka AI Agent" },
                            fontSize = 12.sp,
                            color = Color(0xFF99F6E4)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(hermesTeal)
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ringkasan Eksekutif",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = headingTeal
                        )
                        Text(
                            text = bp.executiveSummary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = inkDark
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Kafka Research & Document Agent — Hal 1",
                            fontSize = 10.sp,
                            color = inkMuted
                        )
                    }
                }
            }
        }

        // Subsequent PDF Folio Pages (grouped per 2 sections)
        val chunks = bp.sections.chunked(2)
        itemsIndexed(chunks) { chunkIdx, sectionPair ->
            Surface(
                color = paperBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(hermesTeal)
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sectionPair.forEach { sec ->
                            Text(
                                text = sec.heading,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = headingTeal
                            )
                            if (sec.body.isNotBlank()) {
                                Text(
                                    text = sec.body,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = inkDark
                                )
                            }
                            sec.bullets.forEach { b ->
                                Text(
                                    text = "  •  $b",
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = inkDark
                                )
                            }
                        }

                        if (chunkIdx == chunks.lastIndex && bp.references.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Text(
                                text = "Referensi & Sumber Terverifikasi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = headingTeal
                            )
                            bp.references.forEachIndexed { rIdx, ref ->
                                Text(
                                    text = "[${rIdx + 1}] $ref",
                                    fontSize = 11.5.sp,
                                    color = inkMuted
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Kafka Research & Document Agent — Hal ${chunkIdx + 2}",
                            fontSize = 10.sp,
                            color = inkMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PptxSlidesPreviewCanvas(bp: DocumentBlueprint) {
    val slides = remember(bp) {
        buildList {
            add(
                bp.title to listOf(
                    bp.subtitle.ifBlank { "Presentasi Riset & Sintesis Kafka AI" },
                    bp.executiveSummary.take(260)
                )
            )
            bp.sections.forEach { sec ->
                val lines = buildList {
                    if (sec.body.isNotBlank()) add(sec.body)
                    addAll(sec.bullets)
                }.take(6)
                add(sec.heading to lines)
            }
            if (bp.references.isNotEmpty()) {
                add("Referensi & Verifikasi Sumber" to bp.references.take(6))
            }
        }
    }

    val slideBg = Color(0xFF0B0E14)
    val slideAccent = Color(0xFF2DD4BF)
    val slideBodyText = Color(0xFFE2E8F0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(slides) { idx, (heading, lines) ->
            val isCover = idx == 0
            Surface(
                color = slideBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, slideAccent.copy(alpha = 0.45f)),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(slideAccent)
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SLIDE ${idx + 1} / ${slides.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = slideAccent.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (isCover) "COVER DECK" else "CONTENT SLIDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Text(
                            text = heading,
                            fontSize = if (isCover) 20.sp else 17.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = slideAccent
                        )

                        lines.forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isCover) {
                                    Text("•", color = slideAccent, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = line,
                                    fontSize = if (isCover) 13.5.sp else 13.sp,
                                    lineHeight = 18.sp,
                                    color = slideBodyText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
