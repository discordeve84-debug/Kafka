package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AgentMode(val label: String, val badge: String) {
    AGENT("Hermes Agent", "AUTONOMOUS"),
    WEB_SEARCH("Live Web", "WEB"),
    JOURNAL_RESEARCH("Jurnal", "ACADEMIC"),
    FACT_VERIFY("Verifikasi", "AUDIT"),
    DOC_STUDIO("Dokumen", "EXPORT");

    companion object {
        fun fromRaw(raw: String): AgentMode =
            entries.firstOrNull { it.name == raw } ?: AGENT
    }
}

enum class VerificationStatus(val label: String) {
    VERIFIED("Terverifikasi"),
    PARTIAL("Sebagian Terbukti"),
    UNVERIFIED("Belum Terbukti"),
    NONE("Tanpa Klaim");

    companion object {
        fun fromRaw(raw: String): VerificationStatus =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class SkillCategory(val label: String) {
    DOCUMENT("Dokumen"),
    RESEARCH("Riset"),
    VERIFICATION("Verifikasi"),
    GITHUB("GitHub"),
    CUSTOM("Kustom");

    companion object {
        fun fromRaw(raw: String): SkillCategory =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

enum class SkillSourceType(val label: String) {
    BUILTIN("Bawaan Kafka"),
    GITHUB("GitHub"),
    AI_CREATED("Dibuat AI");

    companion object {
        fun fromRaw(raw: String): SkillSourceType =
            entries.firstOrNull { it.name == raw } ?: AI_CREATED
    }
}

enum class OutputFormat(val extension: String, val mimeType: String, val displayLabel: String) {
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (.docx)"),
    PDF("pdf", "application/pdf", "PDF (.pdf)"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint (.pptx)"),
    MARKDOWN("md", "text/markdown", "Markdown (.md)"),
    CSV("csv", "text/csv", "CSV (.csv)"),
    JSON("json", "application/json", "JSON"),
    RESEARCH("md", "text/markdown", "Riset");

    companion object {
        fun fromRaw(raw: String): OutputFormat =
            entries.firstOrNull { it.name == raw } ?: RESEARCH

        val exportableFormats = listOf(DOCX, PDF, PPTX, MARKDOWN, CSV)
    }
}

@Keep
@IgnoreExtraProperties
data class AgentSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Sesi Baru",
    val mode: String = AgentMode.AGENT.name,
    val activeSkillIds: List<String> = emptyList(),
    val lastSummary: String = "",
    val verifiedScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CitationItem(
    val refTag: String = "",
    val title: String = "",
    val url: String = "",
    val source: String = "",
    val snippet: String = "",
    val verified: Boolean = false,
    val sourceType: String = "WEB",
    val authorsText: String = "",
    val year: Int = 0,
    val venue: String = "",
    val doi: String = "",
    val citedByCount: Int = 0,
    val provider: String = "",
)

@Keep
@IgnoreExtraProperties
data class AgentMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "AGENT",
    val content: String = "",
    val thoughtSteps: List<String> = emptyList(),
    val verificationStatus: String = VerificationStatus.NONE.name,
    val verificationScore: Long = 0L,
    val verificationSummary: String = "",
    val citations: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun parsedCitations(): List<CitationItem> = citations.mapIndexed { idx, map ->
        CitationItem(
            refTag = (map["refTag"] as? String)?.ifBlank { null } ?: "${idx + 1}",
            title = map["title"] as? String ?: "",
            url = map["url"] as? String ?: "",
            source = map["source"] as? String ?: "",
            snippet = map["snippet"] as? String ?: "",
            verified = map["verified"] as? Boolean ?: false,
            sourceType = map["sourceType"] as? String ?: "WEB",
            authorsText = map["authorsText"] as? String ?: "",
            year = (map["year"] as? Number)?.toInt() ?: 0,
            venue = map["venue"] as? String ?: "",
            doi = map["doi"] as? String ?: "",
            citedByCount = (map["citedByCount"] as? Number)?.toInt() ?: 0,
            provider = map["provider"] as? String ?: "",
        )
    }
}

@Keep
@IgnoreExtraProperties
data class AgentSkill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val category: String = SkillCategory.CUSTOM.name,
    val sourceType: String = SkillSourceType.BUILTIN.name,
    val githubRepo: String? = null,
    val githubUrl: String? = null,
    val systemPrompt: String = "",
    val outputFormat: String = OutputFormat.RESEARCH.name,
    val enabled: Boolean = true,
    val usageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GeneratedArtifact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val title: String = "",
    val format: String = OutputFormat.DOCX.name,
    val fileName: String = "",
    val localPath: String = "",
    val summary: String = "",
    val wordCount: Long = 0L,
    val verified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

// Live Network & Verification Domain Models

data class WebSearchResult(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String,
    val sourceEngine: String,
    val reachable: Boolean = true,
)

data class JournalPaper(
    val title: String,
    val authors: List<String>,
    val year: Int,
    val venue: String,
    val doi: String?,
    val url: String,
    val abstractText: String,
    val citedByCount: Int,
    val sourceProvider: String,
    val isPeerReviewed: Boolean = true,
)

data class GitHubSkillRepo(
    val fullName: String,
    val name: String,
    val description: String,
    val htmlUrl: String,
    val defaultBranch: String,
    val stars: Int,
    val language: String,
    val topics: List<String>,
    val ownerAvatarUrl: String,
)

data class ClaimCheck(
    val claim: String,
    val verdict: String,
    val explanation: String,
    val evidenceSource: String,
    val evidenceUrl: String,
)

data class VerificationReport(
    val overallScore: Int,
    val status: VerificationStatus,
    val summary: String,
    val claims: List<ClaimCheck>,
    val sourcesChecked: Int,
    val verifiedLinks: Int,
)

data class DocumentSection(
    val heading: String,
    val body: String,
    val bullets: List<String>,
)

data class DocumentBlueprint(
    val title: String,
    val subtitle: String,
    val executiveSummary: String,
    val sections: List<DocumentSection>,
    val references: List<String>,
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val darkTheme: Boolean = true,
    val defaultFormat: String = OutputFormat.DOCX.name,
    val autoVerify: Boolean = true,
    val preferredLanguage: String = "Indonesia",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class JournalSummaryVerification(
    val keyFindings: String,
    val methodology: String,
    val limitations: String,
    val accuracyScore: Int,
    val accuracyStatus: VerificationStatus,
    val accuracyNotes: String,
    val crossCheckedSources: Int,
)

data class DocumentPreviewDraft(
    val blueprint: DocumentBlueprint,
    val format: OutputFormat,
    val sourceContent: String,
    val citations: List<CitationItem>,
    val verified: Boolean,
)

data class TrendingResearchTopic(
    val topic: String,
    val category: String,
    val momentumLabel: String,
    val sourceHint: String,
    val suggestedQuery: String,
)

@Keep
@IgnoreExtraProperties
data class ResearchSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceSessionId: String = "",
    val title: String = "",
    val notes: String = "",
    val mode: String = AgentMode.AGENT.name,
    val activeSkillIds: List<String> = emptyList(),
    val messageCount: Long = 0L,
    val messagesArchive: List<Map<String, Any>> = emptyList(),
    val citations: List<Map<String, Any>> = emptyList(),
    val hasActivePreview: Boolean = false,
    val previewFormat: String = "",
    val previewTitle: String = "",
    val previewSubtitle: String = "",
    val previewSummary: String = "",
    val previewSections: List<Map<String, Any>> = emptyList(),
    val previewReferences: List<String> = emptyList(),
    val verifiedScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedCitations(): List<CitationItem> = citations.mapIndexed { idx, map ->
        CitationItem(
            refTag = (map["refTag"] as? String)?.ifBlank { null } ?: "${idx + 1}",
            title = map["title"] as? String ?: "",
            url = map["url"] as? String ?: "",
            source = map["source"] as? String ?: "",
            snippet = map["snippet"] as? String ?: "",
            verified = map["verified"] as? Boolean ?: false,
            sourceType = map["sourceType"] as? String ?: "WEB",
            authorsText = map["authorsText"] as? String ?: "",
            year = (map["year"] as? Number)?.toInt() ?: 0,
            venue = map["venue"] as? String ?: "",
            doi = map["doi"] as? String ?: "",
            citedByCount = (map["citedByCount"] as? Number)?.toInt() ?: 0,
            provider = map["provider"] as? String ?: "",
        )
    }

    fun toPreviewDraftOrNull(): DocumentPreviewDraft? {
        if (!hasActivePreview || previewTitle.isBlank()) return null
        val sections = previewSections.map { m ->
            val bulletsRaw = m["bullets"] as? List<*> ?: emptyList<Any>()
            DocumentSection(
                heading = m["heading"] as? String ?: "Bagian",
                body = m["body"] as? String ?: "",
                bullets = bulletsRaw.mapNotNull { it as? String }
            )
        }
        val format = OutputFormat.fromRaw(previewFormat.ifBlank { OutputFormat.DOCX.name })
        return DocumentPreviewDraft(
            blueprint = DocumentBlueprint(
                title = previewTitle,
                subtitle = previewSubtitle,
                executiveSummary = previewSummary,
                sections = sections,
                references = previewReferences
            ),
            format = format,
            sourceContent = previewSummary,
            citations = parsedCitations(),
            verified = parsedCitations().isNotEmpty()
        )
    }
}


