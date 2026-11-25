package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.WorkspaceSessionStore
import com.example.data.KafkaAiEngine
import com.example.model.GeneratedArtifact
import com.example.model.GmailMessageItem
import com.example.model.GoogleWorkspaceState
import com.example.model.OutputFormat
import com.example.network.GoogleWorkspaceClient
import com.example.network.WorkspaceAuthExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val WORKSPACE_TAG = "WorkspaceViewModel"

class WorkspaceViewModel(
    private val workspaceClient: GoogleWorkspaceClient,
    private val aiEngine: KafkaAiEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GoogleWorkspaceState(accessToken = WorkspaceSessionStore.accessToken.value)
    )
    val state: StateFlow<GoogleWorkspaceState> = _state.asStateFlow()

    private val _emailSummaries = MutableStateFlow<Map<String, String>>(emptyMap())
    val emailSummaries: StateFlow<Map<String, String>> = _emailSummaries.asStateFlow()

    private val _summarizingEmailId = MutableStateFlow<String?>(null)
    val summarizingEmailId: StateFlow<String?> = _summarizingEmailId.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            WorkspaceSessionStore.accessToken.collect { token ->
                val previous = _state.value.accessToken
                _state.update { it.copy(accessToken = token) }
                if (!token.isNullOrBlank() && token != previous) {
                    refreshAll(token)
                }
            }
        }
    }

    fun setConnecting(connecting: Boolean) {
        _state.update { it.copy(isConnecting = connecting, error = if (connecting) null else it.error) }
    }

    fun onAccessTokenGranted(token: String) {
        WorkspaceSessionStore.setAccessToken(token)
        _state.update {
            it.copy(
                accessToken = token,
                isConnecting = false,
                error = null,
                statusMessage = "Google Workspace terhubung! Memuat Gmail, Drive, Calendar & Contacts..."
            )
        }
        refreshAll(token)
    }

    fun onConnectionError(message: String) {
        _state.update { it.copy(isConnecting = false, error = message) }
    }

    fun disconnectWorkspace() {
        WorkspaceSessionStore.clear()
        _emailSummaries.value = emptyMap()
        _state.value = GoogleWorkspaceState()
        _toastEvents.tryEmit("Koneksi Google Workspace diputus.")
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, statusMessage = null) }
    }

    fun refreshAll(explicitToken: String? = _state.value.accessToken) {
        val token = explicitToken?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingData = true, error = null) }
            try {
                coroutineScope {
                    val gmailDeferred = async { workspaceClient.fetchRecentEmails(token, maxResults = 12) }
                    val driveDeferred = async { workspaceClient.listDriveFiles(token, pageSize = 15) }
                    val calDeferred = async { workspaceClient.listUpcomingEvents(token, maxResults = 12) }
                    val contactsDeferred = async { workspaceClient.listContacts(token, pageSize = 25) }

                    val gmailRes = gmailDeferred.await()
                    val driveRes = driveDeferred.await()
                    val calRes = calDeferred.await()
                    val contactsRes = contactsDeferred.await()

                    val expired = listOf(gmailRes, driveRes, calRes, contactsRes)
                        .mapNotNull { it.exceptionOrNull() }
                        .firstOrNull { it is WorkspaceAuthExpiredException }

                    if (expired != null) {
                        WorkspaceSessionStore.clear()
                        _state.update {
                            it.copy(
                                accessToken = null,
                                isLoadingData = false,
                                error = expired.message
                            )
                        }
                        return@coroutineScope
                    }

                    val firstErr = listOf(gmailRes, driveRes, calRes, contactsRes)
                        .mapNotNull { it.exceptionOrNull()?.localizedMessage }
                        .firstOrNull()

                    _state.update { cur ->
                        cur.copy(
                            isLoadingData = false,
                            emails = gmailRes.getOrElse { cur.emails },
                            driveFiles = driveRes.getOrElse { cur.driveFiles },
                            calendarEvents = calRes.getOrElse { cur.calendarEvents },
                            contacts = contactsRes.getOrElse { cur.contacts },
                            statusMessage = if (firstErr == null) {
                                "Sinkronisasi selesai (${gmailRes.getOrNull()?.size ?: 0} Gmail, ${driveRes.getOrNull()?.size ?: 0} Drive, ${calRes.getOrNull()?.size ?: 0} Agenda)."
                            } else cur.statusMessage,
                            error = firstErr
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(WORKSPACE_TAG, "Workspace refresh failed", e)
                _state.update {
                    it.copy(
                        isLoadingData = false,
                        error = e.localizedMessage ?: "Gagal menyinkronkan Google Workspace."
                    )
                }
            }
        }
    }

    fun sendGmail(to: String, subject: String, body: String, onSuccess: () -> Unit = {}) {
        val token = _state.value.accessToken
        if (token.isNullOrBlank()) {
            _toastEvents.tryEmit("Hubungkan Google Workspace terlebih dahulu untuk mengirim Gmail.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingAction = true, error = null) }
            workspaceClient.sendEmail(token, to, subject, body)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isPerformingAction = false,
                            statusMessage = "Email berhasil dikirim ke ${to.trim()}."
                        )
                    }
                    _toastEvents.tryEmit("Email terkirim via Gmail!")
                    onSuccess()
                    refreshAll(token)
                }
                .onFailure { err ->
                    handleActionFailure("Gagal mengirim email", err)
                }
        }
    }

    fun summarizeEmailWithKafka(email: GmailMessageItem) {
        if (_summarizingEmailId.value == email.id) return
        viewModelScope.launch {
            _summarizingEmailId.value = email.id
            try {
                val prompt = buildString {
                    appendLine("Ringkas email berikut secara singkat (maksimal 4 poin padat) dalam bahasa Indonesia dan sebutkan tindakan yang perlu dilakukan:")
                    appendLine("Pengirim: ${email.from}")
                    appendLine("Subjek: ${email.subject}")
                    appendLine("Cuplikan: ${email.snippet}")
                }
                val summary = aiEngine.summarizeQuickText(prompt)
                _emailSummaries.update { it + (email.id to summary) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastEvents.tryEmit("Gagal meringkas email: ${e.localizedMessage}")
            } finally {
                _summarizingEmailId.value = null
            }
        }
    }

    fun uploadArtifactToDrive(artifact: GeneratedArtifact) {
        val token = _state.value.accessToken
        if (token.isNullOrBlank()) {
            _toastEvents.tryEmit("Hubungkan Google Workspace di menu Workspace untuk upload ke Drive.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingAction = true, error = null) }
            val bytesResult = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(artifact.localPath)
                    require(file.exists()) { "File lokal ${artifact.fileName} tidak ditemukan di perangkat." }
                    file.readBytes()
                }
            }
            val bytes = bytesResult.getOrElse { err ->
                handleActionFailure("File lokal tidak terbaca", err)
                return@launch
            }
            val mimeType = OutputFormat.fromRaw(artifact.format).mimeType
            workspaceClient.uploadFileToDrive(token, artifact.fileName, mimeType, bytes)
                .onSuccess { uploaded ->
                    _state.update {
                        it.copy(
                            isPerformingAction = false,
                            driveFiles = listOf(uploaded) + it.driveFiles.filterNot { f -> f.id == uploaded.id },
                            statusMessage = "'${uploaded.name}' berhasil diunggah ke Google Drive."
                        )
                    }
                    _toastEvents.tryEmit("Berhasil upload '${uploaded.name}' ke Google Drive!")
                }
                .onFailure { err ->
                    handleActionFailure("Gagal mengunggah ke Drive", err)
                }
        }
    }

    fun createGoogleDoc(title: String, content: String, onSuccess: () -> Unit = {}) {
        val token = _state.value.accessToken
        if (token.isNullOrBlank()) {
            _toastEvents.tryEmit("Hubungkan Google Workspace terlebih dahulu untuk membuat Google Docs.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingAction = true, error = null) }
            workspaceClient.createGoogleDoc(token, title, content)
                .onSuccess { created ->
                    _state.update {
                        it.copy(
                            isPerformingAction = false,
                            driveFiles = listOf(created) + it.driveFiles,
                            statusMessage = "Google Docs '${created.name}' dibuat di Drive Anda."
                        )
                    }
                    _toastEvents.tryEmit("Google Docs '${created.name}' dibuat!")
                    onSuccess()
                }
                .onFailure { err ->
                    handleActionFailure("Gagal membuat Google Docs", err)
                }
        }
    }

    fun createGoogleSheetFromArtifacts(title: String, artifacts: List<GeneratedArtifact>) {
        val token = _state.value.accessToken
        if (token.isNullOrBlank()) {
            _toastEvents.tryEmit("Hubungkan Google Workspace terlebih dahulu untuk membuat Google Sheets.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingAction = true, error = null) }
            val rows = buildList {
                add(listOf("Judul Dokumen", "Format", "Nama File", "Jumlah Kata", "Terverifikasi"))
                if (artifacts.isEmpty()) {
                    add(listOf("Laporan Awal Kafka", "DOCX", "kafka_report.docx", "0", "Ya"))
                } else {
                    artifacts.take(25).forEach { art ->
                        add(
                            listOf(
                                art.title,
                                art.format,
                                art.fileName,
                                art.wordCount.toString(),
                                if (art.verified) "Terverifikasi" else "Draft"
                            )
                        )
                    }
                }
            }
            workspaceClient.createGoogleSheet(token, title.ifBlank { "Indeks Riset Kafka" }, rows)
                .onSuccess { created ->
                    _state.update {
                        it.copy(
                            isPerformingAction = false,
                            driveFiles = listOf(created) + it.driveFiles,
                            statusMessage = "Google Sheets '${created.name}' dibuat."
                        )
                    }
                    _toastEvents.tryEmit("Google Sheets '${created.name}' dibuat!")
                }
                .onFailure { err ->
                    handleActionFailure("Gagal membuat Google Sheets", err)
                }
        }
    }

    fun createCalendarReminder(
        summary: String,
        description: String,
        hoursFromNow: Int = 24,
        onSuccess: () -> Unit = {},
    ) {
        val token = _state.value.accessToken
        if (token.isNullOrBlank()) {
            _toastEvents.tryEmit("Hubungkan Google Workspace terlebih dahulu untuk membuat agenda Calendar.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingAction = true, error = null) }
            val startMillis = System.currentTimeMillis() + hoursFromNow.coerceAtLeast(1) * 3_600_000L
            workspaceClient.createCalendarEvent(token, summary, description, startMillis)
                .onSuccess { created ->
                    _state.update {
                        it.copy(
                            isPerformingAction = false,
                            calendarEvents = listOf(created) + it.calendarEvents,
                            statusMessage = "Agenda '${created.summary}' ditambahkan ke Google Calendar."
                        )
                    }
                    _toastEvents.tryEmit("Agenda ditambahkan ke Google Calendar!")
                    onSuccess()
                }
                .onFailure { err ->
                    handleActionFailure("Gagal membuat agenda Calendar", err)
                }
        }
    }

    private fun handleActionFailure(prefix: String, err: Throwable) {
        if (err is WorkspaceAuthExpiredException) {
            WorkspaceSessionStore.clear()
            _state.update {
                it.copy(
                    accessToken = null,
                    isPerformingAction = false,
                    error = err.message
                )
            }
        } else {
            _state.update {
                it.copy(
                    isPerformingAction = false,
                    error = "$prefix: ${err.localizedMessage ?: err::class.simpleName}"
                )
            }
        }
    }
}
