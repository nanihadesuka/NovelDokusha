package my.noveldokusha.ui.previewFixtures

import my.noveldokusha.R
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult

fun previewFixturesCatalogList(): List<SourceInterface.Catalog> = (0..7).map {
    object : SourceInterface.Catalog {
        override val catalogUrl = "catalogUrl$it"
        override val language = LanguageCode.ENGLISH
        override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
            Response.Success(listOf())

        override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun getCatalogSearch(
            index: Int,
            input: String
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override val id = "id$it"
        override val nameStrId = R.string.source_name_local
        override val baseUrl = "baseUrl$it"
    }
}