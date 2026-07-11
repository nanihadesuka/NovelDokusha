package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.lua_engine.LuaEngine
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import timber.log.Timber

/**
 * Adapter that implements [SourceInterface.Catalog] by delegating to a
 * loaded Lua plugin's global functions.
 *
 * The Lua plugin exposes:
 *   - Global metadata: id, name, version, baseUrl, language, icon
 *   - Functions: getCatalogList, getCatalogSearch, getBookTitle,
 *     getBookCoverImageUrl, getBookDescription, getChapterList,
 *     getChapterText, and optionally getChapterListHash, parsePage,
 *     getFilterList, getCatalogFiltered, getSettingsSchema.
 *
 * This adapter translates between the Kotlin SourceInterface contract
 * (which uses Response/PagedList/ChapterResult) and the Lua plugin's
 * table-based return format ({items, hasNext} / {title, url, cover}).
 *
 * Display name: "SourceName (HnDK0)" — the "(HnDK0)" suffix identifies
 * these as community-maintained external sources loaded from GitHub.
 *
 * @param luaEngine  the shared Lua engine
 * @param pluginScript the full .lua file content
 * @param pluginId    the plugin's unique ID (from index.yaml)
 * @param displayName the full display name including "(HnDK0)" suffix
 * @param baseUrl     the plugin's base URL
 * @param languageCode the LanguageCode for this source (en/zh/mtl HnDK0 variant)
 * @param iconUrl     the plugin's icon URL
 */
class LuaSourceAdapter(
    private val luaEngine: LuaEngine,
    private val pluginScript: String,
    private val pluginId: String,
    private val _displayName: String,
    override val baseUrl: String,
    private val languageCode: LanguageCode?,
    private val iconUrlStr: String,
) : SourceInterface.Catalog {

    override val id = "hndk0_${pluginId}"
    override val nameStrId = R.string.source_name_local
    override val displayName: String = _displayName
    override val catalogUrl = baseUrl
    override val language = languageCode
    override val iconUrl: Any get() = iconUrlStr

    // Lazy-loaded Lua globals — loaded on first use to avoid blocking
    // Scraper construction.
    private val globals: LuaValue? by lazy {
        luaEngine.loadPlugin(pluginScript, pluginId)
    }

    init {
        Timber.i("LuaSourceAdapter: created for '$displayName' (id=$id, baseUrl=$baseUrl)")
    }

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val g = globals ?: return@withContext null
            val fn = g.get("getChapterTitle") ?: return@withContext null
            if (fn.isnil()) return@withContext null
            try {
                val html = doc.html()
                val result = fn.call(LuaValue.valueOf(html))
                if (result.isnil()) null else result.tojstring().ifBlank { null }
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
                val html = doc.html()
                val url = doc.location()
                val result = fn.call(LuaValue.valueOf(html), LuaValue.valueOf(url))
                if (result.isnil()) "" else result.tojstring()
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
                if (result.isnil()) null else result.tojstring().ifBlank { null }
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
                val chapters = mutableListOf<ChapterResult>()
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = result.next(k)
                    if (n.arg1().isnil()) break
                    val item = n.arg(2)
                    if (item.istable()) {
                        item as LuaTable
                        val title = item.get("title").optjstring("")
                        val url = item.get("url").optjstring("")
                        if (url.isNotEmpty()) {
                            chapters.add(ChapterResult(title = title, url = url))
                        }
                    }
                    k = n.arg1()
                }
                chapters
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
                val items = parseItems(result.get("items"))
                val hasNext = result.get("hasNext").optboolean(false)
                PagedList(list = items, index = index, isLastPage = !hasNext)
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
                val items = parseItems(result.get("items"))
                val hasNext = result.get("hasNext").optboolean(false)
                PagedList(list = items, index = index, isLastPage = !hasNext)
            }
        }

    private fun parseItems(itemsValue: LuaValue): List<BookResult> {
        if (itemsValue.isnil() || !itemsValue.istable()) return emptyList()
        itemsValue as LuaTable
        val items = mutableListOf<BookResult>()
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
                    items.add(BookResult(title = title, url = url, coverImageUrl = cover))
                }
            }
            k = n.arg1()
        }
        return items
    }
}
