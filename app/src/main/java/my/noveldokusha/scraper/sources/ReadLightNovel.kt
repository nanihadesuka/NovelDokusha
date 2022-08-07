package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.*
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilderSafe
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.readlightnovel.org/goat-of-all-ghouls-1
 * Chapter url example:
 * https://www.readlightnovel.org/goat-of-all-ghouls-1/chapter-1
 */
// TODO() missing cookies, doesn't work
class ReadLightNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {

    override val name = "Read Light Novel"
    override val baseUrl = "https://www.readlightnovel.me/"
    override val catalogUrl = "https://www.readlightnovel.me/top-novels/new/1"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? =
        doc.selectFirst(".chapter-content3 h4")?.text()

    override suspend fun getChapterText(doc: Document): String {
        doc.selectFirst(".chapter-content3 > .desc")!!.let {
            it.select("script").remove()
            it.select("a").remove()
            it.select(".ads-title").remove()
            it.select(".hidden").remove()
            return TextExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".novel-cover img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst("h6:containsOwn(Description)")
            ?.parent()
            ?.nextElementSibling()
            ?.let { TextExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc.select(".chapter-chs").select("a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {
            val url = "https://www.readlightnovel.me"
                .toUrlBuilderSafe()
                .addPath("top-novels", "new", page.toString())

            val doc = networkClient.get(url).toDocument()
            doc.select(".top-novel-block")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
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
    }

    // TODO() missing cookies, doesn't work
    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            val request = postRequest("https://www.readlightnovel.org/search/autocomplete")
                .postPayload {
                    add("q", input)
                }

            return@tryConnect networkClient.call(request)
                .toDocument()
                .children()
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = true
                        )
                    )
                }
        }
    }
}
