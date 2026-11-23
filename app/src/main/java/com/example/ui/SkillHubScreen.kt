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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.AgentSkill
import com.example.model.GitHubSkillRepo
import com.example.model.SkillSourceType

@Composable
fun SkillHubScreen(
    viewModel: KafkaViewModel,
    onTestSkillInAgent: (AgentSkill) -> Unit,
    onOpenImageStudio: () -> Unit = {},
    onOpenCyberGame: () -> Unit = {},
) {
    val skillsState by viewModel.skillsState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val hubState by viewModel.skillHubState.collectAsStateWithLifecycle()
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var skillGoalInput by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    val skills = (skillsState as? UiState.Success)?.data ?: emptyList()
    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "Skill Tree",
                    "Daftar (${skills.size})",
                    "Cari GitHub",
                    "Buat dari 0"
                )
                tabs.forEachIndexed { idx, label ->
                    androidx.compose.material3.FilterChip(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("skill_tab_$idx")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenImageStudio,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = { Text("Studio Gambar (Nano Banana 2.1)") },
                    modifier = Modifier.testTag("open_image_studio_chip")
                )
                AssistChip(
                    onClick = onOpenCyberGame,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Mainkan Game 2D 2048") },
                    modifier = Modifier.testTag("open_cyber_game_chip")
                )
            }

            if (hubState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = hubState.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (subTab) {
                0 -> {
                    // VISUAL SKILL TREE
                    when (skillsState) {
                        is UiState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        is UiState.Error -> Text(
                            text = (skillsState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )

                        is UiState.Success -> {
                            VisualSkillTreeView(
                                skills = skills,
                                sessions = sessions,
                                selectedSessionId = selectedSessionId,
                                onSelectSession = { viewModel.selectSession(it) },
                                onToggleSkillForSession = { viewModel.toggleSkillForActiveSession(it) },
                                onToggleSkillSetForSession = { list, enable ->
                                    viewModel.toggleSkillSetForActiveSession(list, enable)
                                },
                                onUseSkillInAgent = onTestSkillInAgent,
                                onNavigateToGitHub = { subTab = 2 },
                                onNavigateToAiBuilder = { subTab = 3 }
                            )
                        }
                    }
                }

                1 -> {
                    // INSTALLED SKILLS LIST
                    when (skillsState) {
                        is UiState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        is UiState.Error -> Text(
                            text = (skillsState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )

                        is UiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(skills, key = { it.id.ifBlank { it.slug } }) { skill ->
                                    InstalledSkillCard(
                                        skill = skill,
                                        onToggle = { viewModel.toggleSkillForActiveSession(skill) },
                                        onUse = { onTestSkillInAgent(skill) },
                                        onDelete = { viewModel.deleteSkill(skill.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // GITHUB SKILL EXPLORER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hubState.githubQuery,
                            onValueChange = viewModel::updateGithubQuery,
                            placeholder = { Text("Cari skill/MCP/agent di GitHub...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("github_search_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchGitHubSkills() })
                        )
                        Button(
                            onClick = { viewModel.searchGitHubSkills() },
                            enabled = !hubState.isSearchingGithub,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("github_search_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (hubState.isSearchingGithub) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = "Cari GitHub")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "hermes agent skills",
                            "mcp server tools",
                            "academic research agent",
                            "document docx pdf generator",
                            "fact check agent"
                        ).forEach { preset ->
                            AssistChip(
                                onClick = { viewModel.searchGitHubSkills(preset) },
                                label = { Text(preset, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hubState.githubRepos, key = { it.fullName }) { repo ->
                            GitHubRepoCard(
                                repo = repo,
                                isImporting = hubState.importingRepoName == repo.fullName,
                                onImport = { viewModel.importGitHubRepoAsSkill(repo) },
                                onOpenRepo = { runCatching { uriHandler.openUri(repo.htmlUrl) } }
                            )
                        }
                    }
                }

                3 -> {
                    // AI SKILL ARCHITECT (CREATE FROM 0)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "AI Skill Architect (Buat dari 0)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Jelaskan kemampuan yang Anda mau; Kafka merancang prompt, alur kerja, & format outputnya otomatis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = skillGoalInput,
                                onValueChange = { skillGoalInput = it },
                                label = { Text("Deskripsi Skill Baru") },
                                placeholder = { Text("Contoh: Skill audit jurnal medis Cochrane dan susun ringkasan ke Word (.docx)...") },
                                minLines = 4,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_skill_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Audit kontrak legal ke DOCX",
                                    "Generator Slide Pitch Deck PPTX",
                                    "Review Literatur IEEE & arXiv",
                                    "Ekstraktor Tabel Data ke CSV"
                                ).forEach { idea ->
                                    AssistChip(
                                        onClick = { skillGoalInput = idea },
                                        label = { Text(idea) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.createSkillFromScratch(skillGoalInput) {
                                        skillGoalInput = ""
                                        subTab = 0
                                    }
                                },
                                enabled = skillGoalInput.isNotBlank() && !hubState.isCreatingSkillFromZero,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("create_skill_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (hubState.isCreatingSkillFromZero) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Merancang Skill dari 0...")
                                } else {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Rancang & Pasang Skill")
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
private fun InstalledSkillCard(
    skill: AgentSkill,
    onToggle: () -> Unit,
    onUse: () -> Unit,
    onDelete: () -> Unit,
) {
    val source = SkillSourceType.fromRaw(skill.sourceType)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (skill.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = skill.outputFormat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = source.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Switch(
                    checked = skill.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("toggle_skill_${skill.slug}")
                )
            }

            Text(text = skill.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = skill.systemPrompt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dipakai ${skill.usageCount}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (source != SkillSourceType.BUILTIN) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus skill",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    OutlinedButton(onClick = onUse, shape = RoundedCornerShape(10.dp)) {
                        Text("Pakai di Agent")
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubRepoCard(
    repo: GitHubSkillRepo,
    isImporting: Boolean,
    onImport: () -> Unit,
    onOpenRepo: () -> Unit
) {
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (repo.ownerAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = repo.ownerAvatarUrl,
                        contentDescription = repo.fullName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(32.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = repo.language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${repo.stars}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text(
                text = repo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenRepo, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Repo")
                }
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mengimpor SKILL.md...")
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import jadi Skill Kafka")
                    }
                }
            }
        }
    }
}
