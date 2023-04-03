package my.noveldokusha.scraper

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.PagedList

data class SearchGenre(val id: String, val genreName: String)

interface DatabaseInterface {

    val id: String
    val name: String
    val baseUrl: String
    val iconUrl: String get() = "$baseUrl/favicon.ico"

    val searchGenresCacheFileName get() = "database_search_genres_v2_$id"

    suspend fun searchByTitle(index: Int, input: String): Response<PagedList<BookMetadata>>

    suspend fun searchByFilters(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<PagedList<BookMetadata>>

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
        val genres: List<String>,
        val bookType: String,
        val relatedBooks: List<BookMetadata>,
        val similarRecommended: List<BookMetadata>,
        val coverImageUrl: String?
    )

    data class AuthorData(
        val name: String,
        val coverImageUrl: String? = null,
        val books: List<BookMetadata>,
        val description: String = "",
        val associatedNames: List<String> = listOf(),
        val genres: List<String> = listOf(),
    )
}