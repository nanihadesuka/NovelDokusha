package my.noveldokusha.scraper

import androidx.annotation.StringRes
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.scraper.domain.BookResult

data class SearchGenre(val id: String, val genreName: String)

interface DatabaseInterface {

    val id: String

    @get:StringRes
    val nameStrId: Int
    val baseUrl: String
    val iconUrl: String get() = "$baseUrl/favicon.ico"

    val searchGenresCacheFileName get() = "database_search_genres_v2_$id"

    suspend fun getCatalog(index: Int): Response<PagedList<BookResult>>

    suspend fun searchByTitle(index: Int, input: String): Response<PagedList<BookResult>>

    suspend fun searchByFilters(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<PagedList<BookResult>>

    suspend fun getSearchFilters(): Response<List<SearchGenre>>

    suspend fun getBookData(bookUrl: String): Response<BookData>

    suspend fun getAuthorData(authorUrl: String): Response<AuthorData>

    data class AuthorMetadata(val name: String, val url: String?)

    data class BookData(
        val title: String,
        val description: String,
        val alternativeTitles: List<String>,
        val authors: List<AuthorMetadata>,
        val tags: List<String>,
        val genres: List<SearchGenre>,
        val bookType: String,
        val relatedBooks: List<BookResult>,
        val similarRecommended: List<BookResult>,
        val coverImageUrl: String?
    )

    data class AuthorData(
        val name: String,
        val coverImageUrl: String? = null,
        val books: List<BookResult>,
        val description: String = "",
        val associatedNames: List<String> = listOf(),
        val genres: List<String> = listOf(),
    )
}