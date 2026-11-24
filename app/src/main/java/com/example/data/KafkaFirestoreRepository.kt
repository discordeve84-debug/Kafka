package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.AgentMessage
import com.example.model.AgentMode
import com.example.model.AgentSession
import com.example.model.AgentSkill
import com.example.model.CitationItem
import com.example.model.DocumentPreviewDraft
import com.example.model.GeneratedArtifact
import com.example.model.OutputFormat
import com.example.model.ResearchSnapshot
import com.example.model.SkillCategory
import com.example.model.SkillSourceType
import com.example.model.UserProfile
import com.example.model.VerificationStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KafkaFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
        check(uid == expectedUserId) { "Akun berubah; akses lintas pengguna ditolak." }
        return uid
    }

    // ==================== SESSIONS ====================

    fun observeSessions(userId: String): Flow<List<AgentSession>> = flow {
        requireAuthorizedUserId(userId)
        val path = "users/$userId/sessions"
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AgentSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSession(
        userId: String,
        title: String,
        mode: AgentMode,
        activeSkillIds: List<String> = emptyList()
    ): Result<String> {
        var docPath = "users/$userId/sessions"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val safeTitle = title.trim().take(200).ifBlank { "Sesi Kafka Baru" }
            val ref = db.collection("users").document(uid).collection("sessions").document()
            docPath = ref.path
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "mode" to mode.name,
                "activeSkillIds" to activeSkillIds.take(10),
                "lastSummary" to "Siap melakukan riset langsung & verifikasi.",
                "verifiedScore" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSessionSummary(
        userId: String,
        sessionId: String,
        title: String? = null,
        mode: AgentMode? = null,
        activeSkillIds: List<String>? = null,
        lastSummary: String? = null,
        verifiedScore: Int? = null
    ): Result<Unit> {
        val path = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(sessionId.isNotBlank()) { "Session ID kosong" }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.let { updates["title"] = it.trim().take(200).ifBlank { "Sesi Kafka" } }
            mode?.let { updates["mode"] = it.name }
            activeSkillIds?.let { updates["activeSkillIds"] = it.take(10) }
            lastSummary?.let { updates["lastSummary"] = it.take(2000) }
            verifiedScore?.let { updates["verifiedScore"] = it.coerceIn(0, 100).toLong() }

            db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
        val path = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== MESSAGES ====================

    fun observeMessages(userId: String, sessionId: String): Flow<List<AgentMessage>> = flow {
        requireAuthorizedUserId(userId)
        require(sessionId.isNotBlank()) { "Session ID tidak valid" }
        val path = "users/$userId/sessions/$sessionId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("sessions").document(sessionId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AgentMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        sessionId: String,
        role: String,
        content: String,
        thoughtSteps: List<String> = emptyList(),
        verificationStatus: VerificationStatus = VerificationStatus.NONE,
        verificationScore: Int = 0,
        verificationSummary: String = "",
        citations: List<CitationItem> = emptyList()
    ): Result<String> {
        var path = "users/$userId/sessions/$sessionId/messages"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val ref = db.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .collection("messages").document()
            path = ref.path
            val citationMaps = citations.take(15).map { c ->
                mapOf(
                    "refTag" to c.refTag.take(16),
                    "title" to c.title.take(180),
                    "url" to c.url.take(400),
                    "source" to c.source.take(100),
                    "snippet" to c.snippet.take(400),
                    "verified" to c.verified,
                    "sourceType" to c.sourceType.take(16),
                    "authorsText" to c.authorsText.take(200),
                    "year" to c.year.toLong(),
                    "venue" to c.venue.take(150),
                    "doi" to c.doi.take(150),
                    "citedByCount" to c.citedByCount.toLong(),
                    "provider" to c.provider.take(60)
                )
            }
            val payload = mapOf(
                "userId" to uid,
                "role" to if (role == "USER") "USER" else "AGENT",
                "content" to content.trim().take(29000).ifBlank { "..." },
                "thoughtSteps" to thoughtSteps.take(20).map { it.take(300) },
                "verificationStatus" to verificationStatus.name,
                "verificationScore" to verificationScore.coerceIn(0, 100).toLong(),
                "verificationSummary" to verificationSummary.take(1900),
                "citations" to citationMaps,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ==================== SKILLS ====================

    fun observeSkills(userId: String): Flow<List<AgentSkill>> = flow {
        requireAuthorizedUserId(userId)
        val path = "users/$userId/skills"
        emitAll(
            db.collection("users").document(userId).collection("skills")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AgentSkill::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveSkill(userId: String, skill: AgentSkill, customDocId: String? = null): Result<String> {
        var path = "users/$userId/skills"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val col = db.collection("users").document(uid).collection("skills")
            val ref = if (!customDocId.isNullOrBlank()) col.document(customDocId) else col.document()
            path = ref.path
            val payload = mapOf(
                "userId" to uid,
                "name" to skill.name.trim().take(100).ifBlank { "Skill Kustom" },
                "slug" to skill.slug.trim().take(80).ifBlank { "custom-skill" },
                "description" to skill.description.trim().take(1000).ifBlank { "Skill agen Kafka" },
                "category" to SkillCategory.fromRaw(skill.category).name,
                "sourceType" to SkillSourceType.fromRaw(skill.sourceType).name,
                "githubRepo" to skill.githubRepo?.trim()?.take(200)?.takeIf { it.isNotBlank() },
                "githubUrl" to skill.githubUrl?.trim()?.take(500)?.takeIf { it.isNotBlank() },
                "systemPrompt" to skill.systemPrompt.trim().take(8000).ifBlank { "Jalankan tugas secara akurat." },
                "outputFormat" to OutputFormat.fromRaw(skill.outputFormat).name,
                "enabled" to skill.enabled,
                "usageCount" to skill.usageCount.coerceIn(0L, 100000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null } as Map<String, Any>
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSkillEnabled(userId: String, skillId: String, enabled: Boolean): Result<Unit> {
        val path = "users/$userId/skills/$skillId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("skills").document(skillId)
                .update(
                    mapOf(
                        "enabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementSkillUsage(userId: String, skillId: String): Result<Unit> {
        val path = "users/$userId/skills/$skillId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("skills").document(skillId)
                .update(
                    mapOf(
                        "usageCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSkill(userId: String, skillId: String): Result<Unit> {
        val path = "users/$userId/skills/$skillId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("skills").document(skillId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== ARTIFACTS ====================

    fun observeArtifacts(userId: String): Flow<List<GeneratedArtifact>> = flow {
        requireAuthorizedUserId(userId)
        val path = "users/$userId/artifacts"
        emitAll(
            db.collection("users").document(userId).collection("artifacts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        GeneratedArtifact::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveArtifact(userId: String, artifact: GeneratedArtifact): Result<String> {
        var path = "users/$userId/artifacts"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val ref = db.collection("users").document(uid).collection("artifacts").document()
            path = ref.path
            val payload = mapOf(
                "userId" to uid,
                "sessionId" to artifact.sessionId.take(128),
                "title" to artifact.title.trim().take(200).ifBlank { "Dokumen Kafka" },
                "format" to OutputFormat.fromRaw(artifact.format).name,
                "fileName" to artifact.fileName.trim().take(150).ifBlank { "dokumen.pdf" },
                "localPath" to artifact.localPath.take(500),
                "summary" to artifact.summary.take(2000),
                "wordCount" to artifact.wordCount.coerceIn(0L, 1000000L),
                "verified" to artifact.verified,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteArtifact(userId: String, artifactId: String): Result<Unit> {
        val path = "users/$userId/artifacts/$artifactId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("artifacts").document(artifactId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== USER PROFILE & PREFERENCES ====================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        requireAuthorizedUserId(userId)
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().take(100).ifBlank { "Peneliti Kafka" }
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(200),
                    "bio" to "Peneliti & Analis Dokumen Kafka",
                    "darkTheme" to true,
                    "defaultFormat" to OutputFormat.DOCX.name,
                    "autoVerify" to true,
                    "preferredLanguage" to "Indonesia",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        bio: String,
        darkTheme: Boolean,
        defaultFormat: OutputFormat,
        autoVerify: Boolean,
        preferredLanguage: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val safeLang = if (preferredLanguage == "English") "English" else "Indonesia"
            val safeFormat = if (defaultFormat in OutputFormat.exportableFormats) {
                defaultFormat.name
            } else OutputFormat.DOCX.name
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "displayName" to displayName.trim().take(100).ifBlank { "Peneliti Kafka" },
                        "bio" to bio.trim().take(300),
                        "darkTheme" to darkTheme,
                        "defaultFormat" to safeFormat,
                        "autoVerify" to autoVerify,
                        "preferredLanguage" to safeLang,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==================== RESEARCH LIBRARY (SNAPSHOTS) ====================

    fun observeResearchLibrary(userId: String): Flow<List<ResearchSnapshot>> = flow {
        requireAuthorizedUserId(userId)
        val path = "users/$userId/researchLibrary"
        emitAll(
            db.collection("users").document(userId).collection("researchLibrary")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ResearchSnapshot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveResearchSnapshot(
        userId: String,
        sourceSessionId: String,
        title: String,
        notes: String,
        mode: AgentMode,
        activeSkillIds: List<String>,
        messages: List<AgentMessage>,
        citations: List<CitationItem>,
        activePreview: DocumentPreviewDraft?,
        verifiedScore: Int
    ): Result<String> {
        var path = "users/$userId/researchLibrary"
        return try {
            val uid = requireAuthorizedUserId(userId)
            val ref = db.collection("users").document(uid).collection("researchLibrary").document()
            path = ref.path

            val archivedMessages = messages.takeLast(20).map { m ->
                mapOf(
                    "role" to m.role.take(10),
                    "content" to m.content.take(4000),
                    "verificationStatus" to m.verificationStatus.take(24),
                    "verificationScore" to m.verificationScore.coerceIn(0L, 100L),
                    "verificationSummary" to m.verificationSummary.take(400),
                    "thoughtSteps" to m.thoughtSteps.take(8).map { it.take(180) }
                )
            }

            val citationMaps = citations.take(30).map { c ->
                mapOf(
                    "refTag" to c.refTag.take(16),
                    "title" to c.title.take(180),
                    "url" to c.url.take(400),
                    "source" to c.source.take(100),
                    "snippet" to c.snippet.take(360),
                    "verified" to c.verified,
                    "sourceType" to c.sourceType.take(16),
                    "authorsText" to c.authorsText.take(200),
                    "year" to c.year.toLong(),
                    "venue" to c.venue.take(150),
                    "doi" to c.doi.take(150),
                    "citedByCount" to c.citedByCount.toLong(),
                    "provider" to c.provider.take(60)
                )
            }

            val previewSectionsMaps = activePreview?.blueprint?.sections?.take(15)?.map { sec ->
                mapOf(
                    "heading" to sec.heading.take(180),
                    "body" to sec.body.take(1500),
                    "bullets" to sec.bullets.take(8).map { it.take(300) }
                )
            } ?: emptyList()

            val payload = mapOf(
                "userId" to uid,
                "sourceSessionId" to sourceSessionId.take(128),
                "title" to title.trim().take(200).ifBlank { "Snapshot Riset Kafka" },
                "notes" to notes.trim().take(1000),
                "mode" to mode.name,
                "activeSkillIds" to activeSkillIds.take(15),
                "messageCount" to messages.size.toLong().coerceIn(0L, 500L),
                "messagesArchive" to archivedMessages,
                "citations" to citationMaps,
                "hasActivePreview" to (activePreview != null),
                "previewFormat" to (activePreview?.format?.name ?: ""),
                "previewTitle" to (activePreview?.blueprint?.title?.take(200) ?: ""),
                "previewSubtitle" to (activePreview?.blueprint?.subtitle?.take(300) ?: ""),
                "previewSummary" to (activePreview?.blueprint?.executiveSummary?.take(2000) ?: ""),
                "previewSections" to previewSectionsMaps,
                "previewReferences" to (activePreview?.blueprint?.references?.take(20)?.map { it.take(300) } ?: emptyList()),
                "verifiedScore" to verifiedScore.coerceIn(0, 100).toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteResearchSnapshot(userId: String, snapshotId: String): Result<Unit> {
        val path = "users/$userId/researchLibrary/$snapshotId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("researchLibrary").document(snapshotId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==================== INITIAL BOOTSTRAP ====================

    suspend fun ensureDefaultSkillsAndSession(userId: String) {
        try {
            val uid = requireAuthorizedUserId(userId)
            val skillsSnap = db.collection("users").document(uid).collection("skills").get().await()
            val existingIds = skillsSnap.documents.map { it.id }.toSet()

            val defaults = listOf(
                "builtin_docx" to AgentSkill(
                    name = "Word (.docx) Architect",
                    slug = "docx-architect",
                    description = "Menyusun laporan formal, makalah, atau proposal lengkap dan mengekspornya ke dokumen Microsoft Word (.docx).",
                    category = SkillCategory.DOCUMENT.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Susun dokumen terstruktur dengan Ringkasan Eksekutif, bagian bernomor, poin kunci, dan daftar pustaka terverifikasi siap ekspor DOCX.",
                    outputFormat = OutputFormat.DOCX.name,
                    enabled = true
                ),
                "builtin_pdf" to AgentSkill(
                    name = "PDF Dossier Publisher",
                    slug = "pdf-dossier",
                    description = "Mengkompilasi riset dan verifikasi menjadi dossier PDF multi-halaman berstandar publikasi.",
                    category = SkillCategory.DOCUMENT.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Rancang laporan analitik ringkas dengan temuan utama, data pendukung, dan referensi DOI yang siap dicetak sebagai PDF.",
                    outputFormat = OutputFormat.PDF.name,
                    enabled = true
                ),
                "builtin_pptx" to AgentSkill(
                    name = "PowerPoint (.pptx) Deck Designer",
                    slug = "pptx-deck",
                    description = "Mengubah topik atau hasil riset menjadi slide presentasi PowerPoint (.pptx) yang padat dan eksekutif.",
                    category = SkillCategory.DOCUMENT.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Pecah materi menjadi slide presentasi berfokus insight: judul kuat, 3-5 poin ringkas per slide, dan slide kesimpulan berbasis bukti.",
                    outputFormat = OutputFormat.PPTX.name,
                    enabled = true
                ),
                "builtin_journal" to AgentSkill(
                    name = "Academic Journal Deep-Researcher",
                    slug = "journal-researcher",
                    description = "Menelusuri OpenAlex, arXiv, dan Crossref untuk literatur ilmiah peer-reviewed beserta DOI.",
                    category = SkillCategory.RESEARCH.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Prioritaskan jurnal peer-reviewed, kutip nama penulis, tahun, penerbit, jumlah sitasi, dan tautan DOI yang ditemukan secara langsung.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_verify" to AgentSkill(
                    name = "Hermes Claim & Fact Verifier",
                    slug = "claim-verifier",
                    description = "Mengaudit klaim faktual terhadap sumber web langsung dan literatur ilmiah untuk mendeteksi halusinasi.",
                    category = SkillCategory.VERIFICATION.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Pisahkan fakta yang didukung bukti langsung dari klaim yang belum terbukti; tampilkan skor kepercayaan dan sumber pembanding.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_web_search" to AgentSkill(
                    name = "Live Web & Search Grounding",
                    slug = "live-web-search",
                    description = "Menelusuri web langsung (DuckDuckGo, Wikipedia, Algolia) dan memeriksa HTTP reachability setiap tautan.",
                    category = SkillCategory.RESEARCH.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Selalu landaskan jawaban pada hasil web terkini yang terverifikasi dan sertakan tag sitasi [W1], [W2].",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_chatbot" to AgentSkill(
                    name = "Gemini Multi-Model Chatbot",
                    slug = "gemini-chatbot",
                    description = "Agen percakapan multi-turn berkecepatan tinggi dengan Gemini 3.8 Flash & fallback otomatis Flash-Lite.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Pertahankan konteks percakapan multi-turn secara konsisten, terstruktur, dan responsif.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_transcribe" to AgentSkill(
                    name = "Voice & Audio Transcriber",
                    slug = "audio-transcriber",
                    description = "Merekam suara via Android AudioRecord 16kHz dan mentranskripsi langsung menjadi kueri riset via Gemini.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Ubah input suara pengguna menjadi kueri riset bersih dalam Bahasa Indonesia atau Inggris.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_voice_chat" to AgentSkill(
                    name = "Voice Conversation (Mic + TTS)",
                    slug = "voice-conversation",
                    description = "Percakapan suara dua arah hands-free: rekam pertanyaan via mikrofon dan dengar jawaban Kafka dengan Android TextToSpeech.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Berikan ringkasan pembuka yang lugas dan mudah dibacakan dengan suara dalam percakapan hands-free.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_image_studio" to AgentSkill(
                    name = "Create & Edit Image (Nano Banana 2.1)",
                    slug = "nano-banana-image-studio",
                    description = "Membuat gambar dari teks dan mengedit foto galeri menggunakan model Firebase AI Logic gemini-nano-banana-2.1.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Bantu pengguna merancang prompt visual detail atau instruksi edit foto untuk Studio Gambar Nano Banana 2.1.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = false
                ),
                "builtin_game_2d" to AgentSkill(
                    name = "Interactive 2D Cyber-Grid Game",
                    slug = "cyber-grid-2d-game",
                    description = "Mini-game puzzle 2D Cyber Matrix 2048 interaktif berbasis Jetpack Compose langsung di dalam Kafka.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Buka atau pandu strategi permainan puzzle angka Cyber Matrix 2048 bagi pengguna.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = false
                ),
                "builtin_maps_place" to AgentSkill(
                    name = "Location & Maps Link Explorer",
                    slug = "maps-place-explorer",
                    description = "Meriset lokasi, koordinat, dan menyertakan tautan pencarian Google Maps yang dapat langsung dibuka.",
                    category = SkillCategory.RESEARCH.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Saat membahas lokasi geografis, tampilkan informasi tempat terverifikasi dan sertakan URL https://www.google.com/maps/search/?api=1&query=<NAMA_TEMPAT>.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = false
                ),
                "builtin_cloud_sync" to AgentSkill(
                    name = "Cloud Firestore & Google Auth",
                    slug = "cloud-firestore-sync",
                    description = "Sinkronisasi otomatis sesi riset, histori dokumen, skill, dan Research Library ke Cloud Firestore per akun Google.",
                    category = SkillCategory.CUSTOM.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Pastikan seluruh artefak riset, sitasi, dan snapshot siap diarsipkan ke Research Library cloud.",
                    outputFormat = OutputFormat.JSON.name,
                    enabled = true
                ),
                "builtin_csv" to AgentSkill(
                    name = "Structured Data & CSV Synthesizer",
                    slug = "csv-synthesizer",
                    description = "Mengekstrak temuan riset dan perbandingan literatur ke tabel terstruktur CSV/Markdown.",
                    category = SkillCategory.DOCUMENT.name,
                    sourceType = SkillSourceType.BUILTIN.name,
                    systemPrompt = "Sajikan perbandingan komparatif dalam format kolom terstruktur yang mudah diekspor ke CSV.",
                    outputFormat = OutputFormat.CSV.name,
                    enabled = false
                ),
                "builtin_agency_researcher" to AgentSkill(
                    name = "Agency Persona: Research & Fact Auditor",
                    slug = "agency-research-auditor",
                    description = "Adaptasi persona spesialis dari msitarzewski/agency-agents untuk riset mendalam, audit DOI, dan QA verifikasi.",
                    category = SkillCategory.VERIFICATION.name,
                    sourceType = SkillSourceType.GITHUB.name,
                    githubRepo = "msitarzewski/agency-agents",
                    githubUrl = "https://github.com/msitarzewski/agency-agents",
                    systemPrompt = "Bertindak sebagai Senior Research & QA Auditor: pisahkan klaim berbasis peer-review (OpenAlex/DOAJ/Europe PMC) dari preprint (arXiv), periksa validitas tautan, dan nyatakan batasan bukti secara transparan.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_agency_android" to AgentSkill(
                    name = "Agency Persona: Android Kotlin Architect",
                    slug = "agency-android-architect",
                    description = "Skill engineering Kotlin/Jetpack Compose dari GuillemRoca/agent-skills-android, nowinandroid & compose-samples.",
                    category = SkillCategory.GITHUB.name,
                    sourceType = SkillSourceType.GITHUB.name,
                    githubRepo = "GuillemRoca/agent-skills-android",
                    githubUrl = "https://github.com/GuillemRoca/agent-skills-android",
                    systemPrompt = "Gunakan praktik terbaik arsitektur Android modern (Kotlin Coroutines, Flow, Jetpack Compose Material 3, MVVM, Clean Architecture, dan testing Robolectric/Maestro).",
                    outputFormat = OutputFormat.MARKDOWN.name,
                    enabled = false
                ),
                "builtin_github_papers" to AgentSkill(
                    name = "GitHub Open-Access Journal & Paper Hunter",
                    slug = "github-journal-hunter",
                    description = "Menjelajahi koleksi jurnal/paper open-access dari GitHub (papers-we-love, awesome-papers), DOAJ, Europe PMC, OpenAlex & arXiv PDF.",
                    category = SkillCategory.RESEARCH.name,
                    sourceType = SkillSourceType.GITHUB.name,
                    githubRepo = "papers-we-love/papers-we-love",
                    githubUrl = "https://github.com/papers-we-love/papers-we-love",
                    systemPrompt = "Prioritaskan paper dan jurnal yang memiliki tautan open-access atau PDF langsung (arXiv, Europe PMC, DOAJ, OpenAlex OA, atau repositori GitHub curated papers) sehingga pengguna dapat langsung membuka naskahnya.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = true
                ),
                "builtin_mcp_bridge" to AgentSkill(
                    name = "MCP Fetch & Memory Orchestrator",
                    slug = "mcp-fetch-orchestrator",
                    description = "Pola orkestrasi Fetch, Memory & Git workflow terinspirasi modelcontextprotocol/servers untuk riset multi-langkah.",
                    category = SkillCategory.GITHUB.name,
                    sourceType = SkillSourceType.GITHUB.name,
                    githubRepo = "modelcontextprotocol/servers",
                    githubUrl = "https://github.com/modelcontextprotocol/servers",
                    systemPrompt = "Jalankan alur tool terstruktur: Fetch sumber web langsung -> Verifikasi HTTP & DOI -> Simpan ringkasan terindeks ke memori sesi/Research Library.",
                    outputFormat = OutputFormat.RESEARCH.name,
                    enabled = false
                ),
                "builtin_firebase_ai_pack" to AgentSkill(
                    name = "Firebase & Gemini Android Samples",
                    slug = "firebase-gemini-samples",
                    description = "Referensi pola integrasi resmi firebase/quickstart-android, android/ai-samples, dan google/skills.",
                    category = SkillCategory.GITHUB.name,
                    sourceType = SkillSourceType.GITHUB.name,
                    githubRepo = "android/ai-samples",
                    githubUrl = "https://github.com/android/ai-samples",
                    systemPrompt = "Terapkan pola resmi Firebase AI Logic, App Check, Firestore security rules, dan multimodal Gemini Android samples.",
                    outputFormat = OutputFormat.MARKDOWN.name,
                    enabled = false
                ),
            )

            defaults.forEach { (id, skill) ->
                if (id !in existingIds) {
                    saveSkill(uid, skill, customDocId = id)
                }
            }

            val sessSnap = db.collection("users").document(uid).collection("sessions").limit(1).get().await()
            if (sessSnap.isEmpty) {
                createSession(
                    userId = uid,
                    title = "Workspace Riset Kafka",
                    mode = AgentMode.AGENT,
                    activeSkillIds = listOf(
                        "builtin_journal",
                        "builtin_verify",
                        "builtin_docx",
                        "builtin_web_search",
                        "builtin_chatbot"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
        }
    }

    companion object {
        fun Timestamp?.toEpochMillis(): Long = this?.toDate()?.time ?: 0L
    }
}
