package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.random.Random

@Composable
fun ImageStudioDialog(
    state: ImageStudioUiState,
    onGenerate: (String) -> Unit,
    onEdit: (Bitmap, String) -> Unit,
    onShareBitmap: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedBitmap = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 680.dp)
                .testTag("image_studio_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
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
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "AI Image Create & Edit",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Model: gemini-nano-banana-2.1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup Studio Gambar")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (pickedBitmap == null) "Pilih Foto (Edit)" else "Ganti Foto")
                    }
                    if (pickedBitmap != null) {
                        TextButton(onClick = { pickedBitmap = null }) {
                            Text("Hapus Foto")
                        }
                    }
                }

                pickedBitmap?.let { srcBmp ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Foto Sumber untuk Diedit:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Image(
                                bitmap = srcBmp.asImageBitmap(),
                                contentDescription = "Gambar sumber yang dipilih",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = {
                        Text(if (pickedBitmap != null) "Instruksi Edit Gambar" else "Prompt Pembuatan Gambar")
                    },
                    placeholder = {
                        Text(
                            if (pickedBitmap != null) "Contoh: Ubah latar menjadi malam neon Tokyo bergaya anime"
                            else "Contoh: Ilustrasi anime asisten riset futuristik di perpustakaan hologram"
                        )
                    },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_studio_prompt")
                )

                Button(
                    onClick = {
                        val src = pickedBitmap
                        if (src != null) {
                            onEdit(src, prompt)
                        } else {
                            onGenerate(prompt)
                        }
                    },
                    enabled = prompt.isNotBlank() && state !is ImageStudioUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("image_studio_generate_btn")
                ) {
                    if (state is ImageStudioUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Memproses (30-60 detik)...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pickedBitmap != null) "Edit Foto dengan Nano Banana 2.1" else "Buat Gambar AI")
                    }
                }

                when (state) {
                    is ImageStudioUiState.Idle -> {
                        Text(
                            text = "Catatan: Model gambar Nano Banana 2.1 memerlukan Firebase Blaze plan aktif pada proyek Firebase Anda.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ImageStudioUiState.Loading -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Mensintesis gambar via Firebase AI Logic...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    is ImageStudioUiState.Success -> {
                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "Hasil gambar AI Kafka",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickedBitmap = state.bitmap },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Jadikan Sumber Edit")
                            }
                            Button(
                                onClick = { onShareBitmap(state.bitmap) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan / Bagikan")
                            }
                        }
                    }
                    is ImageStudioUiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KafkaCyberGameDialog(
    onDismiss: () -> Unit
) {
    var board by remember { mutableStateOf(spawnInitialBoard()) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }

    fun move(direction: GridDirection) {
        val (nextBoard, gained, moved) = step2048Board(board, direction)
        if (moved) {
            board = addRandomTile(nextBoard)
            score += gained
            bestScore = max(bestScore, score)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .testTag("cyber_game_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text("Kafka Cyber Matrix 2048", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Mini-Game Skill Interaktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup game")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = {}, label = { Text("Skor: $score") })
                    AssistChip(onClick = {}, label = { Text("Terbaik: $bestScore") })
                    OutlinedButton(
                        onClick = {
                            board = spawnInitialBoard()
                            score = 0
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ulang")
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (r in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (c in 0 until 4) {
                                    val value = board[r * 4 + c]
                                    val bgColor = when (value) {
                                        0 -> MaterialTheme.colorScheme.surface
                                        2 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        4 -> MaterialTheme.colorScheme.primaryContainer
                                        8 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        16 -> MaterialTheme.colorScheme.secondaryContainer
                                        32 -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val textColor = when {
                                        value == 0 -> Color.Transparent
                                        value >= 64 -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (value > 0) {
                                            Text(
                                                text = "$value",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // D-Pad controls with 48dp+ touch targets
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { move(GridDirection.UP) },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Geser atas")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = { move(GridDirection.LEFT) },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geser kiri")
                            }
                        }
                        Surface(
                            onClick = { move(GridDirection.DOWN) },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Geser bawah")
                            }
                        }
                        Surface(
                            onClick = { move(GridDirection.RIGHT) },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Geser kanan")
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class GridDirection { UP, DOWN, LEFT, RIGHT }

private fun spawnInitialBoard(): List<Int> {
    var board = List(16) { 0 }
    board = addRandomTile(board)
    return addRandomTile(board)
}

private fun addRandomTile(board: List<Int>): List<Int> {
    val empties = board.indices.filter { board[it] == 0 }
    if (empties.isEmpty()) return board
    val pos = empties[Random.nextInt(empties.size)]
    val next = board.toMutableList()
    next[pos] = if (Random.nextFloat() < 0.85f) 2 else 4
    return next
}

private fun slideAndMergeLine(line: List<Int>): Pair<List<Int>, Int> {
    val nonZero = line.filter { it != 0 }.toMutableList()
    val result = mutableListOf<Int>()
    var gained = 0
    var i = 0
    while (i < nonZero.size) {
        if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
            val merged = nonZero[i] * 2
            result.add(merged)
            gained += merged
            i += 2
        } else {
            result.add(nonZero[i])
            i += 1
        }
    }
    while (result.size < 4) result.add(0)
    return result to gained
}

private fun step2048Board(board: List<Int>, dir: GridDirection): Triple<List<Int>, Int, Boolean> {
    val next = MutableList(16) { 0 }
    var totalGain = 0
    for (idx in 0 until 4) {
        val line = when (dir) {
            GridDirection.LEFT -> (0 until 4).map { c -> board[idx * 4 + c] }
            GridDirection.RIGHT -> (3 downTo 0).map { c -> board[idx * 4 + c] }
            GridDirection.UP -> (0 until 4).map { r -> board[r * 4 + idx] }
            GridDirection.DOWN -> (3 downTo 0).map { r -> board[r * 4 + idx] }
        }
        val (merged, gain) = slideAndMergeLine(line)
        totalGain += gain
        for (k in 0 until 4) {
            val pos = when (dir) {
                GridDirection.LEFT -> idx * 4 + k
                GridDirection.RIGHT -> idx * 4 + (3 - k)
                GridDirection.UP -> k * 4 + idx
                GridDirection.DOWN -> (3 - k) * 4 + idx
            }
            next[pos] = merged[k]
        }
    }
    return Triple(next, totalGain, next != board)
}
