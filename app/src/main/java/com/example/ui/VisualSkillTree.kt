package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AccountTree
import androidx.compose.material.icons.sharp.AutoAwesome
import androidx.compose.material.icons.sharp.Build
import androidx.compose.material.icons.sharp.Code
import androidx.compose.material.icons.sharp.Forum
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AgentSession
import com.example.model.AgentSkill
import com.example.model.SkillSourceType
import com.example.ui.theme.KafkaAmber
import com.example.ui.theme.KafkaVerifiedGreen

@Composable
fun VisualSkillTreeView(
    skills: List<AgentSkill>,
    sessions: List<AgentSession>,
    selectedSessionId: String?,
    onSelectSession: (String) -> Unit,
    onToggleSkillForSession: (AgentSkill) -> Unit,
    onToggleSkillSetForSession: (List<AgentSkill>, Boolean) -> Unit,
    onUseSkillInAgent: (AgentSkill) -> Unit,
    onNavigateToGitHub: () -> Unit,
    onNavigateToAiBuilder: () -> Unit,
) {
    val activeSession = sessions.firstOrNull { it.id == selectedSessionId }
    val activeIds = activeSession?.activeSkillIds?.toSet()
        ?: skills.filter { it.enabled }.map { it.id }.toSet()

    val githubSkills = skills.filter { SkillSourceType.fromRaw(it.sourceType) == SkillSourceType.GITHUB }
    val aiCustomSkills = skills.filter { SkillSourceType.fromRaw(it.sourceType) == SkillSourceType.AI_CREATED }
    val builtInSkills = skills.filter { SkillSourceType.fromRaw(it.sourceType) == SkillSourceType.BUILTIN }

    val activeCount = skills.count { it.id in activeIds || (activeIds.isEmpty() && it.enabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("visual_skill_tree"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Session Selector for Session-Scoped Skill Sets
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Target Sesi Aktif",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (sessions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sessions.forEach { sess ->
                                FilterChip(
                                    selected = sess.id == selectedSessionId,
                                    onClick = { onSelectSession(sess.id) },
                                    label = {
                                        Text(
                                            text = sess.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = "Toggle branch kategori atau node individual untuk mengaktifkan/menonaktifkan set skill pada sesi '${activeSession?.title ?: "Aktif"}'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Root Node: Hermes Core
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Sharp.AccountTree,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Kafka Succubus Skill Tree",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$activeCount dari ${skills.size} node terhubung ke sesi aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        SuccubusBatWingCrest(size = 16.dp)
                    }
                }

                // Vertical Root Trunk Connector
                val trunkColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    drawLine(
                        color = trunkColor,
                        start = Offset(32.dp.toPx(), 0f),
                        end = Offset(32.dp.toPx(), size.height),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Branch 1: Discovered GitHub Skills
        item {
            SkillTreeBranchGroup(
                branchTitle = "GitHub Discovered Skills",
                branchSubtitle = "Skill yang diimpor dari repositori GitHub & MCP tools",
                icon = Icons.Sharp.Code,
                accentColor = MaterialTheme.colorScheme.tertiary,
                skillsInBranch = githubSkills,
                activeIds = activeIds,
                emptyActionLabel = "Eksplor Repo GitHub",
                onEmptyAction = onNavigateToGitHub,
                onToggleBranch = { enable -> onToggleSkillSetForSession(githubSkills, enable) },
                onToggleNode = onToggleSkillForSession,
                onUseNode = onUseSkillInAgent,
                branchTag = "branch_github"
            )
        }

        // Branch 2: Custom AI-Generated Agents
        item {
            SkillTreeBranchGroup(
                branchTitle = "AI Custom-Generated Agents",
                branchSubtitle = "Agen khusus yang dirancang dari 0 oleh Kafka AI Architect",
                icon = Icons.Sharp.AutoAwesome,
                accentColor = MaterialTheme.colorScheme.secondary,
                skillsInBranch = aiCustomSkills,
                activeIds = activeIds,
                emptyActionLabel = "Buat Agen dari 0",
                onEmptyAction = onNavigateToAiBuilder,
                onToggleBranch = { enable -> onToggleSkillSetForSession(aiCustomSkills, enable) },
                onToggleNode = onToggleSkillForSession,
                onUseNode = onUseSkillInAgent,
                branchTag = "branch_ai_custom"
            )
        }

        // Branch 3: Core Built-In Capabilities
        item {
            SkillTreeBranchGroup(
                branchTitle = "Core Research & Doc Nodes",
                branchSubtitle = "Kemampuan bawaan Word, PDF, PPTX, Jurnal & Fact Verifier",
                icon = Icons.Sharp.Build,
                accentColor = MaterialTheme.colorScheme.primary,
                skillsInBranch = builtInSkills,
                activeIds = activeIds,
                emptyActionLabel = null,
                onEmptyAction = {},
                onToggleBranch = { enable -> onToggleSkillSetForSession(builtInSkills, enable) },
                onToggleNode = onToggleSkillForSession,
                onUseNode = onUseSkillInAgent,
                branchTag = "branch_builtin"
            )
        }
    }
}

@Composable
private fun SkillTreeBranchGroup(
    branchTitle: String,
    branchSubtitle: String,
    icon: ImageVector,
    accentColor: Color,
    skillsInBranch: List<AgentSkill>,
    activeIds: Set<String>,
    emptyActionLabel: String?,
    onEmptyAction: () -> Unit,
    onToggleBranch: (Boolean) -> Unit,
    onToggleNode: (AgentSkill) -> Unit,
    onUseNode: (AgentSkill) -> Unit,
    branchTag: String,
) {
    val activeInBranch = skillsInBranch.count { it.id in activeIds }
    val allActive = skillsInBranch.isNotEmpty() && activeInBranch == skillsInBranch.size
    val lineColorMuted = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxWidth()) {
        // Branch Category Header Node
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (activeInBranch > 0) accentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(branchTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "$branchTitle ($activeInBranch/${skillsInBranch.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = branchSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                if (skillsInBranch.isNotEmpty()) {
                    Switch(
                        checked = allActive,
                        onCheckedChange = { onToggleBranch(!allActive) },
                        modifier = Modifier.testTag("${branchTag}_switch")
                    )
                }
            }
        }

        if (skillsInBranch.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Belum ada node di cabang ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (emptyActionLabel != null) {
                            OutlinedButton(
                                onClick = onEmptyAction,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(emptyActionLabel)
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                skillsInBranch.forEachIndexed { idx, skill ->
                    val isNodeActive = skill.id in activeIds
                    val isLast = idx == skillsInBranch.lastIndex
                    SkillTreeLeafNodeRow(
                        skill = skill,
                        isNodeActive = isNodeActive,
                        isLast = isLast,
                        accentColor = accentColor,
                        mutedLineColor = lineColorMuted,
                        onToggle = { onToggleNode(skill) },
                        onUse = { onUseNode(skill) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTreeLeafNodeRow(
    skill: AgentSkill,
    isNodeActive: Boolean,
    isLast: Boolean,
    accentColor: Color,
    mutedLineColor: Color,
    onToggle: () -> Unit,
    onUse: () -> Unit,
) {
    val lineColor = if (isNodeActive) accentColor else mutedLineColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tree Elbow Connector Canvas
        Canvas(
            modifier = Modifier
                .width(34.dp)
                .height(86.dp)
        ) {
            val spineX = 18.dp.toPx()
            val midY = size.height / 2f
            val effect = if (isNodeActive) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            drawLine(
                color = lineColor,
                start = Offset(spineX, 0f),
                end = Offset(spineX, if (isLast) midY else size.height),
                strokeWidth = 2.dp.toPx(),
                pathEffect = effect
            )
            drawLine(
                color = lineColor,
                start = Offset(spineX, midY),
                end = Offset(size.width, midY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = effect
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(size.width - 2.dp.toPx(), midY)
            )
        }

        // Leaf Node Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                if (isNodeActive) accentColor.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("tree_node_${skill.slug}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isNodeActive) KafkaVerifiedGreen else KafkaAmber.copy(alpha = 0.5f))
                        )
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = isNodeActive,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.testTag("tree_node_switch_${skill.slug}")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = skill.outputFormat,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (!skill.githubRepo.isNullOrBlank()) {
                            Text(
                                text = skill.githubRepo,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onUse,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Sharp.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Pakai", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
