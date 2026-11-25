package com.example.network

import android.util.Base64
import com.example.model.CalendarEventItem
import com.example.model.DriveFileItem
import com.example.model.GmailMessageItem
import com.example.model.GoogleContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class WorkspaceAuthExpiredException(message: String) : IllegalStateException(message)

class GoogleWorkspaceClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ==================== GMAIL ====================

    suspend fun fetchRecentEmails(
        accessToken: String,
        maxResults: Int = 10,
    ): Result<List<GmailMessageItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val listJson = executeJsonRequest(
                accessToken = accessToken,
                url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$maxResults"
            )
            val messages = listJson.optJSONArray("messages") ?: return@runCatching emptyList()
            val results = mutableListOf<GmailMessageItem>()
            for (i in 0 until minOf(messages.length(), maxResults)) {
                val id = messages.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isBlank()) continue
                val detail = executeJsonRequest(
                    accessToken = accessToken,
                    url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date"
                )
                val payload = detail.optJSONObject("payload")
                val headers = payload?.optJSONArray("headers")
                var subject = "(Tanpa Subjek)"
                var from = ""
                var date = ""
                if (headers != null) {
                    for (h in 0 until headers.length()) {
                        val headerObj = headers.optJSONObject(h) ?: continue
                        when (headerObj.optString("name")) {
                            "Subject" -> subject = headerObj.optString("value").ifBlank { subject }
                            "From" -> from = headerObj.optString("value")
                            "Date" -> date = headerObj.optString("value")
                        }
                    }
                }
                results += GmailMessageItem(
                    id = id,
                    threadId = detail.optString("threadId"),
                    subject = subject,
                    from = from,
                    date = date,
                    snippet = detail.optString("snippet")
                )
            }
            results
        }
    }

    suspend fun sendEmail(
        accessToken: String,
        to: String,
        subject: String,
        body: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTo = to.trim()
            require(cleanTo.contains("@") && cleanTo.contains(".")) {
                "Alamat email tujuan tidak valid."
            }
            require(subject.isNotBlank() && body.isNotBlank()) {
                "Subjek dan isi email wajib diisi."
            }
            val rawMime = buildString {
                append("To: ").append(cleanTo).append("\r\n")
                append("Subject: ").append(subject.trim()).append("\r\n")
                append("Content-Type: text/plain; charset=utf-8\r\n")
                append("MIME-Version: 1.0\r\n\r\n")
                append(body.trim())
            }
            val encoded = Base64.encodeToString(
                rawMime.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val payload = JSONObject().put("raw", encoded).toString()
            val response = executeJsonRequest(
                accessToken = accessToken,
                url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
                method = "POST",
                jsonBody = payload
            )
            response.optString("id", "sent")
        }
    }

    // ==================== GOOGLE DRIVE ====================

    suspend fun listDriveFiles(
        accessToken: String,
        pageSize: Int = 15,
    ): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeJsonRequest(
                accessToken = accessToken,
                url = "https://www.googleapis.com/drive/v3/files?pageSize=$pageSize&orderBy=modifiedTime%20desc&fields=files(id,name,mimeType,modifiedTime,webViewLink)"
            )
            val files = json.optJSONArray("files") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until files.length()) {
                    val obj = files.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    add(
                        DriveFileItem(
                            id = id,
                            name = obj.optString("name", "Dokumen"),
                            mimeType = obj.optString("mimeType"),
                            modifiedTime = obj.optString("modifiedTime"),
                            webViewLink = obj.optString("webViewLink", "https://drive.google.com/file/d/$id/view")
                        )
                    )
                }
            }
        }
    }

    suspend fun uploadFileToDrive(
        accessToken: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<DriveFileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = JSONObject().put("name", fileName).toString()
            val multipart = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadata.toRequestBody(jsonMediaType))
                .addPart(bytes.toRequestBody(mimeType.toMediaType()))
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,modifiedTime,webViewLink")
                .header("Authorization", "Bearer $accessToken")
                .post(multipart)
                .build()

            val body = http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    throw WorkspaceAuthExpiredException("Sesi Google Workspace habis. Ketuk Hubungkan Ulang.")
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Upload Drive gagal (HTTP ${resp.code}): ${extractApiError(text)}")
                }
                JSONObject(text)
            }
            val id = body.optString("id")
            DriveFileItem(
                id = id,
                name = body.optString("name", fileName),
                mimeType = body.optString("mimeType", mimeType),
                modifiedTime = body.optString("modifiedTime"),
                webViewLink = body.optString("webViewLink", "https://drive.google.com/file/d/$id/view")
            )
        }
    }

    // ==================== GOOGLE DOCS ====================

    suspend fun createGoogleDoc(
        accessToken: String,
        title: String,
        content: String,
    ): Result<DriveFileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val createPayload = JSONObject()
                .put("title", title.trim().ifBlank { "Laporan Riset Kafka" })
                .toString()
            val created = executeJsonRequest(
                accessToken = accessToken,
                url = "https://docs.googleapis.com/v1/documents",
                method = "POST",
                jsonBody = createPayload
            )
            val docId = created.optString("documentId")
            require(docId.isNotBlank()) { "Document ID tidak dikembalikan oleh Google Docs API." }

            if (content.isNotBlank()) {
                val requestsArray = JSONArray().put(
                    JSONObject().put(
                        "insertText",
                        JSONObject()
                            .put("location", JSONObject().put("index", 1))
                            .put("text", content.take(45000))
                    )
                )
                executeJsonRequest(
                    accessToken = accessToken,
                    url = "https://docs.googleapis.com/v1/documents/$docId:batchUpdate",
                    method = "POST",
                    jsonBody = JSONObject().put("requests", requestsArray).toString()
                )
            }

            DriveFileItem(
                id = docId,
                name = created.optString("title", title),
                mimeType = "application/vnd.google-apps.document",
                webViewLink = "https://docs.google.com/document/d/$docId/edit"
            )
        }
    }

    // ==================== GOOGLE SHEETS ====================

    suspend fun createGoogleSheet(
        accessToken: String,
        title: String,
        rows: List<List<String>>,
    ): Result<DriveFileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put(
                "properties",
                JSONObject().put("title", title.trim().ifBlank { "Data Riset Kafka" })
            ).toString()
            val created = executeJsonRequest(
                accessToken = accessToken,
                url = "https://sheets.googleapis.com/v4/spreadsheets",
                method = "POST",
                jsonBody = payload
            )
            val sheetId = created.optString("spreadsheetId")
            require(sheetId.isNotBlank()) { "Spreadsheet ID tidak dikembalikan oleh Google Sheets API." }

            if (rows.isNotEmpty()) {
                val valuesJson = JSONArray()
                rows.forEach { row ->
                    val rowArr = JSONArray()
                    row.forEach { cell -> rowArr.put(cell) }
                    valuesJson.put(rowArr)
                }
                val appendPayload = JSONObject().put("values", valuesJson).toString()
                executeJsonRequest(
                    accessToken = accessToken,
                    url = "https://sheets.googleapis.com/v4/spreadsheets/$sheetId/values/A1:append?valueInputOption=USER_ENTERED",
                    method = "POST",
                    jsonBody = appendPayload
                )
            }

            DriveFileItem(
                id = sheetId,
                name = title.trim().ifBlank { "Data Riset Kafka" },
                mimeType = "application/vnd.google-apps.spreadsheet",
                webViewLink = created.optString("spreadsheetUrl", "https://docs.google.com/spreadsheets/d/$sheetId/edit")
            )
        }
    }

    // ==================== GOOGLE CALENDAR ====================

    suspend fun listUpcomingEvents(
        accessToken: String,
        maxResults: Int = 10,
    ): Result<List<CalendarEventItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val nowIso = isoUtcString(System.currentTimeMillis())
            val json = executeJsonRequest(
                accessToken = accessToken,
                url = "https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&timeMin=$nowIso&maxResults=$maxResults"
            )
            val items = json.optJSONArray("items") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until items.length()) {
                    val obj = items.optJSONObject(i) ?: continue
                    val startObj = obj.optJSONObject("start")
                    val endObj = obj.optJSONObject("end")
                    val startStr = startObj?.optString("dateTime")?.ifBlank { null }
                        ?: startObj?.optString("date").orEmpty()
                    val endStr = endObj?.optString("dateTime")?.ifBlank { null }
                        ?: endObj?.optString("date").orEmpty()
                    add(
                        CalendarEventItem(
                            id = obj.optString("id"),
                            summary = obj.optString("summary", "(Tanpa Judul)"),
                            startTime = startStr,
                            endTime = endStr,
                            description = obj.optString("description"),
                            htmlLink = obj.optString("htmlLink")
                        )
                    )
                }
            }
        }
    }

    suspend fun createCalendarEvent(
        accessToken: String,
        summary: String,
        description: String,
        startMillis: Long,
        durationMinutes: Int = 45,
    ): Result<CalendarEventItem> = withContext(Dispatchers.IO) {
        runCatching {
            val endMillis = startMillis + durationMinutes * 60_000L
            val payload = JSONObject()
                .put("summary", summary.trim().ifBlank { "Sesi Riset Kafka" })
                .put("description", description.trim())
                .put("start", JSONObject().put("dateTime", isoUtcString(startMillis)))
                .put("end", JSONObject().put("dateTime", isoUtcString(endMillis)))
                .toString()

            val obj = executeJsonRequest(
                accessToken = accessToken,
                url = "https://www.googleapis.com/calendar/v3/calendars/primary/events",
                method = "POST",
                jsonBody = payload
            )
            CalendarEventItem(
                id = obj.optString("id"),
                summary = obj.optString("summary", summary),
                startTime = obj.optJSONObject("start")?.optString("dateTime").orEmpty(),
                endTime = obj.optJSONObject("end")?.optString("dateTime").orEmpty(),
                description = obj.optString("description"),
                htmlLink = obj.optString("htmlLink")
            )
        }
    }

    // ==================== GOOGLE CONTACTS ====================

    suspend fun listContacts(
        accessToken: String,
        pageSize: Int = 20,
    ): Result<List<GoogleContactItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = executeJsonRequest(
                accessToken = accessToken,
                url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses&pageSize=$pageSize"
            )
            val connections = json.optJSONArray("connections") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until connections.length()) {
                    val person = connections.optJSONObject(i) ?: continue
                    val name = person.optJSONArray("names")
                        ?.optJSONObject(0)
                        ?.optString("displayName")
                        .orEmpty()
                    val email = person.optJSONArray("emailAddresses")
                        ?.optJSONObject(0)
                        ?.optString("value")
                        .orEmpty()
                    if (name.isNotBlank() || email.isNotBlank()) {
                        add(
                            GoogleContactItem(
                                resourceName = person.optString("resourceName", "contact_$i"),
                                displayName = name.ifBlank { email },
                                email = email
                            )
                        )
                    }
                }
            }
        }
    }

    // ==================== INTERNAL HELPERS ====================

    private fun executeJsonRequest(
        accessToken: String,
        url: String,
        method: String = "GET",
        jsonBody: String? = null,
    ): JSONObject {
        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")

        if (method == "POST") {
            builder.post((jsonBody ?: "{}").toRequestBody(jsonMediaType))
        } else {
            builder.get()
        }

        return http.newCall(builder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (resp.code == 401) {
                throw WorkspaceAuthExpiredException("Token Google Workspace kedaluwarsa. Hubungkan ulang akun Anda.")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("Google API error (HTTP ${resp.code}): ${extractApiError(raw)}")
            }
            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }

    private fun extractApiError(raw: String): String {
        return try {
            val errObj = JSONObject(raw).optJSONObject("error")
            errObj?.optString("message")?.takeIf { it.isNotBlank() } ?: raw.take(180)
        } catch (_: Exception) {
            raw.take(180).ifBlank { "Permintaan ditolak oleh server Google." }
        }
    }

    private fun isoUtcString(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}
