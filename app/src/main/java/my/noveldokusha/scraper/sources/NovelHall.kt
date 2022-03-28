package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel Example
 * https://www.novelhall.com/Apollo-s-Heart-15505/
 * Chapter url example:
 * https://www.novelhall.com/Apollo-s-Heart-15505/3304460.html
 */
class NovelHall : SourceInterface.catalog
{
    override val name = "Novel Hall"
    override val baseUrl = "https://www.novelhall.com/"
    override val catalogUrl = "https://www.novelhall.com/all.html"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String
    {
        doc.selectFirst("#htmlContent")!!.let {
            return textExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst(".book-img.hidden-xs")
            ?.select("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst("span.js-close-wrap")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        return doc.select("#morelist li.post-1 a").map {
            ChapterMetadata(title = it.text(), url = it.attr("href"))
        }.reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = catalogUrl.toUrlBuilder()!!.apply {
            if (page > 1) add("-2.html", page)
        }

        return tryConnect {
            fetchDoc(url)
                .selectFirst(".type")!!
                .select("li")
                .mapNotNull { it.selectFirst("a[href]") }
                .map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href").removePrefix("/")) }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        if (input.isBlank() || index > 0)
            return Response.Success(listOf())

        val url = baseUrl.toUrlBuilder()!!.apply {
            appendPath("search")
            add("keyword", input)
        }

        return tryConnect {
            fetchDoc(url)
                .selectFirst(".col-novel-main.archive")!!
                .select(".row")
                .mapNotNull { it.selectFirst("a[href]") }
                .map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href").removePrefix("/")) }
                .let { Response.Success(it) }
        }
    }
}
