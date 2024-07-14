package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.addPath
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.ifCase
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import okhttp3.Headers
import org.jsoup.nodes.Document

class NovelBin(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "Novelbin"
    override val nameStrId = R.string.source_name_novelbin
    override val baseUrl = "https://novelbin.me/"
    override val catalogUrl = "https://novelbin.me/sort/novelbin-daily-update"
    override val iconUrl = "https://novelbin.me/img/logo.png"
    override val language = LanguageCode.ENGLISH

    private suspend fun getPagesList(index: Int, url: String) =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(url).toDocument().run {
                    val isLastPage = select("ul.pagination li.next.disabled").isEmpty()
                    val bookResults =
                        select("#list-page div.list-novel .row").mapNotNull {
                            val link = it.selectFirst("div.col-xs-7 a") ?: return@mapNotNull null
                            val bookCover =
                                it.selectFirst("div.col-xs-3 > div > img")?.attr("data-src") ?: ""
                            BookResult(
                                title = link.attr("title"),
                                url = link.attr("href"),
                                coverImageUrl = bookCover
                            )
                        }
                    PagedList(list = bookResults, index = index, isLastPage = !isLastPage)
                }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) { doc.selectFirst("h2 > .title-chapter")?.text() ?: "" }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".container .adsads")!!.let { TextExtractor.get(it) }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst("meta[itemprop=image]")
                    ?.attr("content")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst("div.desc-text")?.text()
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            tryConnect {
                val keyId =
                    networkClient
                        .get(bookUrl)
                        .toDocument()
                        .selectFirst("a#btn-follow")!!
                        .attr("data-id") ?: ""

                getRequest(
                        url =
                            baseUrl
                                .toUrlBuilderSafe()
                                .addPath("ajax", "chapter-archive")
                                .add("novelId" to keyId)
                                .toString(),
                        headers =
                            Headers.Builder()
                                .add("Accept", "*/*")
                                .add("X-Requested-With", "XMLHttpRequest")
                                .add(
                                    "User-Agent",
                                    "Mozilla/5.0 (Android 13; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0"
                                )
                                .add("Referer", "$bookUrl#tab-chapters-title")
                                .build()
                    )
                    .let { networkClient.call(it) }
                    .toDocument()
                    .select("ul.list-chapter li a")
                    .map { ChapterResult(it.attr("title") ?: "", it.attr("href") ?: "") }
                    .reversed()
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            val page = index + 1
            val url =
                catalogUrl
                    .toUrlBuilderSafe()
                    .ifCase(page > 1) { add("page", page.toString()) }
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
                    .addPath("search")
                    .add("keyword" to input)
                    .ifCase(page > 1) { add("page", page.toString()) }
                    .toString()
            getPagesList(index, url)
        }
}
