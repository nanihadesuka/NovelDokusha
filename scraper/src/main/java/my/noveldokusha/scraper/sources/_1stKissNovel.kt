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
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

class _1stKissNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "1stkissnovel"
    override val nameStrId = R.string.source_name_1stkissnovel
    override val catalogUrl = "https://1stkissnovel.love/novel/?m_orderby=alphabet"
    override val baseUrl = "https://1stkissnovel.love/"
    override val iconUrl =
        "https://1stkissnovel.org/wp-content/uploads/2023/04/cropped-Im-3-32x32.png"
    override val language = LanguageCode.ENGLISH


    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("div.summary_image")
                ?.selectFirst("img[src]")
                ?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".summary__content.show-more")
                ?.let { TextExtractor.get(it) }
        }
    }

    // TODO() not working, website blocking calls
    override suspend fun getChapterList(
        bookUrl: String
    ) = withContext(Dispatchers.Default) {
        tryConnect {
            val url = bookUrl
                .toUrlBuilderSafe()
                .addPath("ajax", "chapters")
                .toString()

            val request = postRequest(url)

            networkClient
                .call(request)
                .toDocument()
                .select(".wp-manga-chapter a[href]")
                .map {
                    ChapterResult(
                        title = it.text(),
                        url = it.attr("href")
                    )
                }
                .reversed()
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ) = withContext(Dispatchers.Default) {
        tryConnect {
            val url = when (val page = index + 1) {
                1 -> catalogUrl
                else -> baseUrl
                    .toUrlBuilderSafe()
                    .addPath("novel", "page", page.toString())
                    .add("m_orderby", "alphabet")
                    .toString()
            }

            val doc = networkClient.get(url).toDocument()
            doc.select(".page-item-detail")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[src]")?.attr("src")
                    BookResult(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = isLastPage(doc)
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ) = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = baseUrl
                .toUrlBuilderSafe()
                .ifCase(page != 1) { addPath("page", page.toString()) }
                .add("s", input)
                .add("post_type", "wp-manga")

            val doc = networkClient.get(url).toDocument()
            doc.select(".row.c-tabs-item__content")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[src]")?.attr("src")
                    BookResult(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = isLastPage(doc)
                    )
                }
        }
    }

    private fun isLastPage(doc: Document) = when (val nav = doc.selectFirst("div.wp-pagenavi")) {
        null -> true
        else -> nav.children().last()?.`is`(".current") ?: true
    }
}