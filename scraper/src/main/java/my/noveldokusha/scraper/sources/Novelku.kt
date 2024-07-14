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

class Novelku(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "novelku"
    override val nameStrId = R.string.source_name_novelku
    override val baseUrl = "https://novelku.id/"
    override val catalogUrl = "https://novelku.id/"
    override val iconUrl =
        "https://novelku.id/wp-content/uploads/2022/03/cropped-fvc-novelku.id_-32x32.png"
    override val language = LanguageCode.INDONESIAN

    private suspend fun getPagesList(
        index: Int,
        url: String,
        isSearch: Boolean = false,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(
                        if (isSearch) ".c-tabs-item__content .tab-thumb a"
                        else "div.page-item-detail .item-thumb a"
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
                            isLastPage = doc.selectFirst(".nav-previous") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".container#chapter-heding")?.text() ?: ""
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".read-container .text-left")!!.let {
                it.select("script").remove()
                TextExtractor.get(it)
            }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".summary_image img")
                    ?.attr("data-src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst(".summary__content")?.let {
                    TextExtractor.get(it)
                }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            // var url = postRequest(url = bookUrl.toUrlBuilderSafe().addPath("ajax", "chapters").toString())
            val postData =
                postRequest(url = bookUrl.toUrlBuilderSafe().addPath("ajax", "chapters").toString())
            tryConnect {
                networkClient
                    .call(postData)
                    .toDocument()
                    .select("li[class=wp-manga-chapter]")
                    .map {
                        it.selectFirst("span")?.remove()
                        ChapterResult(it.text() ?: "", it.selectFirst("a")?.attr("href") ?: "")
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
                    .add("s" to input, "post_type" to "wp-manga")
                    .toString()
            getPagesList(index, url, true)
        }
}
