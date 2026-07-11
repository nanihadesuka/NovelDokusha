package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.lua_engine.LuaEngine
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import okhttp3.FormBody
import org.jsoup.nodes.Document
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import timber.log.Timber

/**
 * Wraps a HnDK0 Chinese (zh) Lua source plugin and translates all text
 * output to English using the Google Translate free API — the same
 * translation engine used by [TimoTxtTranslate].
 *
 * This adapter loads the original zh Lua plugin (for site structure,
 * selectors, URL patterns) but intercepts every text-returning method to
 * translate the Chinese output to English before returning it to the app.
 *
 * The translated sources appear in the catalog under "MTL (HnDK0)"
 * with the display name "SourceName (Translate)".
 *
 * Translation pipeline (identical to TimoTxtTranslate):
 *   1. The underlying Lua plugin fetches Chinese HTML from the source site
 *   2. This adapter extracts Chinese text via the Lua plugin's functions
 *   3. Chinese text is cleaned (strip notices, remove Unicode garbage)
 *   4. Text is batched into ≤4500 char chunks at sentence boundaries
 *   5. Each batch is POST-ed to Google Translate API (client=gtx)
 *   6. Translated English text is returned to the reader
 *
 * Catalog titles and book descriptions use fast-fail translation (1 retry)
 * so browsing is never blocked by a slow translate API.
 *
 * @param luaEngine     the shared Lua engine
 * @param networkClient the shared HTTP client
 * @param pluginScript  the full .lua file content (the zh plugin)
 * @param pluginId      the plugin's unique ID (e.g. "novel543")
 * @param pluginName    the display name (e.g. "Novel543 (Translate)")
 * @param baseUrl       the plugin's base URL (parsed from the .lua file)
 * @param iconUrlStr    the plugin's icon URL
 */
class LuaTranslatedSourceAdapter(
    private val luaEngine: LuaEngine,
    private val networkClient: NetworkClient,
    private val pluginScript: String,
    private val pluginId: String,
    private val _displayName: String,
    override val baseUrl: String,
    private val iconUrlStr: String,
) : SourceInterface.Catalog {

    override val id = "hndk0_trans_${pluginId}"
    override val nameStrId = R.string.source_name_local
    override val displayName: String = _displayName
    override val catalogUrl = baseUrl
    override val language = LanguageCode.MTL_HNDK0
    override val iconUrl: Any get() = iconUrlStr

    // Lazy-loaded Lua globals from the zh plugin
    private val globals: LuaValue? by lazy {
        luaEngine.loadPlugin(pluginScript, pluginId)
    }

    // ─── Translation engine (mirrors TimoTxtTranslate) ──────────────

    companion object {
        private const val TRANSLATE_API_URL =
            "https://translate.googleapis.com/translate_a/single"
        private const val BATCH_CHAR_LIMIT = 4500
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1500L
        private const val TRANSLATE_DELAY_MS = 300L
        private const val TITLE_TRANSLATE_MAX_RETRIES = 1

        private val STRIP_PATTERNS = listOf(
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

        private val UNICODE_WHITELIST = Regex(
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
    }

    private fun isPrimarilyCJK(text: String): Boolean {
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

    private fun cleanChineseText(text: String): String {
        var result = text
        for (pattern in STRIP_PATTERNS) {
            result = result.replace(pattern, "")
        }
        result = result.replace(UNICODE_WHITELIST, "")
        result = result.replace(Regex("\\s{3,}"), "  ").trim()
        return result
    }

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
                response.close()
                return@withContext parseTranslationResponse(json)
            } catch (e: Exception) {
                if (attempt < maxRetries - 1) {
                    delay(RETRY_DELAY_MS * (1L shl attempt))
                }
            }
        }
        null
    }

    private suspend fun translateBatchTitles(titles: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            if (titles.isEmpty()) return@withContext emptyList()

            val chunkSize = 15
            val result = mutableListOf<String>()

            for (batch in titles.chunked(chunkSize)) {
                val joined = batch.joinToString(" ||| ")
                if (!isPrimarilyCJK(joined)) {
                    result.addAll(batch)
                    continue
                }

                val translated = translateBatch(joined, TITLE_TRANSLATE_MAX_RETRIES) ?: joined
                val split = translated.split(" ||| ")

                if (split.size == batch.size) {
                    result.addAll(split)
                } else {
                    for (title in batch) {
                        result.add(translateTextFastFail(title))
                        delay(TRANSLATE_DELAY_MS)
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

    // ─── SourceInterface.Catalog implementation ─────────────────────
    // Delegates to the Lua plugin for site structure, then translates
    // all text output.

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val g = globals ?: return@withContext null
            val fn = g.get("getChapterTitle") ?: return@withContext null
            if (fn.isnil()) return@withContext null
            try {
                val result = fn.call(LuaValue.valueOf(doc.html()))
                if (result.isnil()) null
                else {
                    val titleCn = result.tojstring()
                    if (titleCn.isBlank()) null else translateTextFastFail(titleCn)
                }
            } catch (e: Exception) {
                Timber.e(e, "[$id] getChapterTitle failed")
                null
            }
        }

    override suspend fun getChapterText(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val g = globals ?: return@withContext ""
            val fn = g.get("getChapterText") ?: return@withContext ""
            if (fn.isnil()) return@withContext ""
            try {
                val result = fn.call(
                    LuaValue.valueOf(doc.html()),
                    LuaValue.valueOf(doc.location())
                )
                if (result.isnil()) ""
                else {
                    val rawText = result.tojstring()
                    val cleanedCn = cleanChineseText(rawText)
                    if (cleanedCn.isBlank()) ""
                    else translateText(cleanedCn)
                }
            } catch (e: Exception) {
                Timber.e(e, "[$id] getChapterText failed")
                ""
            }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val g = globals ?: return@tryConnect null
                val fn = g.get("getBookCoverImageUrl") ?: return@tryConnect null
                if (fn.isnil()) return@tryConnect null
                val result = fn.call(LuaValue.valueOf(bookUrl))
                if (result.isnil()) null else result.tojstring().ifBlank { null }
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val g = globals ?: return@tryConnect null
                val fn = g.get("getBookDescription") ?: return@tryConnect null
                if (fn.isnil()) return@tryConnect null
                val result = fn.call(LuaValue.valueOf(bookUrl))
                if (result.isnil()) null
                else {
                    val descCn = result.tojstring()
                    if (descCn.isBlank()) null else translateTextFastFail(descCn)
                }
            }
        }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val g = globals ?: return@tryConnect emptyList()
                val fn = g.get("getChapterList") ?: return@tryConnect emptyList()
                if (fn.isnil()) return@tryConnect emptyList()
                val result = fn.call(LuaValue.valueOf(bookUrl))
                if (result.isnil() || !result.istable()) return@tryConnect emptyList()
                result as LuaTable

                val titles = mutableListOf<String>()
                val urls = mutableListOf<String>()
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = result.next(k)
                    if (n.arg1().isnil()) break
                    val item = n.arg(2)
                    if (item.istable()) {
                        item as LuaTable
                        titles.add(item.get("title").optjstring(""))
                        urls.add(item.get("url").optjstring(""))
                    }
                    k = n.arg1()
                }

                // Batch-translate chapter titles
                val translatedTitles = translateBatchTitles(titles)

                titles.indices.map { i ->
                    ChapterResult(
                        title = translatedTitles.getOrElse(i) { titles[i] },
                        url = urls[i]
                    )
                }.filter { it.url.isNotEmpty() }
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val g = globals ?: return@tryConnect PagedList.createEmpty(index)
                val fn = g.get("getCatalogList") ?: return@tryConnect PagedList.createEmpty(index)
                if (fn.isnil()) return@tryConnect PagedList.createEmpty(index)
                val result = fn.call(LuaValue.valueOf(index))
                if (result.isnil() || !result.istable()) return@tryConnect PagedList.createEmpty(index)
                result as LuaTable

                val rawBooks = mutableListOf<Triple<String, String, String>>()
                val itemsValue = result.get("items")
                if (itemsValue.istable()) {
                    itemsValue as LuaTable
                    var k: LuaValue = LuaValue.NIL
                    while (true) {
                        val n = itemsValue.next(k)
                        if (n.arg1().isnil()) break
                        val item = n.arg(2)
                        if (item.istable()) {
                            item as LuaTable
                            val title = item.get("title").optjstring("")
                            val url = item.get("url").optjstring("")
                            val cover = item.get("cover").optjstring("")
                            if (title.isNotEmpty() && url.isNotEmpty()) {
                                rawBooks.add(Triple(title, url, cover))
                            }
                        }
                        k = n.arg1()
                    }
                }

                val hasNext = result.get("hasNext").optboolean(false)

                // Batch-translate catalog titles (fast-fail)
                val translatedTitles = translateBatchTitles(rawBooks.map { it.first })

                val books = rawBooks.mapIndexed { i, (originalTitle, url, cover) ->
                    BookResult(
                        title = translatedTitles.getOrElse(i) { originalTitle },
                        url = url,
                        coverImageUrl = cover
                    )
                }

                PagedList(list = books, index = index, isLastPage = !hasNext)
            }
        }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val g = globals ?: return@tryConnect PagedList.createEmpty(index)
                val fn = g.get("getCatalogSearch") ?: return@tryConnect PagedList.createEmpty(index)
                if (fn.isnil()) return@tryConnect PagedList.createEmpty(index)
                val result = fn.call(LuaValue.valueOf(index), LuaValue.valueOf(input))
                if (result.isnil() || !result.istable()) return@tryConnect PagedList.createEmpty(index)
                result as LuaTable

                val rawBooks = mutableListOf<Triple<String, String, String>>()
                val itemsValue = result.get("items")
                if (itemsValue.istable()) {
                    itemsValue as LuaTable
                    var k: LuaValue = LuaValue.NIL
                    while (true) {
                        val n = itemsValue.next(k)
                        if (n.arg1().isnil()) break
                        val item = n.arg(2)
                        if (item.istable()) {
                            item as LuaTable
                            val title = item.get("title").optjstring("")
                            val url = item.get("url").optjstring("")
                            val cover = item.get("cover").optjstring("")
                            if (title.isNotEmpty() && url.isNotEmpty()) {
                                rawBooks.add(Triple(title, url, cover))
                            }
                        }
                        k = n.arg1()
                    }
                }

                val hasNext = result.get("hasNext").optboolean(false)

                val translatedTitles = translateBatchTitles(rawBooks.map { it.first })

                val books = rawBooks.mapIndexed { i, (originalTitle, url, cover) ->
                    BookResult(
                        title = translatedTitles.getOrElse(i) { originalTitle },
                        url = url,
                        coverImageUrl = cover
                    )
                }

                PagedList(list = books, index = index, isLastPage = !hasNext)
            }
        }
}
