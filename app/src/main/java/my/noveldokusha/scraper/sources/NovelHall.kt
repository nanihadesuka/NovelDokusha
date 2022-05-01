package my.noveldokusha.scraper.sources

import android.net.Uri
import android.util.Log
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class NovelHall : SourceInterface.catalog
{
    override val name = "NovelHall"
    override val baseUrl = "https://www.novelhall.com/"
    override val catalogUrl = "https://www.novelhall.com/all.html"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String
    {
        return doc
            .selectFirst("div#htmlContent")!!
            .let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc
            .selectFirst(".book-img.hidden-xs")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc
            .selectFirst("span.js-close-wrap")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        return doc.select("#morelist a[href]").map {
            ChapterMetadata(title = it.text(), url = baseUrl + it.attr("href"))
        }
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = baseUrl.toUrlBuilderSafe().apply {
            if (page == 1) addPath("all.html")
            else addPath("all-$page.html")
        }

        return extractBooksCatalog(url)
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        if (input.isBlank())
            return Response.Success(listOf())

        val url = baseUrl.toUrlBuilderSafe().apply {
            addPath("index.php")
            add("s", "so")
            add("module", "book")
            add("keyword", input)
        }
        return extractBooksSearch(url)
    }

    private suspend fun extractBooksCatalog(url: Uri.Builder) = tryConnect {
        fetchDoc(url)
            .select("li.btm")
            .mapNotNull {
                val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                BookMetadata(
                    title = link.text(),
                    url = baseUrl + link.attr("href").removePrefix("/"),
                )
            }
            .let { Response.Success(it) }
    }

    private suspend fun extractBooksSearch(url: Uri.Builder) = tryConnect {
        fetchDoc(url)
            .selectFirst(".section3.inner.mt30 > table")
            ?.select("tr > td:nth-child(2) > a[href]")
            .let { it ?: listOf() }
            .map { link ->
                BookMetadata(
                    title = link.text(),
                    url = baseUrl + link.attr("href").removePrefix("/"),
                )
            }
            .let { Response.Success(it) }
    }
}
