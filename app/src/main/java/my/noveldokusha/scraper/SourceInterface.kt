package my.noveldokusha.scraper

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import org.jsoup.nodes.Document

interface SourceInterface
{
    val name: String
    val baseUrl: String

    // Transform current url to preferred url
    suspend fun transformChapterUrl(url: String): String = url

    suspend fun getChapterTitle(doc: Document): String? = null
    suspend fun getChapterText(doc: Document): String? = null

    interface Base : SourceInterface
    interface Catalog : SourceInterface
    {
        val catalogUrl: String
        val language: String

        suspend fun getBookCoverImageUrl(doc: Document): String? = null
        suspend fun getBookDescripton(doc: Document): String? = null

        /**
         * Chapters list ordered from first one (oldest) to newest one.
         */
        suspend fun getChapterList(doc: Document): List<ChapterMetadata>
        suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>>
        suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookMetadata>>
    }
}