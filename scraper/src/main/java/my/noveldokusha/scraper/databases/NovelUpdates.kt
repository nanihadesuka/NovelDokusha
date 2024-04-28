package my.noveldokusha.scraper.databases

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.Response.Success
import my.noveldokusha.core.runCatchingAsResponse
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryFlatConnect
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class NovelUpdates(
    private val networkClient: NetworkClient
) : DatabaseInterface {
    override val id = "novel_updates"
    override val nameStrId = R.string.database_name_novel_updates
    override val baseUrl = "https://www.novelupdates.com/"
    override val iconUrl = "https://www.novelupdates.com/favicon.ico"

    override suspend fun getCatalog(index: Int) = withContext(Dispatchers.Default) {
        val page = index + 1
        val url = "https://www.novelupdates.com/novelslisting".toUrlBuilderSafe().apply {
            add("sort", "2")
            add("order", "2")
            add("status", "1")
            if (page > 1) appendPath("pg").appendPath(page.toString())
        }

        getSearchList(page, url)
    }

    override suspend fun getSearchFilters() = withContext(Dispatchers.Default) {
        tryFlatConnect {
            networkClient
                .get("https://www.novelupdates.com/series-finder/")
                .toDocument()
                .select(".genreme")
                .associate { it.text().trim() to it.attr("genreid") }
                .map { (genre, id) ->
                    my.noveldokusha.scraper.SearchGenre(
                        id = id,
                        genreName = genre
                    )
                }
                .let(::Success)
        }
    }

    override suspend fun searchByTitle(
        index: Int,
        input: String
    ) = withContext(Dispatchers.Default) {
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
    ) = withContext(Dispatchers.Default) {
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
    ) = withContext(Dispatchers.Default) {
        networkClient.get(bookUrl).toDocument().let(::parseBookData)
    }

    override suspend fun getAuthorData(authorUrl: String) = withContext(Dispatchers.Default) {
        tryFlatConnect {
            val doc = networkClient.get(authorUrl).toDocument()
            val books = doc.select(".search_main_box_nu")
                .mapNotNull {
                    val title =
                        it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                    val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                    BookResult(
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
        tryFlatConnect(extraErrorInfo = "index: $index\n\nurl: $url") {
            val doc = networkClient.get(url).toDocument()
            doc.select(".search_main_box_nu")
                .mapNotNull {
                    val title =
                        it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                    val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                    BookResult(
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
            ?.map {
                BookResult(
                    it.text(),
                    it.attr("href")
                )
            }
            ?.toList() ?: listOf()

        val similarRecommended = doc
            .select("h5")
            .find { it.hasClass("seriesother") && it.text() == "Recommendations" }
            ?.nextElementSiblings()?.asSequence()
            ?.takeWhile { !it.`is`("h5") }
            ?.filter { it.`is`("a[href]") }
            ?.map {
                BookResult(
                    it.text(),
                    it.attr("href")
                )
            }
            ?.toList() ?: listOf()

        val authors = doc
            .selectFirst("#showauthors")!!
            .select("a[href]")
            .map { DatabaseInterface.AuthorMetadata(name = it.text(), url = it.attr("href")) }

        val genres = doc
            .select("#seriesgenre a")
            .map { my.noveldokusha.scraper.SearchGenre(genreName = it.text(), id = it.attr("gid")) }

        val tags = doc
            .select("#showtags a")
            .mapNotNull { it.text().ifBlank { null } }

        val alternativeTitles = TextExtractor
            .get(doc.selectFirst("#editassociated"))
            .split("\n")
            .mapNotNull { it.trim().ifBlank { null } }

        DatabaseInterface.BookData(
            title = doc.selectFirst(".seriestitlenu")?.text()?.trim() ?: "",
            description = TextExtractor.get(doc.selectFirst("#editdescription")).trim(),
            alternativeTitles = alternativeTitles,
            relatedBooks = relatedBooks,
            similarRecommended = similarRecommended,
            bookType = doc.selectFirst(".genre, .type")?.text() ?: "",
            genres = genres,
            tags = tags,
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
