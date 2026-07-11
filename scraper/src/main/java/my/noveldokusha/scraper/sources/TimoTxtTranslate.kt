package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.addPath
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import okhttp3.FormBody
import org.jsoup.nodes.Document
import java.net.URI

/**
 * Auto-translating source for timotxt.com via the Google Translate free API.
 *
 * URL routing: Uses `https://www-timotxt-com.translate.goog/` as the baseUrl
 * so `getCompatibleSource()` can distinguish this source's books from the
 * plain TimoTxt source. This domain is a routing key only — all actual HTTP
 * fetching goes to `https://www.timotxt.com/`.
 *
 * Fetching (OkHttp): `transformChapterUrl()` converts the stored
 * translate.goog URL back to timotxt.com before each fetch. The CF
 * interceptors handle any Cloudflare challenges. The app receives Chinese
 * HTML and translates it via the Google Translate API.
 *
 * WebView: `transformWebviewUrl()` converts the stored translate.goog URL
 * to the translate.goog proxy WITH `_x_tr_*` params. The proxy's JavaScript
 * translates the page to English in the browser.
 *
 * Translation pipeline (mirrors timotxt_extractor.py):
 *   1. Fetch Chinese HTML from timotxt.com.
 *   2. Extract text, strip notices, remove Unicode garbage.
 *   3. Batch into ≤4500 char chunks at sentence boundaries.
 *   4. POST each batch to Google Translate API (`client=gtx`).
 *   5. Clean up English junk patterns.
 */
class TimoTxtTranslate(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "timotxt_translate"
    override val nameStrId = R.string.source_name_timotxt_translate

    override val baseUrl = "https://www-timotxt-com.translate.goog/"
    override val catalogUrl = "https://www-timotxt-com.translate.goog/"
    override val language = LanguageCode.ENGLISH

    private val originalBaseUrl = "https://www.timotxt.com/"

    companion object {
        private const val TRANSLATE_API_URL =
            "https://translate.googleapis.com/translate_a/single"
        private const val BATCH_CHAR_LIMIT = 4500
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1500L
        private const val TRANSLATE_DELAY_MS = 300L
        private const val TITLE_TRANSLATE_MAX_RETRIES = 1

        private const val TRANSLATE_PARAMS =
            "_x_tr_sl=zh-CN&_x_tr_tl=en&_x_tr_hl=en&_x_tr_pto=wapp"

        internal val STRIP_PATTERNS = listOf(
            Regex("溫馨提示.*"),
            Regex("網站即將改版.*"),
            Regex("書架.*閱讀記錄.*"),
            Regex("手機端閱讀.*"),
            Regex("PS[：:].*"),
            Regex("嘤嘤嘤.*"),
            Regex("手機小說閱讀網.*"),
            Regex("手機用戶.*"),
            Regex("最新更新時間.*"),
            Regex("提莫書屋.*"),
            Regex("(?i)timotxt\\.com.*"),
        )

        internal val UNICODE_WHITELIST = Regex(
            "[^\\u0000-\\u007F" +
            "\\u00A0-\\u00FF" +
            "\\u2000-\\u206F" +
            "\\u3000-\\u303F" +
            "\\uFF00-\\uFFEF" +
            "\\u4E00-\\u9FFF" +
            "\\u3400-\\u4DBF" +
            "\\uF900-\\uFAFF" +
            "\\u2E80-\\u2EFF" +
            "\\u2014\\u2013" +
            "\\u201C\\u201D" +
            "\\u2018\\u2019" +
            "\\u2026" +
            "\\u3001\\u3002" +
            "]+"
        )

        internal val ENGLISH_JUNK_PATTERNS = listOf(
            Regex("(?i)warm reminder.*"),
            Regex("(?i)warm tip.*"),
            Regex("(?i)friendly reminder.*"),
            Regex("(?i)mobile novel reading network.*"),
            Regex("(?i)mobile phone novel.*"),
            Regex("(?i)mobile user.*"),
            Regex("(?i)latest update time.*"),
            Regex("(?i)timo book house.*"),
            Regex("(?i)timotxt\\.com.*"),
        )

        fun isPrimarilyCJK(text: String): Boolean {
            if (text.isBlank()) return false
            val cjkCount = text.count { ch ->
                val code = ch.code
                (code in 0x4E00..0x9FFF) ||
                (code in 0x3400..0x4DBF) ||
                (code in 0x2E80..0x2EFF) ||
                (code in 0x3000..0x303F) ||
                (code in 0xFF00..0xFFEF) ||
                (code in 0x3040..0x309F) ||
                (code in 0x30A0..0x30FF)
            }
            return cjkCount.toDouble() / text.length > 0.12
        }
    }

    /**
     * Convert a stored translate.goog URL to the real timotxt.com URL for
     * OkHttp fetching. Strips any Google Translate query params.
     */
    private fun resolveOriginalUrl(url: String): String = url
        .replace("https://www-timotxt-com.translate.goog", originalBaseUrl.trimEnd('/'))
        .replace(Regex("[?&]_x_tr_(sl|tl|hl|pto|pto_ctx)=[^&]*"), "")
        .replace("?&", "?")
        .replace(Regex("[?&]$"), "")

    /** OkHttp fetch: convert stored translate.goog → timotxt.com. */
    override suspend fun transformChapterUrl(url: String): String = resolveOriginalUrl(url)

    /** WebView: convert stored URL → translate.goog with params. */
    override suspend fun transformWebviewUrl(url: String): String {
        if (url.isBlank()) return url
        var cleanUrl = url
            .replace(Regex("[?&]_x_tr_(sl|tl|hl|pto|pto_ctx)=[^&]*"), "")
            .replace("?&", "?")
            .replace(Regex("[?&]$"), "")
            .trimEnd('&', '?')
        cleanUrl = cleanUrl.replace(
            "https://www.timotxt.com",
            "https://www-timotxt-com.translate.goog"
        )
        val separator = if (cleanUrl.contains("?")) "&" else "?"
        return "$cleanUrl$separator$TRANSLATE_PARAMS"
    }

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val titleEl = doc.selectFirst(
                "h1.imgtext, .chapter-content h1, .chapter-content .title, h1.chapter-title, h1"
            ) ?: return@withContext null
            val titleCn = titleEl.text().trim()
            if (titleCn.isBlank()) null else translateTextFastFail(titleCn)
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            val contentEl = doc.selectFirst("div.chapter-content div.content")
                ?: doc.selectFirst("div.chapter-content")
                ?: doc.selectFirst("div.content")
                ?: return@withContext ""

            contentEl.also {
                it.select(".gadBlock").remove()
                it.select("script").remove()
                it.select("style").remove()
                it.select("iframe").remove()
                it.select(".ads").remove()
                it.select(".share-buttons").remove()
                it.select(".pager").remove()
            }

            val rawText = TextExtractor.get(contentEl)
            val cleanedCn = rawText.cleanChineseText()
            if (cleanedCn.isBlank()) return@withContext ""
            val translatedEn = translateText(cleanedCn)
            translatedEn.cleanEnglishJunk()
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(resolveOriginalUrl(bookUrl)).toDocument()
            doc.selectFirst(".cover img[src]")
                ?.attr("src")
                ?: doc.selectFirst("meta[property=og:image]")
                    ?.attr("content")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(resolveOriginalUrl(bookUrl)).toDocument()
            val rawDesc = doc.selectFirst("meta[name=description]")
                ?.attr("content")
                ?.trim()
                ?: doc.selectFirst(".intro, .desc, .bookinfo-detail .desc")
                    ?.let { TextExtractor.get(it).trim() }
            if (rawDesc.isNullOrBlank()) null
            else translateTextFastFail(rawDesc)
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val fetchUrl = resolveOriginalUrl(bookUrl)
            val dirUrl = fetchUrl.toUrlBuilderSafe().addPath("dir").toString()
            val doc = networkClient.get(dirUrl).toDocument()

            val chapterLinks = doc.select(".chaplist ul.all li a[href]")
                .takeIf { it.isNotEmpty() }
                ?: doc.select(".chaplist ul li a[href]")

            val titles = chapterLinks.map { it.text().trim() }
            val translatedTitles = translateBatchTitles(titles, maxRetries = TITLE_TRANSLATE_MAX_RETRIES)

            chapterLinks.mapIndexed { index, element ->
                val realUrl = URI(originalBaseUrl).resolve(element.attr("href")).toString()
                val storedUrl = realUrl.replace(originalBaseUrl, baseUrl)
                ChapterResult(
                    title = translatedTitles.getOrElse(index) { titles[index] },
                    url = storedUrl
                )
            }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = "${originalBaseUrl}bookstack/?page=$page"
            val doc = networkClient.get(url).toDocument()

            val rawBooks = doc.select("ul.list.flex > li").mapNotNull { li ->
                val link = li.selectFirst("h3 a[href], a[href]") ?: return@mapNotNull null
                val cover = li.selectFirst("img[src]")?.attr("src") ?: ""
                val originalTitle = link.text().trim()
                if (originalTitle.isBlank()) return@mapNotNull null
                val realUrl = URI(originalBaseUrl).resolve(link.attr("href")).toString()
                val storedUrl = realUrl.replace(originalBaseUrl, baseUrl)
                Triple(originalTitle, storedUrl, cover)
            }

            val translatedTitles = translateBatchTitles(
                rawBooks.map { it.first },
                maxRetries = TITLE_TRANSLATE_MAX_RETRIES
            )

            val books = rawBooks.mapIndexed { i, (originalTitle, storedUrl, cover) ->
                BookResult(
                    title = translatedTitles.getOrElse(i) { originalTitle },
                    url = storedUrl,
                    coverImageUrl = cover
                )
            }

            val hasNextPage = doc.selectFirst("li.next.pagination-link:not(.disabled)") != null
            PagedList(list = books, index = index, isLastPage = !hasNextPage)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            if (input.startsWith("http") && (input.contains("timotxt.com") || input.contains("translate.goog"))) {
                val fetchUrl = resolveOriginalUrl(input)
                val doc = networkClient.get(fetchUrl).toDocument()
                val rawTitle = doc.selectFirst("h1, .book-name, .book-title")
                    ?.text()?.trim() ?: return@tryConnect PagedList.createEmpty(index)
                val cover = doc.selectFirst(".cover img[src]")
                    ?.attr("src")
                    ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
                    ?: ""
                val storedUrl = fetchUrl.replace(originalBaseUrl, baseUrl)
                return@tryConnect PagedList(
                    list = listOf(
                        BookResult(
                            title = translateTextFastFail(rawTitle),
                            url = storedUrl,
                            coverImageUrl = cover
                        )
                    ),
                    index = index,
                    isLastPage = true
                )
            }

            val searchUrl = originalBaseUrl.toUrlBuilderSafe()
                .addPath("search")
                .addPath(input)
                .toString()
            val doc = networkClient.get(searchUrl).toDocument()
            val rawBooks = doc.select("ul.list.flex > li").mapNotNull { li ->
                val link = li.selectFirst("h3 a[href], a[href]") ?: return@mapNotNull null
                val cover = li.selectFirst("img[src]")?.attr("src") ?: ""
                val originalTitle = link.text().trim()
                if (originalTitle.isBlank()) return@mapNotNull null
                val realUrl = URI(originalBaseUrl).resolve(link.attr("href")).toString()
                val storedUrl = realUrl.replace(originalBaseUrl, baseUrl)
                Triple(originalTitle, storedUrl, cover)
            }

            val translatedTitles = translateBatchTitles(
                rawBooks.map { it.first },
                maxRetries = TITLE_TRANSLATE_MAX_RETRIES
            )

            val books = rawBooks.mapIndexed { i, (originalTitle, storedUrl, cover) ->
                BookResult(
                    title = translatedTitles.getOrElse(i) { originalTitle },
                    url = storedUrl,
                    coverImageUrl = cover
                )
            }

            PagedList(list = books, index = index, isLastPage = true)
        }
    }

    // ─── Translation engine ─────────────────────────────────────────────

    private suspend fun translateText(
        text: String,
        maxRetries: Int = MAX_RETRIES
    ): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        if (!isPrimarilyCJK(text)) return@withContext text

        val batches = splitIntoSentenceBatches(text, BATCH_CHAR_LIMIT)
        val translatedParts = mutableListOf<String>()

        for (batch in batches) {
            val translated = translateBatch(batch, maxRetries) ?: batch
            translatedParts.add(translated)
            delay(TRANSLATE_DELAY_MS)
        }

        translatedParts.joinToString(" ").trim()
    }

    private suspend fun translateTextFastFail(text: String): String =
        translateText(text, maxRetries = TITLE_TRANSLATE_MAX_RETRIES)

    private suspend fun translateBatch(
        text: String,
        maxRetries: Int = MAX_RETRIES
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        repeat(maxRetries) { attempt ->
            try {
                val url = TRANSLATE_API_URL.toUrlBuilderSafe()
                    .add("client", "gtx")
                    .add("sl", "zh-CN")
                    .add("tl", "en")
                    .add("dt", "t")
                    .toString()

                val formBody = FormBody.Builder().add("q", text).build()
                val request = postRequest(url, body = formBody)
                val response = networkClient.call(request)
                val json = response.body.string()
                return@withContext parseTranslationResponse(json)
            } catch (e: Exception) {
                if (attempt < maxRetries - 1) {
                    delay(RETRY_DELAY_MS * (1L shl attempt))
                }
            }
        }
        null
    }

    private suspend fun translateBatchTitles(
        titles: List<String>,
        maxRetries: Int = MAX_RETRIES
    ): List<String> = withContext(Dispatchers.IO) {
        if (titles.isEmpty()) return@withContext emptyList()

        val chunkSize = 15
        val result = mutableListOf<String>()

        for (batch in titles.chunked(chunkSize)) {
            val joined = batch.joinToString(" ||| ")
            if (!isPrimarilyCJK(joined)) {
                result.addAll(batch)
                continue
            }

            val translated = translateBatch(joined, maxRetries) ?: joined
            val split = translated.split(" ||| ")

            if (split.size == batch.size) {
                result.addAll(split)
            } else {
                val fallbackSplit = translated.split(Regex("\\s*\\|{2,3}\\s*"))
                if (fallbackSplit.size == batch.size) {
                    result.addAll(fallbackSplit)
                } else {
                    for (title in batch) {
                        result.add(translateText(title, maxRetries))
                        delay(TRANSLATE_DELAY_MS)
                    }
                }
            }
            delay(TRANSLATE_DELAY_MS)
        }

        result
    }

    private fun splitIntoSentenceBatches(text: String, maxSize: Int): List<String> {
        val sentenceBoundary = Regex("(?<=[。！？」.!?])")
        val sentences = sentenceBoundary.split(text)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val batches = mutableListOf<String>()
        val currentBatch = StringBuilder()

        for (sentence in sentences) {
            if (currentBatch.isNotEmpty() && currentBatch.length + sentence.length > maxSize) {
                batches.add(currentBatch.toString())
                currentBatch.clear()
                currentBatch.append(sentence)
            } else {
                currentBatch.append(sentence)
            }
        }

        if (currentBatch.isNotEmpty()) batches.add(currentBatch.toString())
        return batches
    }

    private fun parseTranslationResponse(json: String): String? = try {
        val root = JsonParser.parseString(json).asJsonArray
        val translatedParts = mutableListOf<String>()
        for (item in root[0].asJsonArray) {
            val innerArray = item.asJsonArray
            if (innerArray.size() > 0 && !innerArray[0].isJsonNull) {
                translatedParts.add(innerArray[0].asString)
            }
        }
        translatedParts.joinToString("")
    } catch (e: Exception) {
        null
    }
}

// ─── String cleaning extensions ─────────────────────────────────────────

fun String.cleanChineseText(): String {
    var result = this
    for (pattern in TimoTxtTranslate.STRIP_PATTERNS) {
        result = result.replace(pattern, "")
    }
    result = result.replace(TimoTxtTranslate.UNICODE_WHITELIST, "")
    result = result.replace(Regex("\\s{3,}"), "  ").trim()
    return result
}

fun String.cleanEnglishJunk(): String {
    var result = this
    for (pattern in TimoTxtTranslate.ENGLISH_JUNK_PATTERNS) {
        result = result.replace(pattern, "")
    }
    return result.replace(Regex("\n{3,}"), "\n\n").trim()
}
