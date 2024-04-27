package my.noveldokusha.scraper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import my.noveldokusha.data.Response
import my.noveldokusha.network.PagedList
import org.jsoup.nodes.Document

sealed interface SourceInterface {
    val id: String
    @get:StringRes
    val nameStrId: Int
    val baseUrl: String
    val isLocalSource: Boolean get() = true

    // Transform current url to preferred url
    suspend fun transformChapterUrl(url: String): String = url

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
        suspend fun getChapterList(bookUrl: String): Response<List<my.noveldokusha.feature.local_database.ChapterMetadata>>
        suspend fun getCatalogList(index: Int): Response<PagedList<my.noveldokusha.feature.local_database.BookMetadata>>
        suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<my.noveldokusha.feature.local_database.BookMetadata>>
    }

    interface Configurable {
        @Composable
        fun ScreenConfig()
    }
}