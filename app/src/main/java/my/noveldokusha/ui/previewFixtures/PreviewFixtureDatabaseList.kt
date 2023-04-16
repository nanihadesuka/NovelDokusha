package my.noveldokusha.ui.previewFixtures

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.PagedList
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre

fun previewFixturesDatabaseList(): List<DatabaseInterface> = (0..2).map {
    object : DatabaseInterface {
        override val id = "id$it"
        override val name = "name$it"
        override val baseUrl = "baseUrl$it"
        override suspend fun getCatalog(index: Int): Response<PagedList<BookMetadata>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByTitle(
            index: Int,
            input: String
        ): Response<PagedList<BookMetadata>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByFilters(
            index: Int,
            genresIncludedId: List<String>,
            genresExcludedId: List<String>
        ): Response<PagedList<BookMetadata>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun getSearchFilters(): Response<List<SearchGenre>> =
            Response.Success(listOf())

        override suspend fun getBookData(bookUrl: String): Response<DatabaseInterface.BookData> =
            TODO()

        override suspend fun getAuthorData(authorUrl: String): Response<DatabaseInterface.AuthorData> =
            TODO()
    }
}

