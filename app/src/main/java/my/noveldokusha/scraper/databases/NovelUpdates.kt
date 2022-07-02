package my.noveldokusha.scraper.databases

import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class NovelUpdates : DatabaseInterface {
    override val id = "novel_updates"
    override val name = "Novel Updates"
    override val baseUrl = "https://www.novelupdates.com/"

    override suspend fun getSearchAuthorSeries(
        index: Int,
        urlAuthorPage: String
    ): Response<PagedList<BookMetadata>> {
        if (index > 0) return Response.Success(
            PagedList(list = listOf(), pageIndex = index, isLastPage = true)
        )

        return tryConnect {
            fetchDoc(urlAuthorPage)
                .select("div.search_title > a[href]")
                .map { BookMetadata(title = it.text(), url = it.attr("href")) }
                .let {
                    Response.Success(
                        PagedList(list = it, pageIndex = index, isLastPage = true)
                    )
                }
        }
    }

    override suspend fun getSearchGenres(): Response<Map<String, String>> {
        return searchGenresCache.fetch {
            tryConnect {
                fetchDoc("https://www.novelupdates.com/series-finder/")
                    .select(".genreme")
                    .associate { it.text().trim() to it.attr("genreid") }
                    .let { Response.Success(it) }
            }
        }
    }

    override suspend fun getSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        val page = index + 1
        val url = baseUrl.toUrlBuilderSafe().apply {
            if (page > 1) appendPath("page").appendPath(page.toString())
            add("s", input)
            add("post_type", "seriesplans")
        }

        return getSearchList(page, url)
    }

    override suspend fun getSearchAdvanced(
        index: Int,
        genresIncludedId: List<String>,
        genresExcludedId: List<String>
    ): Response<PagedList<BookMetadata>> {
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

        return getSearchList(index, url)
    }

    private suspend fun getSearchList(index: Int, url: Uri.Builder) = tryConnect(
        extraErrorInfo = "index: $index\n\nurl: $url"
    ) {
        val doc = fetchDoc(url)
        doc.select(".search_main_box_nu")
            .mapNotNull {
                val title = it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                BookMetadata(
                    title = title.text(),
                    url = title.attr("href"),
                    coverImageUrl = image
                )
            }
            .let {
                val isLastPage = when (val nav = doc.selectFirst("div.nav-links")) {
                    null -> true
                    else -> nav.children().last()?.`is`(".current") ?: true
                }
                Response.Success(
                    PagedList(
                        list = it,
                        pageIndex = index,
                        isLastPage = isLastPage
                    )
                )
            }
    }

    override fun getBookData(doc: Document): DatabaseInterface.BookData {
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
            .map { DatabaseInterface.BookAuthor(name = it.text(), url = it.attr("href")) }

        return DatabaseInterface.BookData(
            title = doc.selectFirst(".seriestitlenu")?.text() ?: "",
            description = textExtractor.get(doc.selectFirst("#editdescription")).trim(),
            alternativeTitles = textExtractor.get(doc.selectFirst("#editassociated")).split("\n"),
            relatedBooks = relatedBooks,
            similarRecommended = similarRecommended,
            bookType = doc.selectFirst(".genre, .type")?.text() ?: "",
            genres = doc.select("#seriesgenre a").map { it.text() },
            tags = doc.select("#showtags a").map { it.text() },
            authors = authors,
            coverImageUrl = doc.selectFirst("div.seriesimg > img[src]")?.attr("src")
        )
    }
}
