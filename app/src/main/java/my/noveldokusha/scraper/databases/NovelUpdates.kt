package my.noveldokusha.scraper.databases

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.data.Response.Success
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.add
import my.noveldokusha.utils.runCatchingAsResponse
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilderSafe
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class NovelUpdates(
    private val networkClient: NetworkClient
) : DatabaseInterface {
    override val id = "novel_updates"
    override val name = "Novel Updates"
    override val baseUrl = "https://www.novelupdates.com/"
    override val iconUrl = "https://www.novelupdates.com/favicon.ico"

    override suspend fun getSearchFilters(): Response<List<SearchGenre>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get("https://www.novelupdates.com/series-finder/")
                    .toDocument()
                    .select(".genreme")
                    .associate { it.text().trim() to it.attr("genreid") }
                    .map { (genre, id) -> SearchGenre(id = id, genreName = genre) }
                    .let { Success(it) }
            }
        }

    override suspend fun searchByTitle(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        val page = index + 1
        val url = baseUrl.toUrlBuilderSafe().apply {
            if (page > 1) appendPath("page").appendPath(page.toString())
            add("s", input)
            add("post_type", "seriesplans")
        }

        getSearchList(page, url)
    }

    override suspend fun searchByFilters(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        val page = index + 1
        val url = "https://www.novelupdates.com/series-finder/?sf=1"
            .toUrlBuilderSafe()
            .apply {
                if (genresIncludedId.isNotEmpty()) add(
                    "gi" to genresIncludedId.joinToString(","),
                    "mgi" to "and"
                )
                if (genresExcludedId.isNotEmpty())
                    add("ge", genresExcludedId.joinToString(","))
                add("sort", "sdate")
                add("order", "desc")
                if (page > 1) add("pg", page)
            }

        getSearchList(index, url)
    }


    override suspend fun getBookData(
        bookUrl: String
    ): Response<DatabaseInterface.BookData> = withContext(Dispatchers.Default) {
        networkClient.get(bookUrl).toDocument().let(::parseBookData)
    }

    override suspend fun getAuthorData(authorUrl: String):
            Response<DatabaseInterface.AuthorData> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(authorUrl).toDocument()
            val books = doc.select(".search_main_box_nu")
                .mapNotNull {
                    val title =
                        it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                    val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = title.text(),
                        url = title.attr("href"),
                        coverImageUrl = image
                    )
                }
            val authorName = doc.selectFirst("h3.mypage.followauthor")!!.text()

            DatabaseInterface.AuthorData(
                name = authorName,
                books = books
            ).let(::Success)
        }
    }

    private suspend fun getSearchList(
        index: Int,
        url: Uri.Builder
    ) = withContext(Dispatchers.Default) {
        tryConnect(extraErrorInfo = "index: $index\n\nurl: $url") {
            val doc = networkClient.get(url).toDocument()
            doc.select(".search_main_box_nu")
                .mapNotNull {
                    val title =
                        it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                    val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = title.text(),
                        url = title.attr("href"),
                        coverImageUrl = image
                    )
                }
                .let { Success(PagedList(list = it, index = index, isLastPage = isLastPage(doc))) }
        }
    }

    private fun parseBookData(
        doc: Document
    ): Response<DatabaseInterface.BookData> = runCatchingAsResponse {
        val relatedBooks = doc
            .select("h5")
            .find { it.hasClass("seriesother") && it.text() == "Related Series" }
            ?.nextElementSiblings()?.asSequence()
            ?.takeWhile { !it.`is`("h5") }
            ?.filter { it.`is`("a[href]") }
            ?.map { BookMetadata(it.text(), it.attr("href")) }
            ?.toList() ?: listOf()

        val similarRecommended = doc
            .select("h5")
            .find { it.hasClass("seriesother") && it.text() == "Recommendations" }
            ?.nextElementSiblings()?.asSequence()
            ?.takeWhile { !it.`is`("h5") }
            ?.filter { it.`is`("a[href]") }
            ?.map { BookMetadata(it.text(), it.attr("href")) }
            ?.toList() ?: listOf()

        val authors = doc
            .selectFirst("#showauthors")!!
            .select("a[href]")
            .map { DatabaseInterface.AuthorMetadata(name = it.text(), url = it.attr("href")) }

        DatabaseInterface.BookData(
            title = doc.selectFirst(".seriestitlenu")?.text() ?: "",
            description = TextExtractor.get(doc.selectFirst("#editdescription")).trim(),
            alternativeTitles = TextExtractor.get(doc.selectFirst("#editassociated"))
                .split("\n"),
            relatedBooks = relatedBooks,
            similarRecommended = similarRecommended,
            bookType = doc.selectFirst(".genre, .type")?.text() ?: "",
            genres = doc.select("#seriesgenre a").map { it.text() },
            tags = doc.select("#showtags a").map { it.text() },
            authors = authors,
            coverImageUrl = doc.selectFirst("div.seriesimg > img[src]")?.attr("src")
        )
    }

    private fun isLastPage(doc: Document) =
        when (val nav = doc.selectFirst("div.digg_pagination")) {
            null -> true
            else -> nav.children().last()?.`is`(".current") ?: true
        }
}
