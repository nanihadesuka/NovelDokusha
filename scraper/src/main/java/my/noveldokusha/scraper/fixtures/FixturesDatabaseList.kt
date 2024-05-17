package my.noveldokusha.scraper.fixtures

import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.domain.BookResult

fun fixturesDatabaseList(): List<DatabaseInterface> = (0..2).map {
    object : DatabaseInterface {
        override val id = "id$it"
        override val nameStrId = R.string.database_name_baka_updates
        override val baseUrl = "baseUrl$it"
        override suspend fun getCatalog(index: Int): Response<PagedList<BookResult>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByTitle(
            index: Int,
            input: String
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByFilters(
            index: Int,
            genresIncludedId: List<String>,
            genresExcludedId: List<String>
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun getSearchFilters(): Response<List<my.noveldokusha.scraper.SearchGenre>> =
            Response.Success(listOf())

        override suspend fun getBookData(bookUrl: String): Response<DatabaseInterface.BookData> =
            TODO()

        override suspend fun getAuthorData(authorUrl: String): Response<DatabaseInterface.AuthorData> =
            TODO()
    }
}

