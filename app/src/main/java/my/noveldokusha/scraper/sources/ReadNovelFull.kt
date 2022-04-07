package my.noveldokusha.scraper.sources

import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god-v2.html
 * Chapter url example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god/chapter-1-starting-over-v1.html
 */
class ReadNovelFull : SourceInterface.catalog
{
    override val name = "Read Novel Full"
    override val baseUrl = "https://readnovelfull.com/"
    override val catalogUrl = "https://readnovelfull.com/most-popular-novel"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String
    {
        doc.selectFirst("#chr-content")!!.let {
            return textExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst(".book")
            ?.select("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst("#tab-description")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        val id = doc.selectFirst("#rating")!!.attr("data-novel-id")
        return connect("https://readnovelfull.com/ajax/chapter-archive")
            .addHeaderRequest()
            .data("novelId", id)
            .getIO()
            .select("a[href]")
            .map { ChapterMetadata(title = it.text(), url = baseUrl + it.attr("href").removePrefix("/")) }
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        return tryConnect {
            val url = catalogUrl.toUrlBuilder()!!.apply {
                if (page > 1) add("page", page)
            }
            parseToBooks(url)
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        if (input.isBlank())
            return Response.Success(listOf())

        val page = index + 1
        return tryConnect {
            val url = baseUrl.toUrlBuilder()!!.apply {
                appendPath("search")
                add("keyword", input)
                if (page > 1) add("page", page)
            }
            parseToBooks(url)
        }
    }

    suspend fun parseToBooks(url: Uri.Builder): Response<List<BookMetadata>>
    {
        return fetchDoc(url)
            .selectFirst(".col-novel-main.archive")!!
            .select(".row")
            .mapNotNull {
                val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                BookMetadata(
                    title = link.text(),
                    url = baseUrl + link.attr("href").removePrefix("/"),
                    coverImageUrl = bookCover
                )
            }
            .let { Response.Success(it) }
    }

}
