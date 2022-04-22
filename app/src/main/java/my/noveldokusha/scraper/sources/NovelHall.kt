
package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class NovelHall : SourceInterface.catalog
{
    override val name = "NovelHall"
    override val baseUrl = "https://www.novelhall.com"
    override val catalogUrl = "https://www.novelhall.com/all-1.html"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String
    {
        return doc.selectFirst("div#htmlContent")!!.let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst(".book-img.hidden-xs")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst("span.js-close-wrap")
            ?.let {
                textExtractor.get(it)
            }
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
       if (page > 1)
         return Response.Success(listOf())

        return tryConnect {
            fetchDoc(catalogUrl)
                .select("tbody tr td ul > .btm  a[href]")
                .map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
   {
        if (input.isBlank() || index > 0)
            return Response.Success(listOf())
    val search = "https://www.novelhall.com/index.php?s=so&module=book&"
        val url = search.toUrlBuilderSafe().apply {
            add("keyword", input)
        }

        return tryConnect {
            fetchDoc(url)
                .select(".section3.inner.mt30")
                .mapNotNull {
                    val link = it.selectFirst("td:nth-child(2) a[href]") ?: return@mapNotNull null
                  //  val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                       url = baseUrl + link.attr("href"),
                      // coverImageUrl = getBookCoverImageUrl
                    )
                }
                .let { Response.Success(it) }
        }
    }
}
