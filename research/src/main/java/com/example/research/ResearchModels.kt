package com.example.research

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
    val pdfUrl: String? = null,
    val isOpenAccess: Boolean = false,
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

data class JournalSummaryVerification(
    val keyFindings: String,
    val methodology: String,
    val limitations: String,
    val accuracyScore: Int,
    val accuracyStatus: VerificationStatus,
    val accuracyNotes: String,
    val crossCheckedSources: Int,
)

data class LinkAuditResult(
    val url: String,
    val reachable: Boolean,
    val httpCode: Int = 0,
)
