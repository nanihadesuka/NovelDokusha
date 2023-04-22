package my.noveldokusha.ui.previewFixtures

import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.PagedList
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.scraper.SourceInterface

fun previewFixturesCatalogList(): List<SourceInterface.Catalog> = (0..7).map {
    object : SourceInterface.Catalog {
        override val catalogUrl = "catalogUrl$it"
        override val language = LanguageCode.ENGLISH
        override suspend fun getChapterList(bookUrl: String): Response<List<ChapterMetadata>> =
            Response.Success(listOf())

        override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun getCatalogSearch(
            index: Int,
            input: String
        ): Response<PagedList<BookMetadata>> = Response.Success(PagedList.createEmpty(0))

        override val id = "id$it"
        override val nameStrId = R.string.source_name_local
        override val baseUrl = "baseUrl$it"
    }
}