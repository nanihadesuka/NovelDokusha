package my.noveldokusha.scraper

import my.noveldokusha.BookMetadata
import my.noveldokusha.ChapterMetadata
import my.noveldokusha.DataCache_DatabaseSearchGenres
import org.jsoup.nodes.Document

interface source_interface
{
    val name: String
    val baseUrl: String

    // Transform current url to preferred url
    suspend fun transformChapterUrl(url: String): String = url

    suspend fun getChapterTitle(doc: Document): String? = null
    suspend fun getChapterText(doc: Document): String? = null

    interface base : source_interface
    interface catalog : source_interface
    {
        val catalogUrl: String
        val language: String

        // The chapter order is ascendent: from 1 ... etc
        suspend fun getChapterList(doc: Document): List<ChapterMetadata>
        suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
        suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    }
}


interface database_interface
{
    val id: String
    val name: String
    val baseUrl: String

    val searchGenresCache get() = DataCache_DatabaseSearchGenres(id)

    suspend fun getSearchAuthorSeries(index: Int, urlAuthorPage: String): Response<List<BookMetadata>>
    suspend fun getSearchGenres(): Response<Map<String, String>>
    suspend fun getSearch(index: Int, input: String): Response<List<BookMetadata>>
    suspend fun getSearchAdvanced(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<List<BookMetadata>>

    data class BookAuthor(val name: String, val url: String?)
    data class BookData(
        val title: String,
        val description: String,
        val alternativeTitles: List<String>,
        val authors: List<BookAuthor>,
        val tags: List<String>,
        val genres: List<String>,
        val bookType: String,
        val relatedBooks: List<BookMetadata>,
        val similarRecommended: List<BookMetadata>,
        val coverImageUrl: String?
    )

    fun getBookData(doc: Document): BookData
}