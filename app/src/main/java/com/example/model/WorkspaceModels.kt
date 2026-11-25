package com.example.model

data class GmailMessageItem(
    val id: String,
    val threadId: String = "",
    val subject: String = "(Tanpa Subjek)",
    val from: String = "",
    val date: String = "",
    val snippet: String = "",
)

data class DriveFileItem(
    val id: String,
    val name: String,
    val mimeType: String = "",
    val modifiedTime: String = "",
    val webViewLink: String = "",
)

data class CalendarEventItem(
    val id: String,
    val summary: String,
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val htmlLink: String = "",
)

data class GoogleContactItem(
    val resourceName: String,
    val displayName: String,
    val email: String = "",
)

data class GoogleWorkspaceState(
    val accessToken: String? = null,
    val isConnecting: Boolean = false,
    val isLoadingData: Boolean = false,
    val isPerformingAction: Boolean = false,
    val emails: List<GmailMessageItem> = emptyList(),
    val driveFiles: List<DriveFileItem> = emptyList(),
    val calendarEvents: List<CalendarEventItem> = emptyList(),
    val contacts: List<GoogleContactItem> = emptyList(),
    val statusMessage: String? = null,
    val error: String? = null,
) {
    val isConnected: Boolean get() = !accessToken.isNullOrBlank()
}
