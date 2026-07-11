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

class BacaLightnovel(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "baca_lightnovel"
    override val nameStrId = R.string.source_name_baca_lightnovel
    override val baseUrl = "https://bacalightnovel.co/"
    override val catalogUrl = "https://bacalightnovel.co/series/"
    override val iconUrl =
        "https://bacalightnovel.co/wp-content/uploads/2022/09/cropped-fav-32x32.png"
    override val language = LanguageCode.INDONESIAN

    private suspend fun getPagesList(
        index: Int,
        url: String,
        isSearch: Boolean = false,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(".listupd > .maindet .mdthumb a")
                    .mapNotNull {
                        BookResult(
                            title = it.selectFirst("img")?.attr("title") ?: "",
                            url = it.attr("href") ?: "",
                            coverImageUrl = it.selectFirst("img")?.attr("src") ?: "",
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = if (isSearch) doc.selectFirst(".bixbox .pagination .next") == null else doc.selectFirst(".bixbox .hpage .r") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) { doc.selectFirst("h1[class=entry-title]")?.text() ?: "" }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst("div .epcontent[itemprop=text] .text-left")!!.let {
                TextExtractor.get(it)
            }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst(".sertothumb img")?.attr("src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst("div[itemprop=description]")
                    ?.let { TextExtractor.get(it) }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .select(".eplister li")
                    .map {
                        ChapterResult(
                            it.selectFirst(".epl-title")?.text() ?: "",
                            it.selectFirst("a")?.attr("href") ?: ""
                        )
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
                    .ifCase(page > 1) { add("page" to page.toString()) }
                    .add("order" to "populer")
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
            getPagesList(index, url, true)
        }
}
