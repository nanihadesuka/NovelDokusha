package my.noveldokusha.scraper.databases

import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class BakaUpdates : DatabaseInterface
{
    override val id = "baka_updates"
    override val name = "Baka-Updates"
    override val baseUrl = "https://www.mangaupdates.com/"

    private fun String.removeNovelTag() = this.removeSuffix("(Novel)").trim()

    override suspend fun getSearchAuthorSeries(index: Int, urlAuthorPage: String): Response<List<BookMetadata>>
    {
        if (index > 0) return Response.Success(listOf())

        return tryConnect {
            fetchDoc(urlAuthorPage)
                .select("div.pl-2.col-md-5.col-7.text-truncate.text")
                .map { it.select("a[href]")[1] }
                .filter { it.text().endsWith("(Novel)") }
                .map { BookMetadata(title = it.text(), url = it.attr("href")) }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getSearchGenres(): Response<Map<String, String>>
    {
        return searchGenresCache.fetch {
            tryConnect {
                fetchDoc("https://www.mangaupdates.com/series.html?act=genresearch")
                    .select(".p-1.col-6.text")
                    .map { it.text().trim() }
                    .associateWith { it.replace(" ", "+") }
                    .let { Response.Success(it) }
            }
        }
    }

    override suspend fun getSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = "https://www.mangaupdates.com/series.html".toUrlBuilderSafe().apply {
            if (page > 1) add("page", page)
            add("perpage", 50)
            add("type", "novel")
            add("search", input)
        }

        return getSearchList(page, url)
    }

    override suspend fun getSearchAdvanced(index: Int, genresIncludedId: List<String>, genresExcludedId: List<String>):
            Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = "https://www.mangaupdates.com/series.html".toUrlBuilderSafe().apply {
            if (page > 1) add("page", page)
            if (genresIncludedId.isNotEmpty()) add("genre", genresIncludedId.joinToString("_"))
            if (genresExcludedId.isNotEmpty()) add("exclude_genre", genresExcludedId.joinToString("_"))
            add("type", "novel")
            add("perpage", 50)
        }

        return getSearchList(page, url)
    }

    suspend fun getSearchList(page: Int, url: Uri.Builder) = tryConnect("page: $page\n\nurl: $url") {
        fetchDoc(url)
            .select(".col-12.col-lg-6.p-3.text")
            .mapNotNull {
                val link = it.selectFirst("div.text > a[href]") ?: return@mapNotNull null
                val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                val description = it.selectFirst("div.text.flex-grow-1")?.text()?.removeNovelTag() ?: ""
                BookMetadata(
                    title = link.text().removeNovelTag(),
                    url = link.attr("href"),
                    coverImageUrl = bookCover,
                    description = description
                )
            }
            .let { Response.Success(it) }
    }

    override fun getBookData(doc: Document): DatabaseInterface.BookData
    {
        fun entry(header: String) = doc.selectFirst("div.sCat > b:containsOwn($header)")!!.parent()!!.nextElementSibling()!!

        val relatedBooks = entry("Category Recommendations")
            .select("a[href]")
            .map { BookMetadata(it.text().removeNovelTag(), "https://www.mangaupdates.com/" + it.attr("href")) }
            .toList()

        val similarRecommended = entry("Recommendations")
            .select("a[href]")
            .map { BookMetadata(it.text().removeNovelTag(), "https://www.mangaupdates.com/" + it.attr("href")) }
            .toList()

        val authors = entry("Author\\(s\\)")
            .select("a[href]")
            .map {
                if (it.attr("href").startsWith("https://www.mangaupdates.com/authors.html"))
                    return@map DatabaseInterface.BookAuthor(name = it.text(), url = it.attr("href"))

                val authorName = it.previousSibling()!!.outerHtml().removeSuffix("&nbsp;[")
                return@map DatabaseInterface.BookAuthor(name = authorName, url = null)
            }

        val description = entry("Description").let {
            it.selectFirst("[id=div_desc_more]") ?: it.selectFirst("div")
        }.also {
            it?.select("a")?.remove()
        }.let { textExtractor.get(it!!) }

        val tags = entry("Categories")
            .select("li > a")
            .map { it.text() }

        val coverImageUrl = entry("Image")
            .selectFirst("img[src]")!!
            .attr("src")

        return DatabaseInterface.BookData(
            title = doc.selectFirst(".releasestitle.tabletitle")!!.text().removeNovelTag(),
            description = description,
            alternativeTitles = textExtractor.get(entry("Associated Names")).split("\n\n"),
            relatedBooks = relatedBooks,
            similarRecommended = similarRecommended,
            bookType = entry("Type").text(),
            genres = entry("Genre").select("a").dropLast(1).map { it.text() },
            tags = tags,
            authors = authors,
            coverImageUrl = coverImageUrl
        )
    }
}