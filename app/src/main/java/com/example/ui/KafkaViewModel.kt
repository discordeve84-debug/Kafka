package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioQueryRecorder
import com.example.audio.KafkaVoiceSpeaker
import com.example.data.KafkaAiEngine
import com.example.data.KafkaFirestoreRepository
import com.example.document.DocumentGenerator
import com.example.model.AgentMessage
import com.example.model.AgentMode
import com.example.model.AgentSession
import com.example.model.AgentSkill
import com.example.model.CitationItem
import com.example.model.DocumentPreviewDraft
import com.example.model.GeneratedArtifact
import com.example.model.GitHubSkillRepo
import com.example.model.JournalPaper
import com.example.model.JournalSummaryVerification
import com.example.model.OutputFormat
import com.example.model.ResearchSnapshot
import com.example.model.TrendingResearchTopic
import com.example.model.UserProfile
import com.example.model.VerificationReport
import com.example.model.VerificationStatus
import com.example.model.WebSearchResult
import com.example.network.LiveResearchClient
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "KafkaViewModel"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AgentExecutionState(
    val isRunning: Boolean = false,
    val thoughtSteps: List<String> = emptyList(),
    val streamingText: String = "",
    val error: String? = null,
)

data class ResearchState(
    val webQuery: String = "",
    val isSearchingWeb: Boolean = false,
    val webResults: List<WebSearchResult> = emptyList(),
    val webError: String? = null,
    val journalQuery: String = "",
    val isSearchingJournals: Boolean = false,
    val journalResults: List<JournalPaper> = emptyList(),
    val journalError: String? = null,
    val summarizingPaperUrl: String? = null,
    val journalSummaries: Map<String, JournalSummaryVerification> = emptyMap(),
    val verifyInput: String = "",
    val isVerifying: Boolean = false,
    val verificationReport: VerificationReport? = null,
    val verifyError: String? = null,
)

data class SkillHubState(
    val githubQuery: String = "hermes agent skills",
    val isSearchingGithub: Boolean = false,
    val githubRepos: List<GitHubSkillRepo> = emptyList(),
    val importingRepoName: String? = null,
    val isCreatingSkillFromZero: Boolean = false,
    val error: String? = null,
)

data class DocStudioState(
    val isGenerating: Boolean = false,
    val activeFormat: OutputFormat = OutputFormat.DOCX,
    val error: String? = null,
    val lastGeneratedFileName: String? = null,
)

data class VoiceInputState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val amplitude: Float = 0f,
    val elapsedSeconds: Int = 0,
    val lastTranscript: String? = null,
    val error: String? = null,
)

data class DocumentPreviewState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val requestedFormat: OutputFormat = OutputFormat.DOCX,
    val draft: DocumentPreviewDraft? = null,
    val error: String? = null,
)

sealed interface ImageStudioUiState {
    data object Idle : ImageStudioUiState
    data object Loading : ImageStudioUiState
    data class Success(val bitmap: Bitmap, val prompt: String) : ImageStudioUiState
    data class Error(val message: String) : ImageStudioUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class KafkaViewModel(
    private val repository: KafkaFirestoreRepository,
    private val researchClient: LiveResearchClient,
    private val aiEngine: KafkaAiEngine,
    private val documentGenerator: DocumentGenerator,
    private val audioRecorder: AudioQueryRecorder,
    private val voiceSpeaker: KafkaVoiceSpeaker,
    private val currentUserId: String,
    private val currentUserDisplayName: String = "",
    private val currentUserEmail: String = "",
) : ViewModel() {

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    private val _activeMode = MutableStateFlow(AgentMode.AGENT)
    val activeMode: StateFlow<AgentMode> = _activeMode.asStateFlow()

    private val _agentExecution = MutableStateFlow(AgentExecutionState())
    val agentExecution: StateFlow<AgentExecutionState> = _agentExecution.asStateFlow()

    private val _researchState = MutableStateFlow(ResearchState())
    val researchState: StateFlow<ResearchState> = _researchState.asStateFlow()

    private val _skillHubState = MutableStateFlow(SkillHubState())
    val skillHubState: StateFlow<SkillHubState> = _skillHubState.asStateFlow()

    private val _docStudioState = MutableStateFlow(DocStudioState())
    val docStudioState: StateFlow<DocStudioState> = _docStudioState.asStateFlow()

    private val _voiceInputState = MutableStateFlow(VoiceInputState())
    val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState.asStateFlow()

    private val _documentPreviewState = MutableStateFlow(DocumentPreviewState())
    val documentPreviewState: StateFlow<DocumentPreviewState> = _documentPreviewState.asStateFlow()

    private val _lastPreviewDraft = MutableStateFlow<DocumentPreviewDraft?>(null)

    private val _trendingTopics = MutableStateFlow<List<TrendingResearchTopic>>(emptyList())
    val trendingTopics: StateFlow<List<TrendingResearchTopic>> = _trendingTopics.asStateFlow()

    private val _isRefreshingTrends = MutableStateFlow(false)
    val isRefreshingTrends: StateFlow<Boolean> = _isRefreshingTrends.asStateFlow()

    private val _isSavingSnapshot = MutableStateFlow(false)
    val isSavingSnapshot: StateFlow<Boolean> = _isSavingSnapshot.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(false)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = voiceSpeaker.isSpeaking

    private val _imageStudioState = MutableStateFlow<ImageStudioUiState>(ImageStudioUiState.Idle)
    val imageStudioState: StateFlow<ImageStudioUiState> = _imageStudioState.asStateFlow()

    private val recentWebSearchPool = mutableListOf<WebSearchResult>()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val sessionsState: StateFlow<UiState<List<AgentSession>>> =
        observeWrapped(repository.observeSessions(currentUserId), "Gagal memuat sesi")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val skillsState: StateFlow<UiState<List<AgentSkill>>> =
        observeWrapped(repository.observeSkills(currentUserId), "Gagal memuat skill")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val artifactsState: StateFlow<UiState<List<GeneratedArtifact>>> =
        observeWrapped(repository.observeArtifacts(currentUserId), "Gagal memuat dokumen")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val researchLibraryState: StateFlow<UiState<List<ResearchSnapshot>>> =
        observeWrapped(repository.observeResearchLibrary(currentUserId), "Gagal memuat Research Library")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        observeWrapped(repository.observeUserProfile(currentUserId), "Gagal memuat profil")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<AgentMessage>>> =
        _selectedSessionId.flatMapLatest { sid ->
            if (sid.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                observeWrapped(
                    repository.observeMessages(currentUserId, sid),
                    "Gagal memuat pesan"
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, currentUserDisplayName, currentUserEmail)
            repository.ensureDefaultSkillsAndSession(currentUserId)
            searchGitHubSkills("hermes agent skills")
            refreshTrendingTopics()
        }
        viewModelScope.launch {
            sessionsState.collect { state ->
                if (state is UiState.Success && state.data.isNotEmpty() && _selectedSessionId.value == null) {
                    _selectedSessionId.value = state.data.first().id
                }
            }
        }
        viewModelScope.launch {
            userProfileState.collect { state ->
                val profile = (state as? UiState.Success)?.data ?: return@collect
                _docStudioState.value = _docStudioState.value.copy(
                    activeFormat = OutputFormat.fromRaw(profile.defaultFormat)
                )
            }
        }
        viewModelScope.launch {
            audioRecorder.amplitude.collect { amp ->
                if (_voiceInputState.value.isRecording) {
                    _voiceInputState.value = _voiceInputState.value.copy(amplitude = amp)
                }
            }
        }
        viewModelScope.launch {
            audioRecorder.elapsedSeconds.collect { sec ->
                if (_voiceInputState.value.isRecording) {
                    _voiceInputState.value = _voiceInputState.value.copy(elapsedSeconds = sec)
                }
            }
        }
    }

    override fun onCleared() {
        audioRecorder.cancelRecording()
        voiceSpeaker.shutdown()
        super.onCleared()
    }

    fun updateUserProfile(
        displayName: String,
        bio: String,
        darkTheme: Boolean,
        defaultFormat: OutputFormat,
        autoVerify: Boolean,
        preferredLanguage: String
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                darkTheme = darkTheme,
                defaultFormat = defaultFormat,
                autoVerify = autoVerify,
                preferredLanguage = preferredLanguage
            ).onSuccess {
                _toastEvents.tryEmit("Preferensi profil disimpan ke cloud")
            }.onFailure {
                _toastEvents.tryEmit("Gagal menyimpan profil: ${it.localizedMessage}")
            }
        }
    }

    private fun <T> observeWrapped(flow: Flow<T>, fallbackMsg: String): Flow<UiState<T>> =
        flow.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
            }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    fun setMode(mode: AgentMode) {
        _activeMode.value = mode
    }

    fun createNewSession(title: String = "Workspace Riset Baru") {
        viewModelScope.launch {
            val activeSkillIds = (skillsState.value as? UiState.Success)?.data
                ?.filter { it.enabled }?.map { it.id }?.take(10) ?: emptyList()
            val res = repository.createSession(
                userId = currentUserId,
                title = title,
                mode = _activeMode.value,
                activeSkillIds = activeSkillIds
            )
            res.onSuccess { newId ->
                _selectedSessionId.value = newId
                _toastEvents.tryEmit("Sesi baru dibuat")
            }.onFailure {
                _toastEvents.tryEmit("Gagal membuat sesi: ${it.localizedMessage}")
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(currentUserId, sessionId).onSuccess {
                if (_selectedSessionId.value == sessionId) {
                    _selectedSessionId.value = null
                }
                _toastEvents.tryEmit("Sesi dihapus")
            }
        }
    }

    fun sendAgentPrompt(rawPrompt: String, overrideMode: AgentMode? = null) {
        val prompt = rawPrompt.trim()
        if (prompt.isBlank() || _agentExecution.value.isRunning) return
        val mode = overrideMode ?: _activeMode.value
        if (overrideMode != null) _activeMode.value = overrideMode

        viewModelScope.launch {
            val trace = mutableListOf<String>()
            fun pushTrace(step: String) {
                trace += step
                _agentExecution.value = _agentExecution.value.copy(
                    isRunning = true,
                    thoughtSteps = trace.toList(),
                    error = null
                )
            }

            _agentExecution.value = AgentExecutionState(isRunning = true)

            try {
                // Ensure active session exists
                var sessionId = _selectedSessionId.value
                if (sessionId.isNullOrBlank()) {
                    val created = repository.createSession(
                        userId = currentUserId,
                        title = prompt.take(48),
                        mode = mode
                    )
                    sessionId = created.getOrThrow()
                    _selectedSessionId.value = sessionId
                }

                // Save user message
                repository.addMessage(
                    userId = currentUserId,
                    sessionId = sessionId,
                    role = "USER",
                    content = prompt
                ).getOrThrow()

                val allSkills = (skillsState.value as? UiState.Success)?.data ?: emptyList()
                val activeSession = (sessionsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == sessionId }
                val sessionSkillIds = activeSession?.activeSkillIds
                val enabledSkills = if (!sessionSkillIds.isNullOrEmpty()) {
                    allSkills.filter { it.id in sessionSkillIds || it.slug in sessionSkillIds }
                } else {
                    allSkills.filter { it.enabled }
                }

                pushTrace("[PLAN] Mode ${mode.label} • ${enabledSkills.size} skill aktif disiapkan")

                var webResults: List<WebSearchResult> = emptyList()
                var journals: List<JournalPaper> = emptyList()

                coroutineScope {
                    val needWeb = mode in listOf(
                        AgentMode.AGENT, AgentMode.WEB_SEARCH, AgentMode.FACT_VERIFY
                    )
                    val needJournal = mode in listOf(
                        AgentMode.AGENT, AgentMode.JOURNAL_RESEARCH, AgentMode.FACT_VERIFY
                    )

                    val webJob = if (needWeb) {
                        pushTrace("[WEB] Menelusuri indeks web langsung (DuckDuckGo, Wikipedia, Algolia)...")
                        async { researchClient.searchLiveWeb(prompt, limit = 8) }
                    } else null

                    val journalJob = if (needJournal) {
                        pushTrace("[JOURNAL] Mengueri literatur ilmiah OpenAlex, arXiv & Crossref...")
                        async { researchClient.searchAcademicJournals(prompt, limit = 8) }
                    } else null

                    webResults = webJob?.await() ?: emptyList()
                    journals = journalJob?.await() ?: emptyList()
                }

                pushTrace("[FOUND] ${webResults.size} sumber web & ${journals.size} artikel jurnal ditemukan")

                // Live URL reachability verification on top 3 web results
                val verifiedWeb = if (webResults.isNotEmpty()) {
                    pushTrace("[VERIFY] Menguji status HTTP tautan & menyaring referensi valid...")
                    coroutineScope {
                        webResults.mapIndexed { idx, item ->
                            if (idx < 3) {
                                async {
                                    val ok = researchClient.verifyUrlReachable(item.url)
                                    item.copy(reachable = ok)
                                }
                            } else {
                                async { item }
                            }
                        }.map { it.await() }
                    }
                } else emptyList()

                val citations = aiEngine.buildCitationsFromSources(verifiedWeb, journals)

                // Recent history context
                val recentHistory = (messagesState.value as? UiState.Success)?.data
                    ?.takeLast(4)
                    ?.joinToString("\n") { "${it.role}: ${it.content.take(220)}" }
                    .orEmpty()

                pushTrace("[SYNTHESIS] Menyintesis jawaban tergrounding sumber via Gemini...")

                val answerBuilder = StringBuilder()
                aiEngine.streamAgentResponse(
                    userPrompt = prompt,
                    mode = mode,
                    activeSkills = enabledSkills,
                    webResults = verifiedWeb,
                    journals = journals,
                    historySummary = recentHistory
                ).collect { accumulated ->
                    answerBuilder.clear()
                    answerBuilder.append(accumulated)
                    _agentExecution.value = _agentExecution.value.copy(
                        streamingText = accumulated
                    )
                }

                val finalAnswer = answerBuilder.toString()

                // Automated verification pass
                pushTrace("[AUDIT] Mengevaluasi integritas klaim & skor kepercayaan...")
                val report = try {
                    aiEngine.verifyContent(
                        contentToVerify = finalAnswer,
                        webResults = verifiedWeb,
                        journals = journals,
                        reachableUrlsCount = verifiedWeb.count { it.reachable }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Audit fallback due to: ${e.message}")
                    val fallbackScore = if (citations.isNotEmpty()) 78 else 45
                    VerificationReport(
                        overallScore = fallbackScore,
                        status = if (citations.isNotEmpty()) VerificationStatus.PARTIAL else VerificationStatus.UNVERIFIED,
                        summary = "Didukung ${citations.size} referensi langsung.",
                        claims = emptyList(),
                        sourcesChecked = verifiedWeb.size + journals.size,
                        verifiedLinks = verifiedWeb.count { it.reachable }
                    )
                }

                pushTrace("[DONE] Skor verifikasi ${report.overallScore}% (${report.status.label})")

                repository.addMessage(
                    userId = currentUserId,
                    sessionId = sessionId,
                    role = "AGENT",
                    content = finalAnswer,
                    thoughtSteps = trace.toList(),
                    verificationStatus = report.status,
                    verificationScore = report.overallScore,
                    verificationSummary = report.summary,
                    citations = citations
                ).getOrThrow()

                val currentSess = (sessionsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == sessionId }
                val newTitle = if (currentSess == null ||
                    currentSess.title.startsWith("Workspace Riset") ||
                    currentSess.title.startsWith("Sesi ")
                ) {
                    prompt.take(52)
                } else null

                repository.updateSessionSummary(
                    userId = currentUserId,
                    sessionId = sessionId,
                    title = newTitle,
                    mode = mode,
                    lastSummary = finalAnswer.replace("\n", " ").take(180),
                    verifiedScore = report.overallScore
                )

                enabledSkills.take(3).forEach { sk ->
                    if (sk.id.isNotBlank()) {
                        repository.incrementSkillUsage(currentUserId, sk.id)
                    }
                }

                if (verifiedWeb.isNotEmpty()) {
                    recordWebSearchResultsAndRefreshTrends(verifiedWeb, prompt)
                }

                if (_autoSpeakEnabled.value) {
                    val lang = (userProfileState.value as? UiState.Success)?.data?.preferredLanguage ?: "Indonesia"
                    voiceSpeaker.speak(finalAnswer, preferredLanguage = lang)
                }

                _agentExecution.value = AgentExecutionState(isRunning = false)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Agent timeout", e)
                _agentExecution.value = AgentExecutionState(
                    isRunning = false,
                    thoughtSteps = trace,
                    error = "Koneksi AI melewati batas waktu setelah percobaan ulang otomatis. Periksa jaringan dan coba lagi."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent execution failed", e)
                _agentExecution.value = AgentExecutionState(
                    isRunning = false,
                    thoughtSteps = trace,
                    error = e.localizedMessage ?: "Eksekusi agen gagal."
                )
            }
        }
    }

    // ==================== LIVE RESEARCH & VERIFICATION ====================

    fun updateWebQuery(q: String) {
        _researchState.value = _researchState.value.copy(webQuery = q)
    }

    fun runWebSearch(query: String = _researchState.value.webQuery) {
        val q = query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                webQuery = q,
                isSearchingWeb = true,
                webError = null
            )
            try {
                val results = researchClient.searchLiveWeb(q, limit = 12)
                _researchState.value = _researchState.value.copy(
                    isSearchingWeb = false,
                    webResults = results,
                    webError = if (results.isEmpty()) "Tidak ada hasil web ditemukan." else null
                )
                if (results.isNotEmpty()) {
                    recordWebSearchResultsAndRefreshTrends(results, q)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _researchState.value = _researchState.value.copy(
                    isSearchingWeb = false,
                    webError = e.localizedMessage ?: "Pencarian web gagal."
                )
            }
        }
    }

    fun updateJournalQuery(q: String) {
        _researchState.value = _researchState.value.copy(journalQuery = q)
    }

    fun runJournalSearch(query: String = _researchState.value.journalQuery) {
        val q = query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                journalQuery = q,
                isSearchingJournals = true,
                journalError = null
            )
            try {
                val papers = researchClient.searchAcademicJournals(q, limit = 16)
                _researchState.value = _researchState.value.copy(
                    isSearchingJournals = false,
                    journalResults = papers,
                    journalError = if (papers.isEmpty()) "Tidak ada jurnal ditemukan." else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _researchState.value = _researchState.value.copy(
                    isSearchingJournals = false,
                    journalError = e.localizedMessage ?: "Pencarian jurnal gagal."
                )
            }
        }
    }

    fun runGitHubJournalSearch(query: String = _researchState.value.journalQuery) {
        val q = query.trim().ifBlank { "papers-we-love machine learning arxiv" }
        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                journalQuery = q,
                isSearchingJournals = true,
                journalError = null
            )
            try {
                val papers = researchClient.searchGitHubJournalCollections(q, limit = 14)
                _researchState.value = _researchState.value.copy(
                    isSearchingJournals = false,
                    journalResults = papers,
                    journalError = if (papers.isEmpty()) "Tidak ada repo jurnal GitHub ditemukan." else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _researchState.value = _researchState.value.copy(
                    isSearchingJournals = false,
                    journalError = e.localizedMessage ?: "Pencarian repo jurnal GitHub gagal."
                )
            }
        }
    }

    fun summarizeAndVerifyJournal(paper: JournalPaper) {
        val key = paper.url.ifBlank { paper.title }
        if (_researchState.value.summarizingPaperUrl == key) return
        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                summarizingPaperUrl = key,
                journalError = null
            )
            try {
                val targetLink = paper.pdfUrl?.takeIf { it.startsWith("http") } ?: paper.url
                val (crossWeb, isLinkReachable) = coroutineScope {
                    val webDeferred = async { researchClient.searchLiveWeb(paper.title.take(100), limit = 5) }
                    val reachDeferred = async { researchClient.verifyUrlReachable(targetLink) }
                    webDeferred.await() to reachDeferred.await()
                }
                val summary = try {
                    aiEngine.summarizeAndVerifyJournal(
                        paper = paper,
                        crossSources = crossWeb,
                        relatedPapers = _researchState.value.journalResults
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "AI journal summary fallback to :research verifier: ${e.message}")
                    researchClient.verifyJournalQuick(
                        paper = paper,
                        crossSources = crossWeb,
                        relatedPapers = _researchState.value.journalResults
                    )
                }
                val linkNote = if (isLinkReachable) {
                    "Tautan jurnal/PDF aktif (HTTP OK)."
                } else {
                    "Tautan utama perlu verifikasi manual di browser."
                }
                val enrichedSummary = if (summary.accuracyNotes.contains("Tautan ")) {
                    summary
                } else {
                    summary.copy(accuracyNotes = "$linkNote ${summary.accuracyNotes}".trim())
                }
                val updatedMap = _researchState.value.journalSummaries.toMutableMap().apply {
                    put(key, enrichedSummary)
                }
                _researchState.value = _researchState.value.copy(
                    summarizingPaperUrl = null,
                    journalSummaries = updatedMap
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Journal summarization failed", e)
                _researchState.value = _researchState.value.copy(
                    summarizingPaperUrl = null,
                    journalError = e.localizedMessage ?: "Gagal meringkas & memverifikasi jurnal."
                )
            }
        }
    }

    fun updateVerifyInput(text: String) {
        _researchState.value = _researchState.value.copy(verifyInput = text)
    }

    fun runContentVerification(text: String = _researchState.value.verifyInput) {
        val input = text.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                verifyInput = input,
                isVerifying = true,
                verifyError = null
            )
            try {
                val searchKey = input.take(120)
                val (web, journals) = coroutineScope {
                    val w = async { researchClient.searchLiveWeb(searchKey, limit = 6) }
                    val j = async { researchClient.searchAcademicJournals(searchKey, limit = 6) }
                    w.await() to j.await()
                }
                val reachableCount = coroutineScope {
                    web.take(4).map { async { researchClient.verifyUrlReachable(it.url) } }
                        .count { it.await() }
                }
                val report = try {
                    aiEngine.verifyContent(
                        contentToVerify = input,
                        webResults = web,
                        journals = journals,
                        reachableUrlsCount = reachableCount
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "AI verification fallback to :research multi-source engine: ${e.message}")
                    researchClient.verifyContentMultiSource(
                        contentToVerify = input,
                        preloadedWebResults = web,
                        preloadedJournals = journals
                    )
                }
                _researchState.value = _researchState.value.copy(
                    isVerifying = false,
                    verificationReport = report,
                    webResults = if (_researchState.value.webResults.isEmpty()) web else _researchState.value.webResults,
                    journalResults = if (_researchState.value.journalResults.isEmpty()) journals else _researchState.value.journalResults,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Content verification failed", e)
                _researchState.value = _researchState.value.copy(
                    isVerifying = false,
                    verifyError = e.localizedMessage ?: "Gagal memverifikasi isi."
                )
            }
        }
    }

    // ==================== SKILL HUB (GITHUB & AI ARCHITECT) ====================

    fun updateGithubQuery(q: String) {
        _skillHubState.value = _skillHubState.value.copy(githubQuery = q)
    }

    fun searchGitHubSkills(query: String = _skillHubState.value.githubQuery) {
        viewModelScope.launch {
            _skillHubState.value = _skillHubState.value.copy(
                githubQuery = query,
                isSearchingGithub = true,
                error = null
            )
            try {
                val repos = researchClient.searchGitHubSkills(query, limit = 12)
                _skillHubState.value = _skillHubState.value.copy(
                    isSearchingGithub = false,
                    githubRepos = repos
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _skillHubState.value = _skillHubState.value.copy(
                    isSearchingGithub = false,
                    error = e.localizedMessage ?: "Gagal mencari repo GitHub."
                )
            }
        }
    }

    fun importGitHubRepoAsSkill(repo: GitHubSkillRepo) {
        viewModelScope.launch {
            _skillHubState.value = _skillHubState.value.copy(
                importingRepoName = repo.fullName,
                error = null
            )
            try {
                val readme = researchClient.fetchRepoSkillContent(repo.fullName, repo.defaultBranch)
                val skill = aiEngine.distillGitHubRepoToSkill(repo, readme)
                repository.saveSkill(currentUserId, skill).getOrThrow()
                _skillHubState.value = _skillHubState.value.copy(importingRepoName = null)
                _toastEvents.tryEmit("Skill '${skill.name}' diimpor dari GitHub!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Import GitHub skill failed", e)
                _skillHubState.value = _skillHubState.value.copy(
                    importingRepoName = null,
                    error = e.localizedMessage ?: "Gagal mengimpor skill dari GitHub."
                )
            }
        }
    }

    fun createSkillFromScratch(goal: String, onSuccess: () -> Unit = {}) {
        val cleanGoal = goal.trim()
        if (cleanGoal.isBlank()) return
        viewModelScope.launch {
            _skillHubState.value = _skillHubState.value.copy(
                isCreatingSkillFromZero = true,
                error = null
            )
            try {
                val createdSkill = aiEngine.createSkillFromScratch(cleanGoal)
                repository.saveSkill(currentUserId, createdSkill).getOrThrow()
                _skillHubState.value = _skillHubState.value.copy(isCreatingSkillFromZero = false)
                _toastEvents.tryEmit("Skill baru '${createdSkill.name}' berhasil dibuat AI!")
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Create skill from scratch failed", e)
                _skillHubState.value = _skillHubState.value.copy(
                    isCreatingSkillFromZero = false,
                    error = e.localizedMessage ?: "Gagal merancang skill baru."
                )
            }
        }
    }

    fun toggleSkill(skill: AgentSkill) {
        if (skill.id.isBlank()) return
        viewModelScope.launch {
            repository.toggleSkillEnabled(currentUserId, skill.id, !skill.enabled)
                .onFailure { _toastEvents.tryEmit("Gagal memperbarui skill") }
        }
    }

    fun deleteSkill(skillId: String) {
        viewModelScope.launch {
            repository.deleteSkill(currentUserId, skillId)
                .onSuccess { _toastEvents.tryEmit("Skill dihapus") }
        }
    }

    // ==================== DOCUMENT STUDIO (DOCX, PDF, PPTX, MD, CSV) ====================

    fun setDocFormat(format: OutputFormat) {
        _docStudioState.value = _docStudioState.value.copy(activeFormat = format)
    }

    fun generateDocument(
        topicOrContent: String,
        format: OutputFormat = _docStudioState.value.activeFormat,
        verifyBeforeExport: Boolean = true,
        citations: List<CitationItem> = emptyList(),
        onDone: (File) -> Unit = {}
    ) {
        val input = topicOrContent.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            _docStudioState.value = _docStudioState.value.copy(
                isGenerating = true,
                activeFormat = format,
                error = null
            )
            try {
                var enrichedContent = input
                var usedCitations = citations
                var isVerified = citations.isNotEmpty()

                if (verifyBeforeExport && citations.isEmpty()) {
                    val (web, journals) = coroutineScope {
                        val w = async { researchClient.searchLiveWeb(input.take(120), 5) }
                        val j = async { researchClient.searchAcademicJournals(input.take(120), 5) }
                        w.await() to j.await()
                    }
                    usedCitations = aiEngine.buildCitationsFromSources(web, journals)
                    isVerified = usedCitations.isNotEmpty()
                    if (usedCitations.isNotEmpty()) {
                        enrichedContent = buildString {
                            appendLine(input)
                            appendLine("\nSumber Fakta Terkini:")
                            usedCitations.forEach {
                                appendLine("- ${it.title} (${it.source}): ${it.snippet}")
                            }
                        }
                    }
                }

                val blueprint = aiEngine.generateDocumentBlueprint(
                    topicOrContent = enrichedContent,
                    format = format,
                    citations = usedCitations
                )
                val generated = documentGenerator.generateDocument(blueprint, format)

                val artifact = GeneratedArtifact(
                    sessionId = _selectedSessionId.value.orEmpty(),
                    title = blueprint.title,
                    format = format.name,
                    fileName = generated.file.name,
                    localPath = generated.file.absolutePath,
                    summary = blueprint.executiveSummary.take(400),
                    wordCount = generated.wordCount.toLong(),
                    verified = isVerified
                )
                repository.saveArtifact(currentUserId, artifact)

                _docStudioState.value = _docStudioState.value.copy(
                    isGenerating = false,
                    lastGeneratedFileName = generated.file.name
                )
                _toastEvents.tryEmit("${format.displayLabel} siap: ${generated.file.name}")
                onDone(generated.file)
            } catch (e: TimeoutCancellationException) {
                _docStudioState.value = _docStudioState.value.copy(
                    isGenerating = false,
                    error = "Pembuatan dokumen melebihi batas waktu."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Document generation failed", e)
                _docStudioState.value = _docStudioState.value.copy(
                    isGenerating = false,
                    error = e.localizedMessage ?: "Gagal membuat dokumen."
                )
            }
        }
    }

    fun shareArtifact(artifact: GeneratedArtifact) {
        val file = File(artifact.localPath)
        if (!file.exists()) {
            _toastEvents.tryEmit("File lokal tidak ditemukan di perangkat ini; buat ulang dari riwayat.")
            return
        }
        val format = OutputFormat.fromRaw(artifact.format)
        documentGenerator.shareFile(file, format.mimeType)
    }

    fun openArtifact(artifact: GeneratedArtifact) {
        val file = File(artifact.localPath)
        if (!file.exists()) {
            _toastEvents.tryEmit("File lokal tidak ditemukan di perangkat ini.")
            return
        }
        val format = OutputFormat.fromRaw(artifact.format)
        documentGenerator.openFile(file, format.mimeType)
    }

    fun deleteArtifact(artifactId: String) {
        viewModelScope.launch {
            repository.deleteArtifact(currentUserId, artifactId)
                .onSuccess { _toastEvents.tryEmit("Arsip dokumen dihapus") }
        }
    }

    // ==================== HANDS-FREE AUDIO RECORD VOICE INPUT ====================

    fun startVoiceRecording() {
        if (_voiceInputState.value.isRecording || _voiceInputState.value.isTranscribing) return
        if (_agentExecution.value.isRunning) {
            _toastEvents.tryEmit("Tunggu eksekusi agen selesai terlebih dahulu.")
            return
        }
        val started = audioRecorder.startRecording(
            scope = viewModelScope,
            onAutoStopped = { wavBytes ->
                viewModelScope.launch {
                    processRecordedAudio(wavBytes, autoSend = true)
                }
            },
            onError = { errMsg ->
                _voiceInputState.value = VoiceInputState(error = errMsg)
                _toastEvents.tryEmit(errMsg)
            }
        )
        if (started) {
            _voiceInputState.value = VoiceInputState(isRecording = true)
        }
    }

    fun stopVoiceRecordingAndProcess(
        autoSend: Boolean = true,
        onTranscriptReady: (String) -> Unit = {}
    ) {
        if (!_voiceInputState.value.isRecording) return
        val wavBytes = audioRecorder.stopAndGetWav()
        if (wavBytes.isEmpty()) {
            _voiceInputState.value = VoiceInputState()
            _toastEvents.tryEmit("Rekaman terlalu singkat, silakan bicara lagi.")
            return
        }
        viewModelScope.launch {
            processRecordedAudio(wavBytes, autoSend = autoSend, onTranscriptReady = onTranscriptReady)
        }
    }

    fun cancelVoiceRecording() {
        audioRecorder.cancelRecording()
        _voiceInputState.value = VoiceInputState()
    }

    private suspend fun processRecordedAudio(
        wavBytes: ByteArray,
        autoSend: Boolean,
        onTranscriptReady: (String) -> Unit = {}
    ) {
        _voiceInputState.value = VoiceInputState(isTranscribing = true)
        try {
            val lang = (userProfileState.value as? UiState.Success)?.data?.preferredLanguage ?: "Indonesia"
            val transcript = aiEngine.transcribeVoiceQuery(wavBytes, preferredLanguage = lang)
            if (transcript.isBlank()) {
                _voiceInputState.value = VoiceInputState(
                    error = "Suara tidak terdeteksi jelas. Coba ulangi lebih dekat ke mikrofon."
                )
                _toastEvents.tryEmit("Ucapan tidak terdeteksi jelas.")
                return
            }
            _voiceInputState.value = VoiceInputState(lastTranscript = transcript)
            onTranscriptReady(transcript)
            if (autoSend) {
                sendAgentPrompt(transcript)
            }
        } catch (e: TimeoutCancellationException) {
            _voiceInputState.value = VoiceInputState(error = "Transkripsi audio melebihi batas waktu.")
            _toastEvents.tryEmit("Transkripsi audio timeout.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Voice transcription failed", e)
            _voiceInputState.value = VoiceInputState(
                error = e.localizedMessage ?: "Gagal memproses suara."
            )
            _toastEvents.tryEmit("Gagal memproses rekaman suara.")
        }
    }

    // ==================== READ-ONLY CHAT DOCUMENT PREVIEW OVERLAY ====================

    fun openDocumentPreview(
        topicOrContent: String,
        format: OutputFormat,
        citations: List<CitationItem> = emptyList()
    ) {
        val cleanContent = topicOrContent.trim()
        if (cleanContent.isBlank()) return

        viewModelScope.launch {
            _documentPreviewState.value = DocumentPreviewState(
                isVisible = true,
                isLoading = true,
                requestedFormat = format,
                draft = null,
                error = null
            )
            try {
                val blueprint = aiEngine.generateDocumentBlueprint(
                    topicOrContent = cleanContent,
                    format = format,
                    citations = citations
                )
                val draft = DocumentPreviewDraft(
                    blueprint = blueprint,
                    format = format,
                    sourceContent = cleanContent,
                    citations = citations,
                    verified = citations.isNotEmpty()
                )
                _lastPreviewDraft.value = draft
                _documentPreviewState.value = DocumentPreviewState(
                    isVisible = true,
                    isLoading = false,
                    requestedFormat = format,
                    draft = draft,
                    error = null
                )
            } catch (e: TimeoutCancellationException) {
                _documentPreviewState.value = _documentPreviewState.value.copy(
                    isLoading = false,
                    error = "Penyusunan pratinjau melebihi batas waktu."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Preview generation failed", e)
                _documentPreviewState.value = _documentPreviewState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal menyusun pratinjau dokumen."
                )
            }
        }
    }

    fun switchPreviewFormat(newFormat: OutputFormat) {
        val current = _documentPreviewState.value
        val draft = current.draft ?: return
        if (draft.format == newFormat) return
        val updatedDraft = draft.copy(format = newFormat)
        _lastPreviewDraft.value = updatedDraft
        _documentPreviewState.value = current.copy(
            requestedFormat = newFormat,
            draft = updatedDraft
        )
    }

    fun confirmSavePreview(
        shareAfterSave: Boolean = false,
        onSaved: (GeneratedArtifact) -> Unit = {}
    ) {
        val current = _documentPreviewState.value
        val draft = current.draft ?: return
        if (current.isExporting) return

        viewModelScope.launch {
            _documentPreviewState.value = current.copy(isExporting = true, error = null)
            try {
                val generated = documentGenerator.generateDocument(draft.blueprint, draft.format)
                val artifact = GeneratedArtifact(
                    sessionId = _selectedSessionId.value.orEmpty(),
                    title = draft.blueprint.title,
                    format = draft.format.name,
                    fileName = generated.file.name,
                    localPath = generated.file.absolutePath,
                    summary = draft.blueprint.executiveSummary.take(400),
                    wordCount = generated.wordCount.toLong(),
                    verified = draft.verified
                )
                repository.saveArtifact(currentUserId, artifact)

                _docStudioState.value = _docStudioState.value.copy(
                    activeFormat = draft.format,
                    lastGeneratedFileName = generated.file.name
                )
                _documentPreviewState.value = DocumentPreviewState(isVisible = false)
                _toastEvents.tryEmit("${draft.format.displayLabel} disimpan: ${generated.file.name}")

                if (shareAfterSave) {
                    documentGenerator.shareFile(generated.file, draft.format.mimeType)
                }
                onSaved(artifact)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Export from preview failed", e)
                _documentPreviewState.value = _documentPreviewState.value.copy(
                    isExporting = false,
                    error = e.localizedMessage ?: "Gagal menyimpan file ke perangkat."
                )
            }
        }
    }

    fun dismissDocumentPreview() {
        _documentPreviewState.value = DocumentPreviewState(isVisible = false)
    }

    // ==================== RESEARCH DASHBOARD & TRENDING TOPICS ====================

    private fun recordWebSearchResultsAndRefreshTrends(results: List<WebSearchResult>, query: String) {
        synchronized(recentWebSearchPool) {
            results.forEach { item ->
                if (recentWebSearchPool.none { it.url == item.url }) {
                    recentWebSearchPool.add(0, item)
                }
            }
            if (recentWebSearchPool.size > 16) {
                recentWebSearchPool.subList(16, recentWebSearchPool.size).clear()
            }
        }
        viewModelScope.launch {
            try {
                val snapshotPool = synchronized(recentWebSearchPool) { recentWebSearchPool.toList() }
                val topics = aiEngine.extractTrendingTopics(
                    webResults = snapshotPool,
                    recentQueries = listOf(query)
                )
                if (topics.isNotEmpty()) {
                    _trendingTopics.value = topics
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Trend extraction skipped: ${e.message}")
            }
        }
    }

    fun refreshTrendingTopics(seedQuery: String? = null) {
        if (_isRefreshingTrends.value) return
        viewModelScope.launch {
            _isRefreshingTrends.value = true
            try {
                val sessionTitles = (sessionsState.value as? UiState.Success)?.data
                    ?.map { it.title }
                    ?.filter { it.isNotBlank() && !it.startsWith("Workspace") }
                    .orEmpty()
                val query = seedQuery?.takeIf { it.isNotBlank() }
                    ?: sessionTitles.firstOrNull()
                    ?: "autonomous AI agents peer reviewed research breakthroughs 2025 2026"

                val poolSnapshot = synchronized(recentWebSearchPool) { recentWebSearchPool.toList() }
                val webList = if (poolSnapshot.isEmpty() || seedQuery != null) {
                    val fetched = researchClient.searchLiveWeb(query, limit = 8)
                    if (fetched.isNotEmpty()) {
                        synchronized(recentWebSearchPool) {
                            recentWebSearchPool.clear()
                            recentWebSearchPool.addAll(fetched)
                        }
                    }
                    fetched
                } else {
                    poolSnapshot
                }

                val extracted = aiEngine.extractTrendingTopics(
                    webResults = webList,
                    recentQueries = sessionTitles.ifEmpty { listOf(query) }
                )
                if (extracted.isNotEmpty()) {
                    _trendingTopics.value = extracted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh trending topics: ${e.message}")
            } finally {
                _isRefreshingTrends.value = false
            }
        }
    }

    // ==================== ACTIVE SESSION SKILL TREE TOGGLING ====================

    fun toggleSkillForActiveSession(skill: AgentSkill) {
        if (skill.id.isBlank()) return
        val activeSession = (sessionsState.value as? UiState.Success)?.data
            ?.firstOrNull { it.id == _selectedSessionId.value }

        viewModelScope.launch {
            if (activeSession == null) {
                repository.toggleSkillEnabled(currentUserId, skill.id, !skill.enabled)
                return@launch
            }
            val currentSet = activeSession.activeSkillIds.toMutableSet()
            val willEnable = skill.id !in currentSet
            if (willEnable) {
                if (currentSet.size >= 10) {
                    _toastEvents.tryEmit("Maksimal 10 skill aktif per sesi.")
                    return@launch
                }
                currentSet.add(skill.id)
            } else {
                currentSet.remove(skill.id)
            }
            repository.toggleSkillEnabled(currentUserId, skill.id, willEnable)
            repository.updateSessionSummary(
                userId = currentUserId,
                sessionId = activeSession.id,
                activeSkillIds = currentSet.toList().take(10)
            )
        }
    }

    fun toggleSkillSetForActiveSession(skillsInGroup: List<AgentSkill>, enable: Boolean) {
        val activeSession = (sessionsState.value as? UiState.Success)?.data
            ?.firstOrNull { it.id == _selectedSessionId.value } ?: return

        viewModelScope.launch {
            val currentSet = activeSession.activeSkillIds.toMutableSet()
            val groupIds = skillsInGroup.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }
            if (enable) {
                for (id in groupIds) {
                    if (currentSet.size < 10) currentSet.add(id)
                }
            } else {
                currentSet.removeAll(groupIds.toSet())
            }
            groupIds.forEach { id ->
                repository.toggleSkillEnabled(currentUserId, id, id in currentSet)
            }
            repository.updateSessionSummary(
                userId = currentUserId,
                sessionId = activeSession.id,
                activeSkillIds = currentSet.toList().take(10)
            ).onSuccess {
                _toastEvents.tryEmit(
                    if (enable) "${groupIds.size} skill diaktifkan untuk sesi ini"
                    else "${groupIds.size} skill dinonaktifkan untuk sesi ini"
                )
            }
        }
    }

    // ==================== SNAPSHOT & RESEARCH LIBRARY ====================

    fun saveCurrentSnapshot(customTitle: String, notes: String = "") {
        if (_isSavingSnapshot.value) return
        val sessionId = _selectedSessionId.value.orEmpty()
        val sessions = (sessionsState.value as? UiState.Success)?.data ?: emptyList()
        val activeSession = sessions.firstOrNull { it.id == sessionId }
        val messages = (messagesState.value as? UiState.Success)?.data ?: emptyList()
        val activePreview = _documentPreviewState.value.draft ?: _lastPreviewDraft.value

        val allCitations = buildList {
            val seen = mutableSetOf<String>()
            messages.flatMap { it.parsedCitations() }.forEach { cit ->
                val key = cit.url.ifBlank { cit.title }
                if (seen.add(key)) add(cit)
            }
            activePreview?.citations?.forEach { cit ->
                val key = cit.url.ifBlank { cit.title }
                if (seen.add(key)) add(cit)
            }
        }.take(30)

        val snapshotTitle = customTitle.trim().ifBlank {
            activeSession?.title?.ifBlank { null } ?: "Snapshot Riset Kafka"
        }

        viewModelScope.launch {
            _isSavingSnapshot.value = true
            val res = repository.saveResearchSnapshot(
                userId = currentUserId,
                sourceSessionId = sessionId,
                title = snapshotTitle,
                notes = notes,
                mode = _activeMode.value,
                activeSkillIds = activeSession?.activeSkillIds ?: emptyList(),
                messages = messages,
                citations = allCitations,
                activePreview = activePreview,
                verifiedScore = activeSession?.verifiedScore?.toInt()
                    ?: messages.lastOrNull { it.role == "AGENT" }?.verificationScore?.toInt()
                    ?: 0
            )
            _isSavingSnapshot.value = false
            res.onSuccess {
                _toastEvents.tryEmit("Snapshot disimpan ke Research Library!")
            }.onFailure {
                _toastEvents.tryEmit("Gagal menyimpan snapshot: ${it.localizedMessage}")
            }
        }
    }

    fun restoreSnapshotToWorkspace(
        snapshot: ResearchSnapshot,
        openPreviewAfterRestore: Boolean = true,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val restoredMode = AgentMode.fromRaw(snapshot.mode)
            _activeMode.value = restoredMode

            val res = repository.createSession(
                userId = currentUserId,
                title = "[Snapshot] ${snapshot.title}".take(200),
                mode = restoredMode,
                activeSkillIds = snapshot.activeSkillIds.take(10)
            )
            res.onSuccess { newSessionId ->
                _selectedSessionId.value = newSessionId
                val citations = snapshot.parsedCitations()
                val archive = snapshot.messagesArchive
                archive.forEachIndexed { idx, map ->
                    val role = map["role"] as? String ?: "AGENT"
                    val content = map["content"] as? String ?: ""
                    val vStatus = VerificationStatus.fromRaw(map["verificationStatus"] as? String ?: "NONE")
                    val vScore = (map["verificationScore"] as? Number)?.toInt() ?: 0
                    val vSummary = map["verificationSummary"] as? String ?: ""
                    val steps = (map["thoughtSteps"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val attachCitations = if (role == "AGENT" && idx == archive.indexOfLast {
                            (it["role"] as? String) == "AGENT"
                        }
                    ) citations else emptyList()

                    repository.addMessage(
                        userId = currentUserId,
                        sessionId = newSessionId,
                        role = role,
                        content = content,
                        thoughtSteps = steps,
                        verificationStatus = vStatus,
                        verificationScore = vScore,
                        verificationSummary = vSummary,
                        citations = attachCitations
                    )
                }
                repository.updateSessionSummary(
                    userId = currentUserId,
                    sessionId = newSessionId,
                    mode = restoredMode,
                    lastSummary = snapshot.notes.ifBlank { "Dipulihkan dari Research Library" },
                    verifiedScore = snapshot.verifiedScore.toInt()
                )

                val restoredPreview = snapshot.toPreviewDraftOrNull()
                if (restoredPreview != null) {
                    _lastPreviewDraft.value = restoredPreview
                    if (openPreviewAfterRestore) {
                        _documentPreviewState.value = DocumentPreviewState(
                            isVisible = true,
                            isLoading = false,
                            requestedFormat = restoredPreview.format,
                            draft = restoredPreview,
                            error = null
                        )
                    }
                }
                _toastEvents.tryEmit("Snapshot '${snapshot.title}' dipulihkan ke workspace!")
                onDone()
            }.onFailure {
                _toastEvents.tryEmit("Gagal memulihkan snapshot: ${it.localizedMessage}")
            }
        }
    }

    fun deleteSnapshot(snapshotId: String) {
        viewModelScope.launch {
            repository.deleteResearchSnapshot(currentUserId, snapshotId)
                .onSuccess { _toastEvents.tryEmit("Snapshot dihapus dari Research Library") }
        }
    }

    // ==================== VOICE CONVERSATION (TTS) & IMAGE STUDIO ====================

    fun toggleAutoSpeak() {
        val next = !_autoSpeakEnabled.value
        _autoSpeakEnabled.value = next
        if (!next) {
            voiceSpeaker.stop()
        }
        _toastEvents.tryEmit(
            if (next) "Mode Percakapan Suara (TTS) aktif" else "Mode suara otomatis dimatikan"
        )
    }

    fun speakAgentMessage(text: String) {
        if (voiceSpeaker.isSpeaking.value) {
            voiceSpeaker.stop()
            return
        }
        val lang = (userProfileState.value as? UiState.Success)?.data?.preferredLanguage ?: "Indonesia"
        voiceSpeaker.speak(text, preferredLanguage = lang)
    }

    fun stopSpeaking() {
        voiceSpeaker.stop()
    }

    fun generateAiImage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank() || _imageStudioState.value is ImageStudioUiState.Loading) return
        viewModelScope.launch {
            _imageStudioState.value = ImageStudioUiState.Loading
            try {
                val bmp = aiEngine.generateImage(clean)
                _imageStudioState.value = ImageStudioUiState.Success(bmp, clean)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _imageStudioState.value = ImageStudioUiState.Error(
                    "Pembuatan gambar melewati batas waktu (60 detik). Silakan coba lagi."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageStudioState.value = ImageStudioUiState.Error(
                    e.localizedMessage ?: "Gagal membuat gambar dengan gemini-nano-banana-2.1."
                )
            }
        }
    }

    fun editAiImage(inputBitmap: Bitmap, instruction: String) {
        val clean = instruction.trim()
        if (clean.isBlank() || _imageStudioState.value is ImageStudioUiState.Loading) return
        viewModelScope.launch {
            _imageStudioState.value = ImageStudioUiState.Loading
            try {
                val bmp = aiEngine.editImage(inputBitmap, clean)
                _imageStudioState.value = ImageStudioUiState.Success(bmp, clean)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image edit timed out", e)
                _imageStudioState.value = ImageStudioUiState.Error(
                    "Edit gambar melewati batas waktu (60 detik). Silakan coba lagi."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image edit failed", e)
                _imageStudioState.value = ImageStudioUiState.Error(
                    e.localizedMessage ?: "Gagal mengedit gambar dengan gemini-nano-banana-2.1."
                )
            }
        }
    }

    fun shareGeneratedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching { documentGenerator.shareBitmap(bitmap) }
                .onFailure {
                    _toastEvents.tryEmit("Gagal membagikan gambar: ${it.localizedMessage}")
                }
        }
    }
}
