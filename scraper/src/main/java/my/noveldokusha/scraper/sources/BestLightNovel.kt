package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.addPath
import my.noveldokusha.network.ifCase
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

class BestLightNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "best_light_novel"
    override val nameStrId = R.string.source_name_bestlightnovel
    override val baseUrl = "https://bestlightnovel.com/"
    override val catalogUrl = "https://bestlightnovel.com/novel_list"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst("#vung_doc")!!.let(TextExtractor::get)
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ) = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".info_image > img[src]")
                ?.attr("src")
        }
    }


    override suspend fun getBookDescription(
        bookUrl: String
    ) = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("#noidungm")
                ?.let {
                    it.select("h2").remove()
                    TextExtractor.get(it)
                }
        }
    }

    override suspend fun getChapterList(bookUrl: String) =
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .select("div.chapter-list a[href]")
                .map {
                    ChapterResult(
                        title = it.text(),
                        url = it.attr("href")
                    )
                }
                .reversed()
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        tryFlatConnect {
            val page = index + 1
            val url = catalogUrl
                .toUrlBuilderSafe()
                .ifCase(page != 1) {
                    add("type", "newest")
                    add("category", "all")
                    add("state", "all")
                    add("page", page)
                }
            parseToBooks(networkClient.get(url).toDocument(), index)
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> {
        if (input.isBlank()) return Response.Success(PagedList.createEmpty(index = index))

        val page = index + 1
        return tryFlatConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("search_novels", input.replace(" ", "_"))
                .ifCase(page != 1) { add("page", page) }
            parseToBooks(networkClient.get(url).toDocument(), index)
        }
    }

    private fun parseToBooks(doc: Document, index: Int): Response<PagedList<BookResult>> {

        return doc.select(".update_item.list_category")
            .mapNotNull {
                val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                BookResult(
                    title = link.attr("title"),
                    url = link.attr("href"),
                    coverImageUrl = bookCover
                )
            }
            .let {
                Response.Success(
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = when (val nav = doc.selectFirst("div.phan-trang")) {
                            null -> true
                            else -> nav.children().takeLast(2).firstOrNull()?.`is`(".pageselect")
                                ?: true
                        }
                    )
                )
            }
    }
}
