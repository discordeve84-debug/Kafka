package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AgentMode
import com.example.model.AgentSkill
import com.example.model.CitationItem
import com.example.model.ClaimCheck
import com.example.model.DocumentBlueprint
import com.example.model.DocumentSection
import com.example.model.GitHubSkillRepo
import com.example.model.JournalPaper
import com.example.model.JournalSummaryVerification
import com.example.model.OutputFormat
import com.example.model.SkillCategory
import com.example.model.SkillSourceType
import com.example.model.TrendingResearchTopic
import com.example.model.VerificationReport
import com.example.model.VerificationStatus
import com.example.model.WebSearchResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "KafkaAiEngine"
private const val MODEL_PRIMARY = "gemini-3.8-flash"
private const val MODEL_FAST = "gemini-3.5-flash-lite"
private const val MODEL_IMAGE = "gemini-nano-banana-2.1"

class KafkaAiEngine {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(MODEL_PRIMARY)
    }

    private val fastChatModel by lazy {
        Firebase.ai.generativeModel(MODEL_FAST)
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val verificationSchema = Schema.obj(
        properties = mapOf(
            "overallScore" to Schema.integer(description = "Verification confidence score 0 to 100"),
            "status" to Schema.enumeration(
                values = listOf("VERIFIED", "PARTIAL", "UNVERIFIED", "NONE"),
                description = "Overall claim verification verdict"
            ),
            "summary" to Schema.string(description = "Concise Indonesian summary of verification audit"),
            "claims" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "claim" to Schema.string(description = "Extracted factual claim"),
                        "verdict" to Schema.enumeration(
                            values = listOf("SUPPORTED", "PARTIAL", "CONTRADICTED", "UNVERIFIED")
                        ),
                        "explanation" to Schema.string(description = "Short explanation referencing retrieved evidence"),
                        "evidenceSource" to Schema.string(description = "Name of supporting source or journal"),
                        "evidenceUrl" to Schema.string(description = "URL or DOI from retrieved sources, or empty")
                    )
                ),
                description = "Up to 6 checked claims"
            )
        )
    )

    private val verificationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = verificationSchema
            }
        )
    }

    private val skillSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Short professional skill name"),
            "slug" to Schema.string(description = "Lowercase kebab-case slug"),
            "description" to Schema.string(description = "Clear Indonesian description of what the skill does"),
            "category" to Schema.enumeration(
                values = listOf("DOCUMENT", "RESEARCH", "VERIFICATION", "GITHUB", "CUSTOM")
            ),
            "outputFormat" to Schema.enumeration(
                values = listOf("DOCX", "PDF", "PPTX", "MARKDOWN", "CSV", "JSON", "RESEARCH")
            ),
            "systemPrompt" to Schema.string(description = "Detailed executable instructions for the Kafka AI agent")
        )
    )

    private val skillArchitectModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_PRIMARY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = skillSchema
            }
        )
    }

    private val fastSkillArchitectModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = skillSchema
            }
        )
    }

    private val blueprintSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Document title"),
            "subtitle" to Schema.string(description = "Subtitle or author tagline"),
            "executiveSummary" to Schema.string(description = "Executive summary paragraph"),
            "sections" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "heading" to Schema.string(description = "Section or slide title"),
                        "body" to Schema.string(description = "Well-written paragraph for this section"),
                        "bullets" to Schema.array(
                            items = Schema.string(),
                            description = "3 to 5 concise bullet points"
                        )
                    )
                ),
                description = "4 to 7 structured sections or presentation slides"
            ),
            "references" to Schema.array(
                items = Schema.string(),
                description = "Cited sources and DOIs"
            )
        )
    )

    private val blueprintModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_PRIMARY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = blueprintSchema
            }
        )
    }

    private val fastBlueprintModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = blueprintSchema
            }
        )
    }

    private val journalSummarySchema = Schema.obj(
        properties = mapOf(
            "keyFindings" to Schema.string(description = "Concise overview of the paper's key findings"),
            "methodology" to Schema.string(description = "Concise overview of research methodology, dataset, or experimental design"),
            "limitations" to Schema.string(description = "Key limitations or scope notes"),
            "accuracyScore" to Schema.integer(description = "Verification confidence 0 to 100"),
            "accuracyStatus" to Schema.enumeration(
                values = listOf("VERIFIED", "PARTIAL", "UNVERIFIED", "NONE"),
                description = "Accuracy status cross-checked against retrieved sources and scientific consensus"
            ),
            "accuracyNotes" to Schema.string(description = "Short explanation of cross-source accuracy verification")
        )
    )

    private val journalSummaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = journalSummarySchema
            }
        )
    }

    private val trendingTopicsSchema = Schema.obj(
        properties = mapOf(
            "topics" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "topic" to Schema.string(description = "Short trending research topic title"),
                        "category" to Schema.string(description = "Domain category e.g. AI Agents, Biotech, Energy, Policy"),
                        "momentumLabel" to Schema.string(description = "Short signal e.g. Rising, Peer-Reviewed Surge, Breakthrough"),
                        "sourceHint" to Schema.string(description = "Supporting domain or publication"),
                        "suggestedQuery" to Schema.string(description = "Ready-to-run research prompt for Kafka agent")
                    )
                ),
                description = "Up to 5 trending research topics distilled from live web findings"
            )
        )
    )

    private val trendingTopicsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_FAST,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = trendingTopicsSchema
            }
        )
    }

    private suspend inline fun <T> withPrimaryThenFastFallback(
        primaryTimeoutMs: Long = 35_000L,
        fallbackTimeoutMs: Long = 35_000L,
        crossinline primaryCall: suspend () -> T,
        crossinline fallbackCall: suspend () -> T,
    ): T {
        return try {
            withTimeout(primaryTimeoutMs) { primaryCall() }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "Primary model ($MODEL_PRIMARY) timed out after ${primaryTimeoutMs}ms; retrying on $MODEL_FAST")
            withTimeout(fallbackTimeoutMs) { fallbackCall() }
        }
    }

    fun streamAgentResponse(
        userPrompt: String,
        mode: AgentMode,
        activeSkills: List<AgentSkill>,
        webResults: List<WebSearchResult>,
        journals: List<JournalPaper>,
        historySummary: String
    ): Flow<String> = flow {
        val skillDirectives = if (activeSkills.isEmpty()) {
            "- Standar Hermes Agent: riset langsung, verifikasi fakta, jawaban ringkas dan terstruktur."
        } else {
            activeSkills.take(4).joinToString("\n") { "- [${it.name} | ${it.outputFormat}]: ${it.systemPrompt.take(260)}" }
        }

        val webContext = if (webResults.isEmpty()) {
            "(Tidak ada hasil web langsung)"
        } else {
            webResults.take(5).mapIndexed { idx, w ->
                "[W${idx + 1}] ${w.title} (${w.domain}) — ${w.snippet.take(200)} | URL: ${w.url}"
            }.joinToString("\n")
        }

        val journalContext = if (journals.isEmpty()) {
            "(Tidak ada jurnal ilmiah ditemukan)"
        } else {
            journals.take(5).mapIndexed { idx, j ->
                "[J${idx + 1}] ${j.title} | ${j.authors.take(2).joinToString()} (${j.year}) | ${j.venue} | Sitasi: ${j.citedByCount} | DOI: ${j.doi ?: "-"} | URL: ${j.url}\nAbstrak: ${j.abstractText.take(220)}"
            }.joinToString("\n\n")
        }

        val prompt = """
            Kamu adalah **Kafka**, agen AI riset otonom bergaya Hermes Agent yang akurat dan anti-halusinasi.
            Jawab dalam Bahasa Indonesia yang jelas, terstruktur, dan ringkas-padat (maksimal ~450 kata kecuali diminta lebih rinci).
            Mode aktif: ${mode.label} (${mode.badge})

            SKILL AKTIF:
            $skillDirectives

            KONTEKS PERCAKAPAN SEBELUMNYA:
            ${historySummary.ifBlank { "Sesi baru." }.take(400)}

            HASIL LIVE WEB SEARCH (TOP 5):
            $webContext

            HASIL JURNAL ILMIAH (TOP 5):
            $journalContext

            INSTRUKSI PENJAWABAN:
            1. Gunakan bukti nyata dari sumber di atas dan cantumkan tag [W1] atau [J1] jika merujuk sumber.
            2. Jika data kurang, nyatakan jujur bagian yang belum terkonfirmasi—jangan mengarang kutipan atau DOI.
            3. Susun dengan heading & bullet poin rapi siap pakai untuk dokumen Word/PDF/PPT.

            PERMINTAAN PENGGUNA:
            $userPrompt
        """.trimIndent()

        val accumulated = StringBuilder()
        var streamCompleted = false
        try {
            withTimeout(35_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        accumulated.append(part)
                        emit(accumulated.toString())
                    }
                }
            }
            streamCompleted = accumulated.isNotBlank()
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "Primary stream ($MODEL_PRIMARY) timed out; falling back to $MODEL_FAST")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Primary stream error (${e.message}); falling back to $MODEL_FAST", e)
        }

        if (!streamCompleted) {
            val prefix = if (accumulated.isNotBlank()) "${accumulated}\n\n" else ""
            val fallbackAcc = StringBuilder()
            try {
                withTimeout(35_000L) {
                    fastChatModel.generateContentStream(prompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            fallbackAcc.append(part)
                            emit(prefix + fallbackAcc.toString())
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Fast stream ($MODEL_FAST) timed out; trying unary generateContent")
            }

            if (fallbackAcc.isBlank()) {
                val unaryText = withTimeout(35_000L) {
                    fastChatModel.generateContent(prompt).text.orEmpty()
                }
                if (unaryText.isNotBlank()) {
                    emit(prefix + unaryText)
                } else if (accumulated.isBlank()) {
                    throw IllegalStateException("Model mengembalikan respons kosong.")
                }
            }
        }
    }

    suspend fun verifyContent(
        contentToVerify: String,
        webResults: List<WebSearchResult>,
        journals: List<JournalPaper>,
        reachableUrlsCount: Int
    ): VerificationReport {
        val evidence = buildString {
            appendLine("=== SUMBER WEB LANGSUNG ===")
            webResults.forEachIndexed { i, w ->
                appendLine("[W${i + 1}] ${w.title} | ${w.url} | ${w.snippet}")
            }
            appendLine("\n=== LITERATUR JURNAL ILMIAH ===")
            journals.forEachIndexed { i, j ->
                appendLine("[J${i + 1}] ${j.title} (${j.year}, ${j.venue}, sitasi=${j.citedByCount}) | URL=${j.url} | ${j.abstractText.take(300)}")
            }
        }

        val prompt = """
            Lakukan audit verifikasi fakta terhadap teks berikut berdasarkan sumber web langsung dan jurnal ilmiah yang tersedia.
            Jangan mengarang bukti. Jika tidak ada bukti yang mendukung klaim, tandai UNVERIFIED.

            TEKS YANG DIVERIFIKASI:
            ${contentToVerify.take(6000)}

            BUKTI YANG DITEMUKAN:
            $evidence
        """.trimIndent()

        val responseText = try {
            withTimeout(60_000L) {
                verificationModel.generateContent(prompt).text
            } ?: throw IllegalStateException("Hasil verifikasi kosong.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Verification timeout", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        }

        val json = JSONObject(responseText)
        val score = json.optInt("overallScore", 50).coerceIn(0, 100)
        val status = VerificationStatus.fromRaw(json.optString("status", "PARTIAL"))
        val summary = json.optString("summary", "Audit verifikasi selesai.")
        val claimsArr = json.optJSONArray("claims")
        val claims = buildList {
            if (claimsArr != null) {
                for (i in 0 until claimsArr.length()) {
                    val obj = claimsArr.optJSONObject(i) ?: continue
                    add(
                        ClaimCheck(
                            claim = obj.optString("claim"),
                            verdict = obj.optString("verdict", "UNVERIFIED"),
                            explanation = obj.optString("explanation"),
                            evidenceSource = obj.optString("evidenceSource"),
                            evidenceUrl = obj.optString("evidenceUrl"),
                        )
                    )
                }
            }
        }

        return VerificationReport(
            overallScore = score,
            status = status,
            summary = summary,
            claims = claims,
            sourcesChecked = webResults.size + journals.size,
            verifiedLinks = reachableUrlsCount
        )
    }

    suspend fun createSkillFromScratch(userGoal: String): AgentSkill {
        val prompt = """
            Rancang sebuah Skill Agen AI lengkap dari nol untuk aplikasi Kafka (berbasis arsitektur Hermes Agent).
            Kebutuhan pengguna: "$userGoal"
            Buat systemPrompt yang sangat jelas, terstruktur (peran, langkah eksekusi, verifikasi, format output), dan pilih outputFormat yang paling tepat.
        """.trimIndent()

        val text = withPrimaryThenFastFallback(
            primaryCall = { skillArchitectModel.generateContent(prompt).text },
            fallbackCall = { fastSkillArchitectModel.generateContent(prompt).text }
        ) ?: throw IllegalStateException("AI gagal merancang skill.")

        val json = JSONObject(text)
        return AgentSkill(
            name = json.optString("name", "Skill Kustom Kafka").take(100),
            slug = json.optString("slug", "kafka-skill")
                .lowercase()
                .replace(Regex("[^a-z0-9\\-]"), "-")
                .take(80),
            description = json.optString("description", userGoal).take(1000),
            category = SkillCategory.fromRaw(json.optString("category", "CUSTOM")).name,
            sourceType = SkillSourceType.AI_CREATED.name,
            systemPrompt = json.optString("systemPrompt", userGoal).take(8000),
            outputFormat = OutputFormat.fromRaw(json.optString("outputFormat", "RESEARCH")).name,
            enabled = true,
            usageCount = 0L
        )
    }

    suspend fun distillGitHubRepoToSkill(
        repo: GitHubSkillRepo,
        readmeContent: String
    ): AgentSkill {
        val prompt = """
            Konversi repositori GitHub berikut menjadi sebuah Skill Agen Kafka yang siap dieksekusi.
            Repo: ${repo.fullName}
            Deskripsi: ${repo.description}
            Topik: ${repo.topics.joinToString(", ")}

            Cuplikan SKILL.md / README.md:
            ${readmeContent.ifBlank { repo.description }.take(4500)}
        """.trimIndent()

        val text = withPrimaryThenFastFallback(
            primaryCall = { skillArchitectModel.generateContent(prompt).text },
            fallbackCall = { fastSkillArchitectModel.generateContent(prompt).text }
        ) ?: throw IllegalStateException("Gagal mengimpor skill dari GitHub.")

        val json = JSONObject(text)
        return AgentSkill(
            name = json.optString("name", repo.name).take(100),
            slug = json.optString("slug", repo.name.lowercase())
                .lowercase()
                .replace(Regex("[^a-z0-9\\-]"), "-")
                .take(80),
            description = json.optString("description", repo.description).take(1000),
            category = SkillCategory.GITHUB.name,
            sourceType = SkillSourceType.GITHUB.name,
            githubRepo = repo.fullName.take(200),
            githubUrl = repo.htmlUrl.take(500),
            systemPrompt = json.optString("systemPrompt", repo.description).take(8000),
            outputFormat = OutputFormat.fromRaw(json.optString("outputFormat", "RESEARCH")).name,
            enabled = true,
            usageCount = 0L
        )
    }

    suspend fun generateDocumentBlueprint(
        topicOrContent: String,
        format: OutputFormat,
        citations: List<CitationItem>
    ): DocumentBlueprint {
        val citationBlock = if (citations.isEmpty()) {
            "Gunakan hanya fakta yang valid."
        } else {
            citations.joinToString("\n") { "- ${it.title} (${it.source}) ${it.url}" }
        }

        val formatHint = when (format) {
            OutputFormat.PPTX -> "Format ini adalah presentasi PowerPoint (.pptx): buat 5-6 section yang masing-masing cocok menjadi 1 slide (heading tajam, body 1 kalimat pengantar, 3-4 bullets singkat)."
            OutputFormat.PDF -> "Format ini adalah laporan dossier PDF formal: buat 4-6 section analitik mendalam."
            OutputFormat.DOCX -> "Format ini adalah dokumen Microsoft Word (.docx): buat 4-6 section terstruktur dengan ringkasan eksekutif dan poin-poin terperinci."
            OutputFormat.CSV -> "Format ini adalah tabel data CSV: buat section yang merangkum baris dan poin terstruktur."
            else -> "Format ini adalah dokumen Markdown terstruktur."
        }

        val prompt = """
            Susun blueprint dokumen profesional berbahasa Indonesia berdasarkan materi berikut.
            $formatHint

            MATERI / TOPIK:
            ${topicOrContent.take(6500)}

            REFERENSI TERSEDIA:
            $citationBlock
        """.trimIndent()

        val text = withPrimaryThenFastFallback(
            primaryCall = { blueprintModel.generateContent(prompt).text },
            fallbackCall = { fastBlueprintModel.generateContent(prompt).text }
        ) ?: throw IllegalStateException("Gagal menyusun struktur dokumen.")

        val json = JSONObject(text)
        val sectionsArr = json.optJSONArray("sections")
        val sections = buildList {
            if (sectionsArr != null) {
                for (i in 0 until sectionsArr.length()) {
                    val s = sectionsArr.optJSONObject(i) ?: continue
                    val bulletsArr = s.optJSONArray("bullets")
                    val bullets = buildList {
                        if (bulletsArr != null) {
                            for (b in 0 until bulletsArr.length()) {
                                val item = bulletsArr.optString(b)
                                if (item.isNotBlank()) add(item)
                            }
                        }
                    }
                    add(
                        DocumentSection(
                            heading = s.optString("heading", "Bagian ${i + 1}"),
                            body = s.optString("body"),
                            bullets = bullets
                        )
                    )
                }
            }
        }
        val refsArr = json.optJSONArray("references")
        val refs = buildList {
            if (refsArr != null) {
                for (r in 0 until refsArr.length()) {
                    val v = refsArr.optString(r)
                    if (v.isNotBlank()) add(v)
                }
            }
        }

        return DocumentBlueprint(
            title = json.optString("title", "Laporan Riset Kafka"),
            subtitle = json.optString("subtitle", "Disusun oleh Kafka Autonomous Research Agent"),
            executiveSummary = json.optString("executiveSummary", ""),
            sections = sections,
            references = refs
        )
    }

    suspend fun summarizeAndVerifyJournal(
        paper: JournalPaper,
        crossSources: List<WebSearchResult>,
        relatedPapers: List<JournalPaper>
    ): JournalSummaryVerification {
        val crossEvidence = buildString {
            appendLine("=== SUMBER PEMBANDING WEB ===")
            crossSources.take(4).forEachIndexed { idx, w ->
                appendLine("[W${idx + 1}] ${w.title} (${w.domain}) — ${w.snippet}")
            }
            appendLine("\n=== PUBLIKASI TERKAIT ===")
            relatedPapers.filter { it.title != paper.title }.take(4).forEachIndexed { idx, p ->
                appendLine("[P${idx + 1}] ${p.title} (${p.year}, ${p.venue}, sitasi=${p.citedByCount}) — ${p.abstractText.take(220)}")
            }
        }

        val prompt = """
            Ringkas jurnal ilmiah berikut secara padat dan jelas (dalam Bahasa Indonesia), mencakup:
            1. keyFindings: Temuan utama makalah.
            2. methodology: Metodologi, desain riset, atau pendekatan yang digunakan.
            3. limitations: Batasan studi atau hal yang perlu dicermati.
            4. Verifikasi akurasi klaim dan konsistensinya terhadap pengetahuan ilmiah umum serta sumber pembanding yang ditemukan.

            DETAIL JURNAL:
            Judul: ${paper.title}
            Penulis: ${paper.authors.joinToString()}
            Tahun: ${paper.year} | Venue: ${paper.venue} | Sitasi: ${paper.citedByCount} | Peer-reviewed: ${paper.isPeerReviewed}
            DOI: ${paper.doi ?: "-"}
            Abstrak: ${paper.abstractText}

            $crossEvidence
        """.trimIndent()

        val text = withTimeout(60_000L) {
            journalSummaryModel.generateContent(prompt).text
        } ?: throw IllegalStateException("Ringkasan jurnal kosong.")

        val json = JSONObject(text)
        return JournalSummaryVerification(
            keyFindings = json.optString("keyFindings", "Ringkasan temuan tersedia."),
            methodology = json.optString("methodology", "Metodologi berbasis abstrak."),
            limitations = json.optString("limitations", "Informasi lengkap merujuk ke naskah asli."),
            accuracyScore = json.optInt("accuracyScore", if (paper.isPeerReviewed) 82 else 65).coerceIn(0, 100),
            accuracyStatus = VerificationStatus.fromRaw(json.optString("accuracyStatus", "PARTIAL")),
            accuracyNotes = json.optString("accuracyNotes", "Diverifikasi terhadap metadata publikasi dan sumber pembanding."),
            crossCheckedSources = crossSources.size + relatedPapers.size
        )
    }

    suspend fun transcribeVoiceQuery(
        wavBytes: ByteArray,
        preferredLanguage: String = "Indonesia"
    ): String {
        val promptContent = content {
            inlineData(wavBytes, "audio/wav")
            text(
                "Transkripsikan ucapan pengguna dalam audio ini menjadi teks kueri riset yang akurat. " +
                    "Bahasa utama kemungkinan $preferredLanguage atau Inggris teknis. " +
                    "Kembalikan HANYA teks transkripsi murni tanpa tanda kutip, label, atau komentar tambahan. " +
                    "Jika tidak ada ucapan jelas, kembalikan string kosong."
            )
        }
        val response = withPrimaryThenFastFallback(
            primaryTimeoutMs = 30_000L,
            fallbackTimeoutMs = 30_000L,
            primaryCall = { chatModel.generateContent(promptContent) },
            fallbackCall = { fastChatModel.generateContent(promptContent) }
        )
        return response.text?.trim().orEmpty()
    }

    suspend fun generateImage(prompt: String): Bitmap {
        val response = withTimeout(60_000L) {
            imageGenModel.generateContent(prompt)
        }
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw IllegalStateException("Model gambar ($MODEL_IMAGE) tidak mengembalikan gambar.")
    }

    suspend fun editImage(inputBitmap: Bitmap, editInstruction: String): Bitmap {
        val response = withTimeout(60_000L) {
            imageGenModel.generateContent(
                content {
                    image(inputBitmap)
                    text(editInstruction)
                }
            )
        }
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw IllegalStateException("Model edit gambar ($MODEL_IMAGE) tidak mengembalikan hasil.")
    }

    suspend fun extractTrendingTopics(
        webResults: List<WebSearchResult>,
        recentQueries: List<String>
    ): List<TrendingResearchTopic> {
        if (webResults.isEmpty() && recentQueries.isEmpty()) return emptyList()
        val evidence = buildString {
            if (recentQueries.isNotEmpty()) {
                appendLine("=== RECENT RESEARCH QUERIES ===")
                recentQueries.take(5).forEach { appendLine("- $it") }
            }
            appendLine("=== LIVE WEB RESULTS ===")
            webResults.take(10).forEachIndexed { idx, w ->
                appendLine("[${idx + 1}] ${w.title} (${w.domain}) — ${w.snippet}")
            }
        }
        val prompt = """
            Identifikasi 4 hingga 5 topik riset yang sedang tren / penting berdasarkan hasil pencarian web langsung dan riwayat kueri berikut.
            Setiap topik harus nyata sesuai konteks bukti yang ditemukan dan menyertakan `suggestedQuery` dalam Bahasa Indonesia agar pengguna bisa langsung meluncurkan riset mendalam.

            $evidence
        """.trimIndent()

        val text = withTimeout(40_000L) {
            trendingTopicsModel.generateContent(prompt).text
        } ?: return emptyList()

        val json = JSONObject(text)
        val arr = json.optJSONArray("topics") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val topic = obj.optString("topic").trim()
                if (topic.isBlank()) continue
                add(
                    TrendingResearchTopic(
                        topic = topic,
                        category = obj.optString("category", "Riset AI"),
                        momentumLabel = obj.optString("momentumLabel", "Trending"),
                        sourceHint = obj.optString("sourceHint", "Live Web"),
                        suggestedQuery = obj.optString("suggestedQuery", "Riset mendalam dan verifikasi fakta tentang $topic")
                    )
                )
            }
        }
    }

    fun buildCitationsFromSources(
        webResults: List<WebSearchResult>,
        journals: List<JournalPaper>
    ): List<CitationItem> {
        val list = mutableListOf<CitationItem>()
        journals.take(5).forEachIndexed { idx, j ->
            list += CitationItem(
                refTag = "J${idx + 1}",
                title = j.title,
                url = j.url,
                source = "${j.sourceProvider} • ${j.venue} (${j.year})",
                snippet = j.abstractText.take(360),
                verified = j.isPeerReviewed || j.doi != null,
                sourceType = "JOURNAL",
                authorsText = j.authors.joinToString(", "),
                year = j.year,
                venue = j.venue,
                doi = j.doi.orEmpty(),
                citedByCount = j.citedByCount,
                provider = j.sourceProvider
            )
        }
        webResults.take(5).forEachIndexed { idx, w ->
            list += CitationItem(
                refTag = "W${idx + 1}",
                title = w.title,
                url = w.url,
                source = "${w.sourceEngine} • ${w.domain}",
                snippet = w.snippet.take(300),
                verified = w.reachable,
                sourceType = "WEB",
                venue = w.domain,
                provider = w.sourceEngine
            )
        }
        return list.take(10)
    }

    suspend fun summarizeQuickText(prompt: String): String {
        return try {
            withTimeout(30_000L) {
                val response = fastChatModel.generateContent(prompt)
                response.text?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Ringkasan tidak tersedia untuk teks ini."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Quick summary timed out", e)
            "Ringkasan melebihi batas waktu. Silakan coba lagi."
        } catch (e: Exception) {
            Log.e(TAG, "Quick summary failed", e)
            throw e
        }
    }
}
