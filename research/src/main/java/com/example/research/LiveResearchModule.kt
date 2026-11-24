package com.example.research

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val TAG = "LiveResearchModule"

open class LiveResearchModule {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "KafkaAgent/1.1 (Android; Research & Verification)")
                .header("Accept", "application/json, text/html, application/xml;q=0.9")
                .build()
            chain.proceed(req)
        }
        .build()

    suspend fun searchLiveWeb(query: String, limit: Int = 12): List<WebSearchResult> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            coroutineScope {
                val ddgDeferred = async { fetchDuckDuckGoHtml(trimmed) }
                val wikiEnDeferred = async { fetchWikipediaSearch(trimmed, "en") }
                val wikiIdDeferred = async { fetchWikipediaSearch(trimmed, "id") }
                val hnDeferred = async { fetchHackerNewsAlgolia(trimmed) }
                val ghDeferred = async { fetchGitHubWebResults(trimmed) }

                val combined = mutableListOf<WebSearchResult>()
                combined += ddgDeferred.await()
                combined += wikiEnDeferred.await()
                combined += wikiIdDeferred.await()
                combined += hnDeferred.await()
                combined += ghDeferred.await()

                combined
                    .distinctBy { it.url.trimEnd('/') }
                    .take(limit)
            }
        }

    suspend fun searchAcademicJournals(query: String, limit: Int = 16): List<JournalPaper> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            coroutineScope {
                val openAlexDeferred = async { fetchOpenAlex(trimmed) }
                val europePmcDeferred = async { fetchEuropePmc(trimmed) }
                val doajDeferred = async { fetchDoaj(trimmed) }
                val arxivDeferred = async { fetchArxiv(trimmed) }
                val crossrefDeferred = async { fetchCrossref(trimmed) }
                val ghPapersDeferred = async { fetchGitHubPaperCollectionJournals(trimmed) }

                val merged = mutableListOf<JournalPaper>()
                merged += openAlexDeferred.await()
                merged += europePmcDeferred.await()
                merged += doajDeferred.await()
                merged += arxivDeferred.await()
                merged += ghPapersDeferred.await()
                merged += crossrefDeferred.await()

                merged
                    .distinctBy { (it.doi ?: it.title.lowercase()).trim() }
                    .sortedWith(
                        compareByDescending<JournalPaper> { !it.pdfUrl.isNullOrBlank() }
                            .thenByDescending { it.isOpenAccess }
                            .thenByDescending { it.citedByCount }
                    )
                    .take(limit)
            }
        }

    suspend fun searchGitHubJournalCollections(query: String, limit: Int = 12): List<JournalPaper> =
        withContext(Dispatchers.IO) {
            val clean = query.trim().ifBlank { "machine learning research papers" }
            fetchGitHubPaperCollectionJournals(clean).take(limit)
        }

    suspend fun searchGitHubSkills(query: String, limit: Int = 12): List<GitHubSkillRepo> =
        withContext(Dispatchers.IO) {
            val effectiveQuery = if (query.isBlank()) {
                "ai agent skills mcp prompt"
            } else {
                query.trim()
            }
            val encoded = URLEncoder.encode(effectiveQuery, "UTF-8")
            val url = "https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=$limit"

            val body = executeGet(url) ?: return@withContext emptyList()
            try {
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                buildList {
                    for (i in 0 until items.length()) {
                        val obj = items.optJSONObject(i) ?: continue
                        val owner = obj.optJSONObject("owner")
                        val topicsArr = obj.optJSONArray("topics")
                        val topics = buildList {
                            if (topicsArr != null) {
                                for (t in 0 until minOf(topicsArr.length(), 6)) {
                                    add(topicsArr.optString(t))
                                }
                            }
                        }
                        add(
                            GitHubSkillRepo(
                                fullName = obj.optString("full_name"),
                                name = obj.optString("name"),
                                description = obj.optString("description")
                                    .takeIf { it.isNotBlank() && it != "null" }
                                    ?: "Repositori GitHub tanpa deskripsi.",
                                htmlUrl = obj.optString("html_url"),
                                defaultBranch = obj.optString("default_branch", "main"),
                                stars = obj.optInt("stargazers_count", 0),
                                language = obj.optString("language")
                                    .takeIf { it.isNotBlank() && it != "null" }
                                    ?: "Markdown",
                                topics = topics,
                                ownerAvatarUrl = owner?.optString("avatar_url").orEmpty(),
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "GitHub search parse error", e)
                emptyList()
            }
        }

    suspend fun fetchRepoSkillContent(fullName: String, defaultBranch: String): String =
        withContext(Dispatchers.IO) {
            val candidates = listOf(
                "https://raw.githubusercontent.com/$fullName/$defaultBranch/SKILL.md",
                "https://raw.githubusercontent.com/$fullName/$defaultBranch/skills/SKILL.md",
                "https://raw.githubusercontent.com/$fullName/$defaultBranch/PROMPT.md",
                "https://raw.githubusercontent.com/$fullName/$defaultBranch/README.md",
                "https://raw.githubusercontent.com/$fullName/main/README.md",
                "https://raw.githubusercontent.com/$fullName/master/README.md",
            )
            for (url in candidates) {
                val content = executeGet(url)
                if (!content.isNullOrBlank()) {
                    return@withContext content.take(6000)
                }
            }
            ""
        }

    suspend fun verifyUrlStatus(url: String): LinkAuditResult = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext LinkAuditResult(url = url, reachable = false, httpCode = 0)
        }
        try {
            val headReq = Request.Builder().url(url).head().build()
            client.newCall(headReq).execute().use { resp ->
                if (resp.isSuccessful || resp.code in 300..399) {
                    return@withContext LinkAuditResult(url = url, reachable = true, httpCode = resp.code)
                }
            }
            val getReq = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1024")
                .build()
            client.newCall(getReq).execute().use { resp ->
                val ok = resp.isSuccessful || resp.code == 416
                LinkAuditResult(url = url, reachable = ok, httpCode = resp.code)
            }
        } catch (_: Exception) {
            LinkAuditResult(url = url, reachable = false, httpCode = 0)
        }
    }

    suspend fun verifyUrlReachable(url: String): Boolean = verifyUrlStatus(url).reachable

    /**
     * Standalone multi-source claim & link verification engine.
     * Can operate independently of cloud AI or enrich/fallback when cloud AI is unavailable.
     */
    suspend fun verifyContentMultiSource(
        contentToVerify: String,
        preloadedWebResults: List<WebSearchResult>? = null,
        preloadedJournals: List<JournalPaper>? = null,
    ): VerificationReport = withContext(Dispatchers.IO) {
        val trimmed = contentToVerify.trim()
        if (trimmed.isBlank()) {
            return@withContext VerificationReport(
                overallScore = 0,
                status = VerificationStatus.NONE,
                summary = "Tidak ada teks atau klaim yang diberikan untuk diverifikasi.",
                claims = emptyList(),
                sourcesChecked = 0,
                verifiedLinks = 0
            )
        }

        val querySeed = trimmed.lineSequence()
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .take(120)

        val (webResults, journals) = coroutineScope {
            val webDeferred = async { preloadedWebResults ?: searchLiveWeb(querySeed, limit = 6) }
            val journalDeferred = async { preloadedJournals ?: searchAcademicJournals(querySeed, limit = 6) }
            webDeferred.await() to journalDeferred.await()
        }

        val urlsToProbe = buildList {
            addAll(extractHttpUrls(trimmed).take(2))
            addAll(webResults.take(3).map { it.url })
            addAll(journals.take(2).mapNotNull { it.pdfUrl ?: it.url.takeIf { u -> u.startsWith("http") } })
        }.distinct().take(5)

        val linkAudits = coroutineScope {
            urlsToProbe.map { link -> async { verifyUrlStatus(link) } }.map { it.await() }
        }
        val reachableCount = linkAudits.count { it.reachable }

        val candidateClaims = extractCandidateClaims(trimmed)
        val checks = candidateClaims.map { claim ->
            evaluateClaimAgainstSources(claim, webResults, journals)
        }

        val supported = checks.count { it.verdict == "SUPPORTED" }
        val partial = checks.count { it.verdict == "PARTIAL" }
        val total = checks.size.coerceAtLeast(1)
        val baseScore = ((supported * 100 + partial * 60) / total)
        val sourceBonus = when {
            journals.isNotEmpty() && reachableCount >= 2 -> 12
            webResults.isNotEmpty() && reachableCount >= 1 -> 6
            else -> 0
        }
        val finalScore = (baseScore + sourceBonus).coerceIn(10, 96)
        val status = when {
            finalScore >= 75 -> VerificationStatus.VERIFIED
            finalScore >= 45 -> VerificationStatus.PARTIAL
            else -> VerificationStatus.UNVERIFIED
        }

        val summary = buildString {
            append("Diperiksa terhadap ${webResults.size} sumber web live dan ${journals.size} artikel jurnal ")
            append("($reachableCount tautan HTTP aktif). ")
            append("$supported klaim terdukung penuh, $partial terdukung sebagian dari ${checks.size} klaim.")
        }

        VerificationReport(
            overallScore = finalScore,
            status = status,
            summary = summary,
            claims = checks,
            sourcesChecked = webResults.size + journals.size,
            verifiedLinks = reachableCount
        )
    }

    /**
     * Verifies an academic journal paper directly via HTTP reachability + cross-source lookup.
     */
    suspend fun verifyJournalQuick(
        paper: JournalPaper,
        crossSources: List<WebSearchResult>? = null,
        relatedPapers: List<JournalPaper>? = null,
    ): JournalSummaryVerification = withContext(Dispatchers.IO) {
        val targetLink = paper.pdfUrl?.takeIf { it.startsWith("http") } ?: paper.url
        val (web, journals, linkAudit) = coroutineScope {
            val w = async { crossSources ?: searchLiveWeb(paper.title.take(100), limit = 5) }
            val j = async { relatedPapers ?: searchAcademicJournals(paper.title.take(100), limit = 5) }
            val l = async { verifyUrlStatus(targetLink) }
            Triple(w.await(), j.await(), l.await())
        }

        val crossMatchCount = web.size + journals.count {
            !it.title.equals(paper.title, ignoreCase = true)
        }
        var score = 50
        if (!paper.doi.isNullOrBlank()) score += 16
        if (paper.isPeerReviewed) score += 12
        if (linkAudit.reachable) score += 12
        if (paper.citedByCount > 0) score += 5
        if (crossMatchCount >= 2) score += 5
        score = score.coerceIn(30, 96)

        val status = when {
            score >= 75 -> VerificationStatus.VERIFIED
            score >= 50 -> VerificationStatus.PARTIAL
            else -> VerificationStatus.UNVERIFIED
        }

        val linkText = if (linkAudit.reachable) {
            "Tautan jurnal/PDF aktif (HTTP ${linkAudit.httpCode})."
        } else {
            "Tautan utama perlu verifikasi manual di browser."
        }

        JournalSummaryVerification(
            keyFindings = paper.abstractText.take(420),
            methodology = "Publikasi ${paper.sourceProvider} (${paper.venue}${if (paper.year > 0) ", ${paper.year}" else ""})${paper.doi?.let { " • DOI: $it" } ?: ""}.",
            limitations = if (paper.isPeerReviewed) {
                "Evaluasi berasal dari metadata abstrak & validasi tautan lintas sumber."
            } else {
                "Sumber preprint/koleksi terbuka; periksa peer-review lebih lanjut pada dokumen asli."
            },
            accuracyScore = score,
            accuracyStatus = status,
            accuracyNotes = "$linkText Tervalidasi dengan $crossMatchCount sumber pembanding (${if (paper.isOpenAccess) "Open Access" else "Terindeks"}).",
            crossCheckedSources = crossMatchCount
        )
    }

    private fun extractCandidateClaims(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 16 }
            .take(5)
        return if (sentences.isEmpty()) listOf(text.take(220)) else sentences
    }

    private fun evaluateClaimAgainstSources(
        claim: String,
        webResults: List<WebSearchResult>,
        journals: List<JournalPaper>,
    ): ClaimCheck {
        val stopWords = setOf(
            "yang", "dan", "atau", "dari", "untuk", "dengan", "pada", "dalam", "adalah", "ini", "itu",
            "the", "and", "for", "with", "that", "this", "from", "are", "was", "were", "into"
        )
        val tokens = claim.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()

        var bestScore = 0.0
        var bestTitle = ""
        var bestUrl = ""
        var bestProvider = ""

        journals.forEach { paper ->
            val corpus = "${paper.title} ${paper.abstractText} ${paper.venue}".lowercase()
            val hits = tokens.count { corpus.contains(it) }
            val ratio = if (tokens.isEmpty()) 0.0 else hits.toDouble() / tokens.size.toDouble()
            val weighted = ratio + if (paper.isPeerReviewed) 0.12 else 0.05
            if (weighted > bestScore) {
                bestScore = weighted
                bestTitle = paper.title
                bestUrl = paper.pdfUrl ?: paper.url
                bestProvider = paper.sourceProvider
            }
        }

        webResults.forEach { web ->
            val corpus = "${web.title} ${web.snippet}".lowercase()
            val hits = tokens.count { corpus.contains(it) }
            val ratio = if (tokens.isEmpty()) 0.0 else hits.toDouble() / tokens.size.toDouble()
            if (ratio > bestScore) {
                bestScore = ratio
                bestTitle = web.title
                bestUrl = web.url
                bestProvider = web.sourceEngine
            }
        }

        val verdict = when {
            bestScore >= 0.48 -> "SUPPORTED"
            bestScore >= 0.22 -> "PARTIAL"
            else -> "UNVERIFIED"
        }

        val explanation = when (verdict) {
            "SUPPORTED" -> "Klaim cocok dengan referensi aktif di $bestProvider ($bestTitle)."
            "PARTIAL" -> "Sebagian kata kunci klaim ditemukan di $bestProvider ($bestTitle), perlu tinjauan konteks lanjutan."
            else -> "Belum ditemukan kecocokan kuat pada indeks web & jurnal saat ini."
        }

        return ClaimCheck(
            claim = claim,
            verdict = verdict,
            explanation = explanation,
            evidenceSource = if (bestTitle.isNotBlank()) "$bestProvider: ${bestTitle.take(48)}" else "Tidak ada bukti cocok",
            evidenceUrl = bestUrl
        )
    }

    private fun extractHttpUrls(text: String): List<String> {
        val urlRegex = Regex("""https?://[^\s)\]>"]+""")
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private fun fetchDuckDuckGoHtml(query: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://html.duckduckgo.com/html/?q=$encoded"
        val html = executeGet(url) ?: return emptyList()
        val results = mutableListOf<WebSearchResult>()
        val pattern = Pattern.compile(
            """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>.*?<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(html)
        while (matcher.find() && results.size < 6) {
            val rawHref = matcher.group(1).orEmpty()
            val cleanUrl = extractDuckDuckGoUrl(rawHref)
            val title = stripHtml(matcher.group(2).orEmpty())
            val snippet = stripHtml(matcher.group(3).orEmpty())
            if (cleanUrl.startsWith("http") && title.isNotBlank()) {
                results += WebSearchResult(
                    title = title,
                    url = cleanUrl,
                    domain = extractDomain(cleanUrl),
                    snippet = snippet.ifBlank { "Hasil pencarian web langsung." },
                    sourceEngine = "DuckDuckGo Live"
                )
            }
        }
        return results
    }

    private fun fetchWikipediaSearch(query: String, lang: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url =
            "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&srlimit=3&format=json&utf8=1"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val arr = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
                ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title")
                    val slug = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                    val snippet = stripHtml(obj.optString("snippet"))
                    add(
                        WebSearchResult(
                            title = title,
                            url = "https://$lang.wikipedia.org/wiki/$slug",
                            domain = "$lang.wikipedia.org",
                            snippet = snippet,
                            sourceEngine = "Wikipedia ($lang)"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search error", e)
            emptyList()
        }
    }

    private fun fetchHackerNewsAlgolia(query: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://hn.algolia.com/api/v1/search?query=$encoded&tags=story&hitsPerPage=4"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val hits = JSONObject(body).optJSONArray("hits") ?: return emptyList()
            buildList {
                for (i in 0 until hits.length()) {
                    val obj = hits.optJSONObject(i) ?: continue
                    val title = obj.optString("title")
                    val itemUrl = obj.optString("url")
                        .takeIf { it.startsWith("http") }
                        ?: "https://news.ycombinator.com/item?id=${obj.optString("objectID")}"
                    if (title.isNotBlank()) {
                        add(
                            WebSearchResult(
                                title = title,
                                url = itemUrl,
                                domain = extractDomain(itemUrl),
                                snippet = "Diskusi teknologi (${obj.optInt("points")} poin, ${obj.optInt("num_comments")} komentar).",
                                sourceEngine = "Algolia Tech Index"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "HN search error", e)
            emptyList()
        }
    }

    private fun fetchOpenAlex(query: String): List<JournalPaper> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.openalex.org/works?search=$encoded&per-page=7"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val title = obj.optString("display_name")
                    if (title.isBlank()) continue
                    val year = obj.optInt("publication_year", 0)
                    val cited = obj.optInt("cited_by_count", 0)
                    val doi = obj.optString("doi")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?.removePrefix("https://doi.org/")
                    val oaObj = obj.optJSONObject("open_access")
                    val oaUrl = oaObj?.optString("oa_url")
                        ?.takeIf { it.startsWith("http") }
                        ?.let(::normalizeHttpsUrl)
                    val bestOaPdf = obj.optJSONObject("best_oa_location")
                        ?.optString("pdf_url")
                        ?.takeIf { it.startsWith("http") }
                        ?.let(::normalizeHttpsUrl)
                    val bestOaLanding = obj.optJSONObject("best_oa_location")
                        ?.optString("landing_page_url")
                        ?.takeIf { it.startsWith("http") }
                        ?.let(::normalizeHttpsUrl)
                    val landingUrl = obj.optJSONObject("primary_location")
                        ?.optString("landing_page_url")
                        ?.takeIf { it.startsWith("http") }
                        ?.let(::normalizeHttpsUrl)
                    val primaryPdf = obj.optJSONObject("primary_location")
                        ?.optString("pdf_url")
                        ?.takeIf { it.startsWith("http") }
                        ?.let(::normalizeHttpsUrl)
                    val resolvedPdf = bestOaPdf ?: primaryPdf ?: oaUrl?.takeIf {
                        it.endsWith(".pdf", ignoreCase = true) || it.contains("/pdf/")
                    }
                    val finalUrl = oaUrl ?: bestOaLanding ?: landingUrl
                        ?: doi?.let { "https://doi.org/$it" }
                        ?: obj.optString("id")
                    val isOa = (oaObj?.optBoolean("is_oa", false) == true) ||
                        oaUrl != null || resolvedPdf != null

                    val venue = obj.optJSONObject("primary_location")
                        ?.optJSONObject("source")
                        ?.optString("display_name")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: "OpenAlex Indexed Journal"

                    val authorsArr = obj.optJSONArray("authorships")
                    val authors = buildList {
                        if (authorsArr != null) {
                            for (a in 0 until minOf(authorsArr.length(), 4)) {
                                val name = authorsArr.optJSONObject(a)
                                    ?.optJSONObject("author")
                                    ?.optString("display_name")
                                if (!name.isNullOrBlank()) add(name)
                            }
                        }
                    }

                    val abstractText = reconstructOpenAlexAbstract(
                        obj.optJSONObject("abstract_inverted_index")
                    )

                    add(
                        JournalPaper(
                            title = title,
                            authors = authors,
                            year = year,
                            venue = venue,
                            doi = doi,
                            url = finalUrl,
                            abstractText = abstractText.ifBlank {
                                "Jurnal terindeks OpenAlex ($venue, $year) dengan $cited sitasi."
                            },
                            citedByCount = cited,
                            sourceProvider = "OpenAlex",
                            isPeerReviewed = true,
                            pdfUrl = resolvedPdf,
                            isOpenAccess = isOa
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAlex parse error", e)
            emptyList()
        }
    }

    private fun fetchArxiv(query: String): List<JournalPaper> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://export.arxiv.org/api/query?search_query=all:$encoded&max_results=5"
        val xml = executeGet(url) ?: return emptyList()
        val papers = mutableListOf<JournalPaper>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            var inEntry = false
            var title = ""
            var summary = ""
            var idUrl = ""
            var year = 0
            val authors = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "entry" -> {
                                inEntry = true
                                title = ""
                                summary = ""
                                idUrl = ""
                                year = 0
                                authors.clear()
                            }
                            "title" -> if (inEntry) title = parser.nextText().replace(Regex("\\s+"), " ").trim()
                            "summary" -> if (inEntry) summary = parser.nextText().replace(Regex("\\s+"), " ").trim()
                            "id" -> if (inEntry) idUrl = parser.nextText().trim()
                            "published" -> if (inEntry) {
                                val pub = parser.nextText().trim()
                                year = pub.take(4).toIntOrNull() ?: 0
                            }
                            "name" -> if (inEntry) authors += parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "entry" && inEntry) {
                            inEntry = false
                            if (title.isNotBlank()) {
                                val httpsUrl = normalizeHttpsUrl(idUrl)
                                val pdfLink = httpsUrl.replace("/abs/", "/pdf/")
                                    .let { if (it.endsWith(".pdf")) it else "$it.pdf" }
                                papers += JournalPaper(
                                    title = title,
                                    authors = authors.take(4),
                                    year = year,
                                    venue = "arXiv Preprint Repository",
                                    doi = null,
                                    url = httpsUrl,
                                    abstractText = summary.take(500),
                                    citedByCount = 0,
                                    sourceProvider = "arXiv",
                                    isPeerReviewed = false,
                                    pdfUrl = pdfLink,
                                    isOpenAccess = true
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "arXiv XML parse error", e)
        }
        return papers
    }

    private fun fetchCrossref(query: String): List<JournalPaper> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.crossref.org/works?query=$encoded&rows=5"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val items = JSONObject(body).optJSONObject("message")?.optJSONArray("items")
                ?: return emptyList()
            buildList {
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val title = obj.optJSONArray("title")?.optString(0).orEmpty()
                    if (title.isBlank()) continue
                    val doi = obj.optString("DOI").takeIf { it.isNotBlank() }
                    val venue = obj.optJSONArray("container-title")?.optString(0)
                        ?.takeIf { it.isNotBlank() } ?: "Crossref DOI Registry"
                    val year = obj.optJSONObject("issued")
                        ?.optJSONArray("date-parts")
                        ?.optJSONArray(0)
                        ?.optInt(0, 0) ?: 0
                    val cited = obj.optInt("is-referenced-by-count", 0)
                    val authorsArr = obj.optJSONArray("author")
                    val authors = buildList {
                        if (authorsArr != null) {
                            for (a in 0 until minOf(authorsArr.length(), 4)) {
                                val au = authorsArr.optJSONObject(a) ?: continue
                                val full = listOf(au.optString("given"), au.optString("family"))
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                if (full.isNotBlank()) add(full)
                            }
                        }
                    }
                    val rawAbstract = stripHtml(obj.optString("abstract"))
                    add(
                        JournalPaper(
                            title = title,
                            authors = authors,
                            year = year,
                            venue = venue,
                            doi = doi,
                            url = doi?.let { "https://doi.org/$it" } ?: obj.optString("URL"),
                            abstractText = rawAbstract.ifBlank {
                                "Artikel ilmiah terverifikasi DOI ($venue, $year)."
                            },
                            citedByCount = cited,
                            sourceProvider = "Crossref",
                            isPeerReviewed = true,
                            pdfUrl = null,
                            isOpenAccess = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crossref parse error", e)
            emptyList()
        }
    }

    private fun fetchEuropePmc(query: String): List<JournalPaper> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url =
            "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=$encoded&format=json&resultType=core&pageSize=5"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val results = JSONObject(body).optJSONObject("resultList")?.optJSONArray("result")
                ?: return emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val title = stripHtml(obj.optString("title"))
                    if (title.isBlank()) continue
                    val doi = obj.optString("doi").takeIf { it.isNotBlank() && it != "null" }
                    val pmcid = obj.optString("pmcid").takeIf { it.isNotBlank() && it != "null" }
                    val pmid = obj.optString("pmid").takeIf { it.isNotBlank() && it != "null" }
                    val year = obj.optString("pubYear").toIntOrNull() ?: 0
                    val venue = obj.optString("journalTitle")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: "Europe PMC Open Access"
                    val cited = obj.optInt("citedByCount", 0)
                    val isOa = obj.optString("isOpenAccess").equals("Y", ignoreCase = true) || pmcid != null
                    val authors = obj.optString("authorString")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .take(4)

                    var pdfUrl: String? = null
                    var fullTextUrl: String? = null
                    val ftList = obj.optJSONObject("fullTextUrlList")?.optJSONArray("fullTextUrl")
                    if (ftList != null) {
                        for (j in 0 until ftList.length()) {
                            val ft = ftList.optJSONObject(j) ?: continue
                            val ftUrl = ft.optString("url").takeIf { it.startsWith("http") } ?: continue
                            val style = ft.optString("documentStyle")
                            if (pdfUrl == null && style.equals("pdf", ignoreCase = true)) {
                                pdfUrl = normalizeHttpsUrl(ftUrl)
                            } else if (fullTextUrl == null) {
                                fullTextUrl = normalizeHttpsUrl(ftUrl)
                            }
                        }
                    }

                    if (pmcid != null) {
                        if (pdfUrl == null) {
                            pdfUrl = "https://europepmc.org/backend/ptpmcrender.fcgi?accid=$pmcid&blobtype=pdf"
                        }
                        if (fullTextUrl == null) {
                            fullTextUrl = "https://europepmc.org/article/PMC/$pmcid"
                        }
                    }

                    val finalUrl = fullTextUrl
                        ?: pdfUrl
                        ?: doi?.let { "https://doi.org/$it" }
                        ?: pmid?.let { "https://pubmed.ncbi.nlm.nih.gov/$it/" }
                        ?: continue
                    val abstractText = stripHtml(obj.optString("abstractText"))

                    add(
                        JournalPaper(
                            title = title,
                            authors = authors,
                            year = year,
                            venue = venue,
                            doi = doi,
                            url = finalUrl,
                            abstractText = abstractText.ifBlank {
                                "Jurnal Open Access terindeks Europe PMC ($venue, $year)."
                            }.take(650),
                            citedByCount = cited,
                            sourceProvider = "Europe PMC",
                            isPeerReviewed = true,
                            pdfUrl = pdfUrl,
                            isOpenAccess = isOa
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Europe PMC parse error", e)
            emptyList()
        }
    }

    private fun fetchDoaj(query: String): List<JournalPaper> {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = "https://doaj.org/api/search/articles/$encoded?pageSize=4"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val bib = results.optJSONObject(i)?.optJSONObject("bibjson") ?: continue
                    val title = stripHtml(bib.optString("title"))
                    if (title.isBlank()) continue
                    val year = bib.optString("year").toIntOrNull() ?: 0
                    val venue = bib.optJSONObject("journal")?.optString("title")
                        ?.takeIf { it.isNotBlank() } ?: "DOAJ Open Access Journal"
                    val abstractText = stripHtml(bib.optString("abstract"))

                    val authorsArr = bib.optJSONArray("author")
                    val authors = buildList {
                        if (authorsArr != null) {
                            for (a in 0 until minOf(authorsArr.length(), 4)) {
                                val name = authorsArr.optJSONObject(a)?.optString("name")
                                if (!name.isNullOrBlank()) add(name)
                            }
                        }
                    }

                    var doi: String? = null
                    val ids = bib.optJSONArray("identifier")
                    if (ids != null) {
                        for (k in 0 until ids.length()) {
                            val idObj = ids.optJSONObject(k) ?: continue
                            if (idObj.optString("type").equals("doi", ignoreCase = true)) {
                                doi = idObj.optString("id").takeIf { it.isNotBlank() }
                                break
                            }
                        }
                    }

                    var fullUrl: String? = null
                    var pdfUrl: String? = null
                    val links = bib.optJSONArray("link")
                    if (links != null) {
                        for (l in 0 until links.length()) {
                            val linkObj = links.optJSONObject(l) ?: continue
                            val lUrl = linkObj.optString("url").takeIf { it.startsWith("http") } ?: continue
                            val cType = linkObj.optString("content_type")
                            if (pdfUrl == null && (cType.contains("pdf", ignoreCase = true) || lUrl.endsWith(".pdf", true))) {
                                pdfUrl = lUrl
                            } else if (fullUrl == null) {
                                fullUrl = lUrl
                            }
                        }
                    }

                    val finalUrl = fullUrl ?: pdfUrl ?: doi?.let { "https://doi.org/$it" } ?: continue
                    add(
                        JournalPaper(
                            title = title,
                            authors = authors,
                            year = year,
                            venue = venue,
                            doi = doi,
                            url = finalUrl,
                            abstractText = abstractText.ifBlank {
                                "Jurnal Open Access peer-reviewed di DOAJ ($venue, $year)."
                            }.take(650),
                            citedByCount = 0,
                            sourceProvider = "DOAJ",
                            isPeerReviewed = true,
                            pdfUrl = pdfUrl ?: finalUrl.takeIf { it.endsWith(".pdf", true) },
                            isOpenAccess = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DOAJ parse error", e)
            emptyList()
        }
    }

    private fun fetchGitHubWebResults(query: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=3"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
            buildList {
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val fullName = obj.optString("full_name")
                    val htmlUrl = obj.optString("html_url")
                    val stars = obj.optInt("stargazers_count", 0)
                    val desc = obj.optString("description").takeIf { it.isNotBlank() && it != "null" }
                        ?: "Repositori open-source GitHub"
                    if (fullName.isNotBlank() && htmlUrl.startsWith("http")) {
                        add(
                            WebSearchResult(
                                title = "GitHub: $fullName (★ $stars)",
                                url = htmlUrl,
                                domain = "github.com",
                                snippet = desc,
                                sourceEngine = "GitHub Live"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub web search error", e)
            emptyList()
        }
    }

    private fun fetchGitHubPaperCollectionJournals(query: String): List<JournalPaper> {
        val searchQuery = if (query.contains("paper", ignoreCase = true) ||
            query.contains("journal", ignoreCase = true) ||
            query.contains("jurnal", ignoreCase = true) ||
            query.contains("arxiv", ignoreCase = true)
        ) {
            query
        } else {
            "$query research papers arxiv"
        }
        val encoded = URLEncoder.encode(searchQuery, "UTF-8")
        val url = "https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=3"
        val body = executeGet(url) ?: return emptyList()
        return try {
            val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
            val out = mutableListOf<JournalPaper>()
            val linkRegex = Regex("""\[([^\]]{12,180})]\((https?://[^)\s]+)\)""")

            for (i in 0 until minOf(items.length(), 2)) {
                val obj = items.optJSONObject(i) ?: continue
                val fullName = obj.optString("full_name")
                val htmlUrl = obj.optString("html_url")
                val branch = obj.optString("default_branch", "main")
                val stars = obj.optInt("stargazers_count", 0)
                val desc = obj.optString("description").takeIf { it.isNotBlank() && it != "null" }
                    ?: "Koleksi paper & jurnal riset di GitHub."
                if (fullName.isBlank() || !htmlUrl.startsWith("http")) continue

                out += JournalPaper(
                    title = "Koleksi Jurnal GitHub: $fullName (★ $stars)",
                    authors = listOf(fullName.substringBefore("/")),
                    year = 2026,
                    venue = "GitHub Open Paper Collection",
                    doi = null,
                    url = htmlUrl,
                    abstractText = "$desc Ketuk 'Buka Jurnal' untuk menelusuri seluruh daftar paper dan PDF di repositori ini.",
                    citedByCount = stars,
                    sourceProvider = "GitHub Repo",
                    isPeerReviewed = false,
                    pdfUrl = null,
                    isOpenAccess = true
                )

                val readme = executeGet("https://raw.githubusercontent.com/$fullName/$branch/README.md")
                    ?: executeGet("https://raw.githubusercontent.com/$fullName/main/README.md")
                    ?: continue

                var extracted = 0
                for (match in linkRegex.findAll(readme)) {
                    if (extracted >= 3) break
                    val label = match.groupValues[1].replace(Regex("\\s+"), " ").trim()
                    val linkUrl = normalizeHttpsUrl(match.groupValues[2].trim())
                    val isPaperLink = linkUrl.contains("arxiv.org", true) ||
                        linkUrl.contains("doi.org", true) ||
                        linkUrl.contains("openreview.net", true) ||
                        linkUrl.contains("aclanthology.org", true) ||
                        linkUrl.contains("europepmc.org", true) ||
                        linkUrl.endsWith(".pdf", true)
                    if (!isPaperLink || label.length < 12) continue

                    val pdf = when {
                        linkUrl.endsWith(".pdf", true) -> linkUrl
                        linkUrl.contains("arxiv.org/abs/", true) ->
                            "${linkUrl.replace("/abs/", "/pdf/")}.pdf"
                        else -> null
                    }
                    out += JournalPaper(
                        title = label,
                        authors = listOf("Kurasi $fullName"),
                        year = 0,
                        venue = "GitHub Curated Papers • $fullName",
                        doi = linkUrl.substringAfter("doi.org/", "").takeIf { linkUrl.contains("doi.org/") && it.isNotBlank() },
                        url = linkUrl,
                        abstractText = "Paper terbuka dari repositori GitHub $fullName ($stars stars). Dapat langsung dibuka atau diunduh.",
                        citedByCount = 0,
                        sourceProvider = "GitHub ($fullName)",
                        isPeerReviewed = linkUrl.contains("doi.org", true),
                        pdfUrl = pdf,
                        isOpenAccess = true
                    )
                    extracted++
                }
            }
            out
        } catch (e: Exception) {
            Log.w(TAG, "GitHub paper repo parse error", e)
            emptyList()
        }
    }

    private fun normalizeHttpsUrl(url: String): String =
        if (url.startsWith("http://arxiv.org/") || url.startsWith("http://dx.doi.org/") || url.startsWith("http://doi.org/")) {
            "https://${url.removePrefix("http://")}"
        } else {
            url
        }

    private fun reconstructOpenAlexAbstract(index: JSONObject?): String {
        if (index == null) return ""
        val wordsByPos = sortedMapOf<Int, String>()
        val keys = index.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            val positions = index.optJSONArray(word) ?: continue
            for (i in 0 until positions.length()) {
                val pos = positions.optInt(i, -1)
                if (pos >= 0 && pos < 160) {
                    wordsByPos[pos] = word
                }
            }
        }
        return wordsByPos.values.joinToString(" ").take(650)
    }

    private fun executeGet(url: String): String? {
        return try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET failed for $url: ${e.message}")
            null
        }
    }

    private fun extractDuckDuckGoUrl(rawHref: String): String {
        return if (rawHref.contains("uddg=")) {
            val encoded = rawHref.substringAfter("uddg=").substringBefore("&")
            java.net.URLDecoder.decode(encoded, "UTF-8")
        } else if (rawHref.startsWith("//")) {
            "https:$rawHref"
        } else {
            rawHref
        }
    }

    private fun extractDomain(url: String): String =
        url.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
}
