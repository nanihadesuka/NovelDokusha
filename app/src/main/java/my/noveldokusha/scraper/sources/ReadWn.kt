package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


class ReadWn : SourceInterface.catalog
{
    override val name = "ReadWn"
    override val baseUrl = "https://www.readwn.com/"
    override val catalogUrl = "https://www.readwn.com/list/all/all-onclick-0.html"
    override val language = "English"

    override suspend fun getChapterText(doc: Document): String
    {
        return doc.selectFirst(".chapter-content")!!.let {
            textExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst(".cover > img[src]")
            ?.dataset()?.get(baseUrl + "src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst(".summary > .content")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        
        val id = doc.selectFirst(".pagination a[href]")!!.attr( "href")
        return connect(baseUrl + id)
            .addHeaderRequest()

            .getIO()
            .select(".chapter-list a[href]")
           .map { ChapterMetadata(title = it.attr("strong"), url = baseUrl + it.attr("href")) }



    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = catalogUrl.toUrlBuilder()!!.apply {
            if (page > 1) addPath(page.toString())
        }

        return tryConnect {
            fetchDoc(catalogUrl)
                .select(".novel-item a[href]")
                .map { BookMetadata(title = it.attr("title"), url = baseUrl + it.attr("href")) }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        if (input.isBlank() || index > 0)
            return Response.Success(listOf())

        return tryConnect {
            val json = connect("https://www.readwn.com/search.html")
                .addHeaderRequest()
                .data("inputContent", input)
                .ignoreContentType(true)
                .postIO()
                .text()

            JsonParser
                .parseString(json)
                .asJsonObject["resultview"]
                .asString
                .let { Jsoup.parse(it) }
                .let { getBooksList(it) }
                .let { Response.Success(it) }
        }
    }

    fun getBooksList(element: Element) = element
        .select(".novel-item")
        .mapNotNull {
            val coverUrl = it.selectFirst(".novel-cover > img[src]")?.attr("src") ?: ""
            val book = it.selectFirst("a[title]") ?: return@mapNotNull null
            BookMetadata(
                title = book.attr("title"),
                url = baseUrl + book.attr("href").removePrefix("/"),
                coverImageUrl = coverUrl
            )
        }
}
