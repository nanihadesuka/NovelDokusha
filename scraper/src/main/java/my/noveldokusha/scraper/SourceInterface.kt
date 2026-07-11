package my.noveldokusha.scraper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

sealed interface SourceInterface {
    val id: String

    @get:StringRes
    val nameStrId: Int
    val baseUrl: String
    val isLocalSource: Boolean get() = true
    val requiresLogin: Boolean get() = false

    /**
     * Optional display name that overrides [nameStrId] when non-null.
     * Used by dynamically-loaded sources (e.g. Lua plugins from HnDK0)
     * whose names aren't known at compile time.
     */
    val displayName: String? get() = null

    /**
     * Transform a chapter URL for OkHttp fetching (the in-app reader).
     *
     * Override in sources whose stored URL is a routing key that doesn't
     * directly serve content (e.g. TimoTxtTranslate stores translate.goog
     * URLs but fetches from timotxt.com; TimoTxtGemini stores gemini.goog
     * URLs but fetches from timotxt.com).
     */
    suspend fun transformChapterUrl(url: String): String = url

    /**
     * Transform a chapter URL for opening in the in-app WebView browser.
     *
     * Override in sources whose stored URL won't render usefully in a
     * browser without modification (e.g. TimoTxt sources convert to the
     * translate.goog proxy with `_x_tr_*` params so the proxy's JS
     * translates the page to English in the browser; Reddit converts to
     * old.reddit.com).
     *
     * Default: return the URL unchanged.
     */
    suspend fun transformWebviewUrl(url: String): String = url

    suspend fun getChapterTitle(doc: Document): String? = null
    suspend fun getChapterText(doc: Document): String? = null

    interface Base : SourceInterface
    interface Catalog : SourceInterface {
        val catalogUrl: String
        val language: LanguageCode?
        val iconUrl: Any get() = "$baseUrl/favicon.ico"

        suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
            Response.Success(null)

        suspend fun getBookDescription(bookUrl: String): Response<String?> = Response.Success(null)

        /**
         * Chapters list ordered from first one (oldest) to newest one.
         */
        suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>>
        suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>>
        suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>>
    }

    interface Configurable {
        @Composable
        fun ScreenConfig()
    }
}