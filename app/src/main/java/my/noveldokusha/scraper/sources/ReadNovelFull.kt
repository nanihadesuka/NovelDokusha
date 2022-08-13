package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.Response
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.add
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilderSafe
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god-v2.html
 * Chapter url example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god/chapter-1-starting-over-v1.html
 */
class ReadNovelFull(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "read_novel_full"
    override val name = "Read Novel Full"
    override val baseUrl = "https://readnovelfull.com/"
    override val catalogUrl = "https://readnovelfull.com/novel-list/most-popular-novel"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst("#chr-content")!!
            .let { TextExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".book")
            ?.select("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst("#tab-description")
            ?.let { TextExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val id = doc.selectFirst("#rating")!!.attr("data-novel-id")
        val url = "https://readnovelfull.com/ajax/chapter-archive"
            .toUrlBuilderSafe()
            .add("novelId", id)
            .toString()

        return networkClient.get(url)
            .toDocument()
            .select("a[href]")
            .map {
                ChapterMetadata(
                    title = it.text(),
                    url = baseUrl + it.attr("href").removePrefix("/")
                )
            }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {
            val url = catalogUrl
                .toUrlBuilderSafe()
                .apply {
                    if (page > 1) add("page", page)
                }
            val doc = networkClient.get(url).toDocument()
            parseToBooks(doc, index)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank())
            return Response.Success(PagedList.createEmpty(index = index))

        val page = index + 1
        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("novel-list", "search")
                .apply {
                    add("keyword", input)
                    if (page > 1) add("page", page)
                }.toString()

            val doc = networkClient.get(url).toDocument()
            parseToBooks(doc, index)
        }
    }

    private fun parseToBooks(doc: Document, index: Int): Response<PagedList<BookMetadata>> {
        return doc.selectFirst(".col-novel-main.archive")!!
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
            .let {
                Response.Success(
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = when (val nav = doc.selectFirst("ul.pagination")) {
                            null -> true
                            else -> nav.children().last()?.`is`(".disabled") ?: true
                        }
                    )
                )
            }
    }
}
