package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.nodes.Document
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * WTR-LAB — Chinese-to-English machine-translated novel source.
 *
 * Native Kotlin implementation ported from wtr_lab_scraper.py.
 *
 * Key features ported from the Python script:
 *   1. **Local AES-256-GCM decryption** of chapter bodies using the key
 *      extracted from the site's Next.js chunk — no proxy needed.
 *   2. **Next.js __NEXT_DATA__ JSON parsing** for catalog/search/info pages.
 *   3. **Retry with `retry: true`** on translator warmup misses.
 *   4. **Glossary marker substitution** (※{idx}⛬ and ※{idx}〓).
 *   5. **URL format**: `/{locale}/novel/{id}/{slug}` with locale prefix.
 *
 * The Cloudflare bypass is handled by the existing OkHttp interceptor chain
 * (BrowserHeadersInterceptor + CloudFareVerificationInterceptor).
 */
class Wtrlab(
    private val networkClient: NetworkClient,
) : SourceInterface.Catalog {
    override val id = "wtrlab"
    override val nameStrId = R.string.source_name_wtrlab
    override val baseUrl = "https://wtr-lab.com/"
    override val catalogUrl = "https://wtr-lab.com/en/novel-list"
    override val language = LanguageCode.ENGLISH
    override val iconUrl = "https://wtr-lab.com/favicon.ico"

    companion object {
        private const val BASE_URL = "https://wtr-lab.com"
        private const val LOCALE = "en"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        // AES-256-GCM key extracted from wtr-lab.com's Next.js chunk
        // /_next/static/chunks/f1284758969025b7.js
        private val AES_KEY = "IJAFUUxjM25hyzL2AZrn0wl7cESED6Ru".toByteArray()

        // Fallback proxy (used if local decryption fails)
        private const val PROXY_URL = "https://wtr-lab-proxy.fly.dev/chapter"
    }

    // ─── Catalog ──────────────────────────────────────────────────────

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val page = index + 1
                val url = "$BASE_URL/$LOCALE/novel-list?page=$page"
                val html = fetchText(url)
                val nextData = extractNextData(html)
                val series = nextData
                    .getAsJsonObject("props")
                    ?.getAsJsonObject("pageProps")
                    ?.getAsJsonArray("series") ?: return@tryConnect PagedList.createEmpty(index)

                val items = mutableListOf<BookResult>()
                for (i in 0 until series.size()) {
                    val n = series[i].asJsonObject
                    val rawId = n.get("raw_id")?.asString ?: continue
                    val data = n.getAsJsonObject("data") ?: n
                    val title = data.get("title")?.asString ?: n.get("search_text")?.asString ?: continue
                    val slug = n.get("slug")?.asString ?: ""
                    val cover = data.get("image")?.asString ?: ""
                    val bookUrl = "$BASE_URL/$LOCALE/novel/$rawId/$slug"
                    items.add(BookResult(title = title, url = bookUrl, coverImageUrl = cover))
                }

                PagedList(list = items, index = index, isLastPage = items.isEmpty())
            }
        }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                if (input.isBlank()) return@tryConnect PagedList.createEmpty(index)
                val page = index + 1
                val encodedQuery = java.net.URLEncoder.encode(input, "UTF-8")
                val url = "$BASE_URL/$LOCALE/novel-finder?text=$encodedQuery&page=$page"
                val html = fetchText(url)
                val nextData = extractNextData(html)
                val series = nextData
                    .getAsJsonObject("props")
                    ?.getAsJsonObject("pageProps")
                    ?.getAsJsonArray("series") ?: return@tryConnect PagedList.createEmpty(index)

                val items = mutableListOf<BookResult>()
                for (i in 0 until series.size()) {
                    val n = series[i].asJsonObject
                    val rawId = n.get("raw_id")?.asString ?: continue
                    val data = n.getAsJsonObject("data") ?: n
                    val title = data.get("title")?.asString ?: n.get("search_text")?.asString ?: continue
                    val slug = n.get("slug")?.asString ?: ""
                    val cover = data.get("image")?.asString ?: ""
                    val bookUrl = "$BASE_URL/$LOCALE/novel/$rawId/$slug"
                    items.add(BookResult(title = title, url = bookUrl, coverImageUrl = cover))
                }

                PagedList(list = items, index = index, isLastPage = items.isEmpty())
            }
        }

    // ─── Book metadata ────────────────────────────────────────────────

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val html = fetchText(bookUrl)
                val nextData = extractNextData(html)
                val serie = nextData
                    .getAsJsonObject("props")
                    ?.getAsJsonObject("pageProps")
                    ?.getAsJsonObject("serie") ?: return@tryConnect null
                val sd = serie.getAsJsonObject("serie_data") ?: return@tryConnect null
                val data = sd.getAsJsonObject("data") ?: return@tryConnect null
                data.get("image")?.asString
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val html = fetchText(bookUrl)
                val nextData = extractNextData(html)
                val serie = nextData
                    .getAsJsonObject("props")
                    ?.getAsJsonObject("pageProps")
                    ?.getAsJsonObject("serie") ?: return@tryConnect null
                val sd = serie.getAsJsonObject("serie_data") ?: return@tryConnect null
                val data = sd.getAsJsonObject("data") ?: return@tryConnect null
                data.get("description")?.asString
            }
        }

    // ─── Chapter list ────────────────────────────────────────────────

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val (rawId, slug) = parseNovelUrl(bookUrl) ?: return@tryConnect emptyList()
                val apiUrl = "$BASE_URL/api/chapters/$rawId"
                val novelUrl = "$BASE_URL/$LOCALE/novel/$rawId/$slug"

                val response = networkClient.get(apiUrl)
                val jsonBody = response.body?.string() ?: return@tryConnect emptyList()
                response.close()

                val data = JsonParser.parseString(jsonBody).asJsonObject
                val chaptersData = data.getAsJsonArray("chapters") ?: return@tryConnect emptyList()

                val chapters = mutableListOf<ChapterResult>()
                for (i in 0 until chaptersData.size()) {
                    val ch = chaptersData[i].asJsonObject
                    val order = ch.get("order")?.asInt ?: (i + 1)
                    val title = ch.get("title")?.asString ?: "Chapter $order"
                    val chUrl = "$BASE_URL/$LOCALE/novel/$rawId/$slug/chapter-$order"
                    chapters.add(ChapterResult(
                        title = "$order: $title",
                        url = chUrl
                    ))
                }
                chapters
            }
        }

    // ─── Chapter text ────────────────────────────────────────────────

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            // The chapter title is in the page's h1 or in __NEXT_DATA__
            doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
        }

    override suspend fun getChapterText(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val chapterUrl = doc.location().ifBlank {
                doc.selectFirst("link[rel='canonical']")?.attr("href") ?: ""
            }
            if (chapterUrl.isBlank()) return@withContext ""

            val (rawId, slug, chapterNo) = parseChapterUrl(chapterUrl) ?: return@withContext ""
            val canonicalUrl = "$BASE_URL/$LOCALE/novel/$rawId/$slug/chapter-$chapterNo"

            // Build request body — try with retry=false first, then retry=true
            val bodyPayload = """{"translate":"ai","language":"none","raw_id":"$rawId","chapter_no":$chapterNo,"retry":false,"force_retry":false}"""
            val retryPayload = """{"translate":"ai","language":"none","raw_id":"$rawId","chapter_no":$chapterNo,"retry":true,"force_retry":false}"""

            // First attempt
            var data = postReaderGet(canonicalUrl, bodyPayload)

            // Check if translator warmup failed — retry with retry=true
            if (data != null && data.get("success")?.asBoolean == false) {
                val errorMsg = data.get("error")?.asString ?: ""
                if ("translat" in errorMsg.lowercase()) {
                    delay(1000)
                    data = postReaderGet(canonicalUrl, retryPayload)
                }
            }

            if (data == null) return@withContext ""
            if (data.get("success")?.asBoolean == false) return@withContext ""

            // Navigate: json.data.data
            val outerData = data.getAsJsonObject("data") ?: return@withContext ""
            val innerData = outerData.getAsJsonObject("data") ?: outerData
            val bodyElement = innerData.get("body") ?: return@withContext ""

            // Handle body: it can be a string (encrypted or plaintext) or a
            // JSON array (already-decoded paragraphs, rare in ai mode).
            if (bodyElement.isJsonArray) {
                // Body is already a non-encrypted JSON array of paragraph strings
                val paragraphs = bodyElement.asJsonArray
                    .map { it.asString }
                    .filter { it.isNotBlank() }
                    .map { cleanParagraph(it) }
                    .filter { it.isNotBlank() }
                return@withContext paragraphs.joinToString("\n\n")
            }

            // Body is a string — may be encrypted (arr:... / str:...) or plaintext
            val rawBodyStr = bodyElement.asString
            if (rawBodyStr.isBlank() || rawBodyStr == "null") return@withContext ""

            val decrypted = decryptBody(rawBodyStr)

            // Build paragraphs
            val paragraphs = buildParagraphs(decrypted)

            // Apply glossary (for ai mode — we use web mode, but still check)
            val glossaryData = innerData.getAsJsonObject("glossary_data")
            val finalParagraphs = if (glossaryData != null) {
                applyGlossary(paragraphs, glossaryData)
            } else {
                paragraphs
            }

            // Clean paragraphs
            finalParagraphs.map { cleanParagraph(it) }
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        }

    // ─── AES-256-GCM Decryption ──────────────────────────────────────

    /**
     * Decrypt an `arr:...` / `str:...` payload from /api/reader/get.
     *
     * The format is: `arr:<iv_b64>:<tag_b64>:<ct_b64>` or `str:<iv_b64>:<tag_b64>:<ct_b64>`
     * Decrypted with AES-256-GCM using the key extracted from the site's JS.
     *
     * Returns a List<String> (for arr:) or String (for str:).
     */
    private suspend fun decryptBody(raw: String): Any {
        if (!raw.startsWith("arr:") && !raw.startsWith("str:")) {
            return raw // plaintext
        }

        val isArr = raw.startsWith("arr:")
        val payload = raw.substring(4)
        val parts = payload.split(":")
        if (parts.size != 3) return raw

        val iv = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
        val tag = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
        val ct = android.util.Base64.decode(parts[2], android.util.Base64.DEFAULT)

        // Try local AES-GCM decryption first
        try {
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            // AES-GCM in Java: ciphertext = ct + tag (appended)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(tag.size * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            // Java's GCM expects the tag appended to the ciphertext
            val ctWithTag = ct + tag
            val plaintext = cipher.doFinal(ctWithTag)
            val text = String(plaintext, Charsets.UTF_8)
            return if (isArr) {
                val parsed = JsonParser.parseString(text)
                if (parsed.isJsonArray) {
                    parsed.asJsonArray.map { it.asString }
                } else {
                    listOf(text)
                }
            } else {
                text
            }
        } catch (e: Exception) {
            Timber.w(e, "[wtrlab] local AES-GCM decrypt failed, trying proxy")
        }

        // Fallback: round-trip through the public proxy
        return try {
            val proxyBody = """{"payload":"$raw"}""".toRequestBody(JSON_MEDIA_TYPE.toMediaType())
            val proxyRequest = postRequest(PROXY_URL, body = proxyBody)
                .addHeader("Content-Type", "application/json")
            val proxyResponse = networkClient.call(proxyRequest)
            val proxyRespBody = proxyResponse.body?.string() ?: return raw
            proxyResponse.close()

            val result = JsonParser.parseString(proxyRespBody)
            when {
                result.isJsonArray -> result.asJsonArray.map { it.asString }
                result.isJsonObject && result.asJsonObject.has("body") ->
                    result.asJsonObject.get("body").let {
                        if (it.isJsonArray) it.asJsonArray.map { e -> e.asString }
                        else it.asString
                    }
                else -> raw
            }
        } catch (e: Exception) {
            Timber.e(e, "[wtrlab] proxy decryption also failed")
            raw
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private suspend fun fetchText(url: String): String {
        val response = networkClient.get(url)
        val body = response.body?.string() ?: ""
        response.close()
        return body
    }

    private fun extractNextData(html: String): com.google.gson.JsonObject {
        val regex = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(html) ?: throw RuntimeException("could not find __NEXT_DATA__ script")
        return JsonParser.parseString(match.groupValues[1]).asJsonObject
    }

    private fun parseNovelUrl(url: String): Pair<String, String>? {
        val regex = Regex("""/novel/(\d+)/([^/?#]+)""")
        val match = regex.find(url) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    private fun parseChapterUrl(url: String): Triple<String, String, Int>? {
        val regex = Regex("""/novel/(\d+)/([^/?#]+)/chapter-(\d+)""")
        val match = regex.find(url) ?: return null
        return Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3].toInt())
    }

    private suspend fun postReaderGet(chapterUrl: String, payload: String): com.google.gson.JsonObject? {
        return try {
            val request = postRequest("$BASE_URL/api/reader/get", body = payload.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", chapterUrl)
                .addHeader("Origin", BASE_URL)
                .addHeader("Accept", "application/json, text/plain, */*")

            val response = networkClient.call(request)
            val respBody = response.body?.string()
            response.close()
            if (respBody.isNullOrBlank()) null
            else JsonParser.parseString(respBody).asJsonObject
        } catch (e: Exception) {
            Timber.e(e, "[wtrlab] POST /api/reader/get failed")
            null
        }
    }

    private fun buildParagraphs(decrypted: Any): List<String> {
        return when (decrypted) {
            is List<*> -> decrypted.filterIsInstance<String>().filter { it.isNotBlank() }
            is String -> decrypted.split("\n").filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun cleanParagraph(text: String): String {
        var result = text.trim('\uFEFF', ' ', '\t', '\r', '\n')
        // Strip "Chapter N" / "第N章" prefix
        result = Regex("""^\s*(Chapter\s+\d+|第\s*\d+\s*章)[^\n\r]*""").replace(result, "").trim()
        // Strip translator/editor watermarks
        result = Regex("""^\s*(Translator|Editor|Proofreader|Read\s+(at|on|latest))\s*[:\s][^\n\r]{0,70}""", RegexOption.IGNORE_CASE).replace(result, "").trim()
        return result
    }

    private fun applyGlossary(paragraphs: List<String>, glossaryData: com.google.gson.JsonObject): List<String> {
        val terms = glossaryData.getAsJsonArray("terms") ?: return paragraphs
        if (terms.size() == 0) return paragraphs

        // Build {idx: term} mapping (0-based index)
        val glossary = mutableMapOf<Int, String>()
        for (i in 0 until terms.size()) {
            val entry = terms[i].asJsonArray
            if (entry.size() > 0) {
                glossary[i] = entry[0].asString
            }
        }

        return paragraphs.map { p ->
            var text = p
            for ((idx, term) in glossary) {
                if (term.isNotBlank()) {
                    // Two marker styles: ※{idx}⛬ (U+26EC) and ※{idx}〓 (U+3013)
                    text = text.replace("※${idx}\u26ec", term)
                    text = text.replace("※${idx}\u3013", term)
                }
            }
            text
        }
    }
}
