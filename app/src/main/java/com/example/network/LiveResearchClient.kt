package com.example.network

import android.util.Log
import com.example.model.GitHubSkillRepo
import com.example.model.JournalPaper
import com.example.model.WebSearchResult
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

private const val TAG = "LiveResearchClient"

class LiveResearchClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "KafkaAgent/1.0 (Android; Research & Verification)")
                .header("Accept", "application/json, text/html, application/xml;q=0.9")
                .build()
            chain.proceed(req)
        }
        .build()

    suspend fun searchLiveWeb(query: String, limit: Int = 10): List<WebSearchResult> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            coroutineScope {
                val ddgDeferred = async { fetchDuckDuckGoHtml(trimmed) }
                val wikiEnDeferred = async { fetchWikipediaSearch(trimmed, "en") }
                val wikiIdDeferred = async { fetchWikipediaSearch(trimmed, "id") }
                val hnDeferred = async { fetchHackerNewsAlgolia(trimmed) }

                val combined = mutableListOf<WebSearchResult>()
                combined += ddgDeferred.await()
                combined += wikiEnDeferred.await()
                combined += wikiIdDeferred.await()
                combined += hnDeferred.await()

                combined
                    .distinctBy { it.url.trimEnd('/') }
                    .take(limit)
            }
        }

    suspend fun searchAcademicJournals(query: String, limit: Int = 12): List<JournalPaper> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext emptyList()

            coroutineScope {
                val openAlexDeferred = async { fetchOpenAlex(trimmed) }
                val arxivDeferred = async { fetchArxiv(trimmed) }
                val crossrefDeferred = async { fetchCrossref(trimmed) }

                val merged = mutableListOf<JournalPaper>()
                merged += openAlexDeferred.await()
                merged += arxivDeferred.await()
                merged += crossrefDeferred.await()

                merged
                    .distinctBy { (it.doi ?: it.title.lowercase()).trim() }
                    .sortedByDescending { it.citedByCount }
                    .take(limit)
            }
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

    suspend fun verifyUrlReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return@withContext false
        try {
            val headReq = Request.Builder().url(url).head().build()
            client.newCall(headReq).execute().use { resp ->
                if (resp.isSuccessful || resp.code in 300..399) return@withContext true
            }
            val getReq = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1024")
                .build()
            client.newCall(getReq).execute().use { resp ->
                resp.isSuccessful || resp.code == 416
            }
        } catch (_: Exception) {
            false
        }
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
                    val oaUrl = obj.optJSONObject("open_access")?.optString("oa_url")
                        ?.takeIf { it.startsWith("http") }
                    val landingUrl = obj.optJSONObject("primary_location")
                        ?.optString("landing_page_url")
                        ?.takeIf { it.startsWith("http") }
                    val finalUrl = oaUrl ?: landingUrl ?: doi?.let { "https://doi.org/$it" }
                        ?: obj.optString("id")

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
                            isPeerReviewed = true
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
                                papers += JournalPaper(
                                    title = title,
                                    authors = authors.take(4),
                                    year = year,
                                    venue = "arXiv Preprint Repository",
                                    doi = null,
                                    url = idUrl,
                                    abstractText = summary.take(500),
                                    citedByCount = 0,
                                    sourceProvider = "arXiv",
                                    isPeerReviewed = false
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
                            isPeerReviewed = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crossref parse error", e)
            emptyList()
        }
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
