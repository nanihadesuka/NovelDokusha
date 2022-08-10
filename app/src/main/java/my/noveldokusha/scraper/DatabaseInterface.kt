package my.noveldokusha.scraper

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.Response
import org.jsoup.nodes.Document

interface DatabaseInterface {

    val id: String
    val name: String
    val baseUrl: String
    val iconUrl: String get() = "$baseUrl/favicon.ico"

    val searchGenresCacheFileName get() = "database_search_genres__$id"

    suspend fun getSearchAuthorSeries(
        index: Int,
        urlAuthorPage: String
    ): Response<PagedList<BookMetadata>>

    suspend fun getSearchGenres(): Response<Map<String, String>>
    suspend fun getSearch(index: Int, input: String): Response<PagedList<BookMetadata>>
    suspend fun getSearchAdvanced(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<PagedList<BookMetadata>>

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