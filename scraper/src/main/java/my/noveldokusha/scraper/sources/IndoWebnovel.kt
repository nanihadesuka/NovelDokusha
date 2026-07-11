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
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

class IndoWebnovel(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "indo_webnovel"
    override val nameStrId = R.string.source_name_indo_webnovel
    override val baseUrl = "https://indowebnovel.id/"
    override val catalogUrl = "https://indowebnovel.id/series/"
    override val iconUrl =
        "https://indowebnovel.id/wp-content/uploads/2022/12/cropped-Phoenix-PNG-32x32.png"
    override val language = LanguageCode.INDONESIAN

    private suspend fun getPagesList(
        index: Int,
        url: String,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(".flexbox2-item > .flexbox2-content")
                    .mapNotNull {
                        val link = it.selectFirst("a") ?: return@mapNotNull null
                        val bookCover = it.selectFirst(".flexbox2-thumb > img")?.attr("src") ?: ""
                        BookResult(
                            title = link.attr("title"),
                            url = link.attr("href"),
                            coverImageUrl = bookCover,
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst("div.pagination .next") == null,
                        )
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
                    .selectFirst(".series-thumb:has(img) img")
                    ?.attr("src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst(".series-synops")?.let {
                    it.selectFirst("h1")?.remove()
                    TextExtractor.get(it)
                }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .select(".series-chapterlist .flexch-infoz a")
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
                    .add("s" to input)
                    .toString()
            getPagesList(index, url)
        }
}
