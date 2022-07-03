package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.cookiesData
import my.noveldokusha.data.headersData
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated
 * Chapter url example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated/1348-chapter-0
 */
class LightNovelWorld : SourceInterface.Catalog {
    override val name = "Light Novel World"
    override val baseUrl = "https://www.lightnovelworld.com/"
    override val catalogUrl = "https://www.lightnovelworld.com/genre/all/popular/all/"
    override val language = "English"

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst("#chapter-container")!!.let {
            textExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".cover > img[^src]")
            ?.dataset()?.get("src")
    }

    override suspend fun getBookDescripton(doc: Document): String? {
        return doc.selectFirst(".summary > .content")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {

        val list = mutableListOf<ChapterMetadata>()
        for (page in 1..Int.MAX_VALUE) {
            val urlBuilder = doc
                .location()
                .toUrlBuilder()!!
                .addPath("chapters", "page-$page")

            val res = fetchDoc(urlBuilder)
                .select(".chapter-list > li > a")
                .map {
                    ChapterMetadata(
                        title = it.attr("title"),
                        url = baseUrl + it.attr("href").removePrefix("/")
                    )
                }

            if (res.isEmpty())
                break
            list.addAll(res)
        }
        return list
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        val url = catalogUrl.toUrlBuilder()!!.apply {
            if (page > 1) addPath(page.toString())
        }

        return tryConnect {
            getBooksList(fetchDoc(url), index)
        }
    }


    // TODO(): Doesn't seem to work anymore, 400 response error
    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            val json = connect("https://www.lightnovelworld.com/lnsearchlive")
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
                .let { getBooksList(it, index) }
        }
    }

    private fun getBooksList(doc: Document, index: Int) = doc
        .select(".novel-item")
        .mapNotNull {
            val coverUrl = it.selectFirst(".novel-cover > img[src]")?.attr("src") ?: ""
            val book = it.selectFirst("a[title]") ?: return@mapNotNull null
            BookMetadata(
                title = book.attr("title"),
                url = baseUrl + book.attr("href").removePrefix("/"),
                coverImageUrl = coverUrl
            )
        }.let {
            Response.Success(
                PagedList(
                    list = it,
                    index = index,
                    isLastPage = when (val nav = doc.selectFirst("ul.pagination")) {
                        null -> true
                        else -> nav.children().last()?.`is`(".active") ?: true
                    }
                )
            )
        }
}
