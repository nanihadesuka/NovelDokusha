package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.addPath
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.network.tryFlatConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
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
    override val nameStrId = R.string.source_name_read_novel_full
    override val baseUrl = "https://readnovelfull.com/"
    override val catalogUrl = "https://readnovelfull.com/novel-list/most-popular-novel"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst("#chr-content")!!
            .let { TextExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".book")
                ?.select("img[src]")
                ?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument().selectFirst("#tab-description")
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val id = doc.selectFirst("#rating")!!.attr("data-novel-id")
            val url = "https://readnovelfull.com/ajax/chapter-archive"
                .toUrlBuilderSafe()
                .add("novelId", id)
                .toString()

            networkClient.get(url)
                .toDocument()
                .select("a[href]")
                .map {
                    ChapterResult(
                        title = it.text(),
                        url = baseUrl + it.attr("href").removePrefix("/")
                    )
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryFlatConnect {
            val page = index + 1
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
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryFlatConnect {
            if (input.isBlank())
                return@tryFlatConnect Response.Success(PagedList.createEmpty(index = index))

            val page = index + 1
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

    private suspend fun parseToBooks(
        doc: Document,
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            doc.selectFirst(".col-novel-main.archive")!!
                .select(".row")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookResult(
                        title = link.text(),
                        url = baseUrl + link.attr("href").removePrefix("/"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = when (val nav = doc.selectFirst("ul.pagination")) {
                            null -> true
                            else -> nav.children().last()?.`is`(".disabled") ?: true
                        }
                    )
                }
        }
    }
}
