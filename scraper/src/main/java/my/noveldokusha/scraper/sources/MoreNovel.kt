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

class MoreNovel(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "more_webnovel"
    override val nameStrId = R.string.source_name_more_novel
    override val baseUrl = "https://morenovel.net/"
    override val catalogUrl = "https://morenovel.net/novel/?m_orderby=views"
    override val iconUrl = "https://morenovel.net/wp-content/uploads/2020/03/cropped-m2-32x32.png"
    override val language = LanguageCode.INDONESIAN

    suspend fun getPagesList(
        index: Int,
        url: String,
        isSearch: Boolean = false,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(
                        if (isSearch) ".tab-content-wrap .c-tabs-item .tab-thumb a"
                        else ".tab-content-wrap .c-tabs-item .item-thumb a"
                    )
                    .mapNotNull {
                        BookResult(
                            title = it.attr("title"),
                            url = it.attr("href"),
                            coverImageUrl = it.selectFirst("img")?.attr("data-src") ?: "",
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst(".nextpostslink") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) { doc.selectFirst("#chapter-heading")?.text() ?: "" }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".reading-content .text-left")?.let { 
              it.select("div").remove()
              TextExtractor.get(it) 
            } ?: ""
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".profile-manga .summary_image img ")
                    ?.attr("data-src") ?: ""
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".content-area .summary__content p")
                    ?.let { TextExtractor.get(it) }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            val postData =
                postRequest(url = bookUrl.toUrlBuilderSafe().addPath("ajax", "chapters").toString())
            tryConnect {
                networkClient
                    .call(postData)
                    .toDocument()
                    .select("li[class=wp-manga-chapter]")
                    .map {
                        it?.selectFirst("span")?.remove()
                        ChapterResult(it?.text() ?: "", it?.selectFirst("a")?.attr("href") ?: "")
                    }
                    .reversed()
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            val page = index + 1
            val url =
                catalogUrl
                    .toUrlBuilderSafe()
                    .ifCase(page > 1) { addPath("page", page.toString()) }
                    .toString()
            getPagesList(index, url)
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            val page = index + 1
            val url =
                baseUrl
                    .toUrlBuilderSafe()
                    .ifCase(page > 1) { addPath("page", page.toString()) }
                    .add("s" to input, "post_type" to "wp-manga", "m_orderby" to "views")
                    .toString()
            getPagesList(index, url, true)
        }
}
