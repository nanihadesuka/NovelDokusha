package my.noveldokusha.scraper

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.DataCache_DatabaseSearchGenres
import org.jsoup.nodes.Document


interface DatabaseInterface
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