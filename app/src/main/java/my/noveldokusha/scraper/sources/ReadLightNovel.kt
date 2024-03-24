package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.postPayload
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilderSafe
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.readlightnovel.today/goat-of-all-ghouls-1
 * Chapter url example:
 * https://www.readlightnovel.today/goat-of-all-ghouls-1/chapter-1
 */
class ReadLightNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "read_light_novel"
    override val nameStrId = R.string.source_name_read_light_novel
    override val baseUrl = "https://www.readlightnovel.today/"
    override val catalogUrl = "https://www.readlightnovel.today/top-novels/new/1"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".chapter-content3 h4")?.text()
        }

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst(".chapter-content3 > .desc")!!.let {
            it.select("script").remove()
            it.select("a").remove()
            it.select(".ads-title").remove()
            it.select(".hidden").remove()
            TextExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".novel-cover img[src]")
                ?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("h6:containsOwn(Description)")
                ?.parent()
                ?.nextElementSibling()
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .select(".chapter-chs")
                .select("a[href]")
                .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index") {
            val page = index + 1
            val url = baseUrl
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
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = when (val nav = doc.selectFirst("ul.pagination")) {
                            null -> true
                            else -> nav.children().last()?.`is`(".active") ?: true
                        }
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            val request = postRequest("${baseUrl}search/autocomplete")
                .addHeader("accept", "*/*")
                .addHeader("accept-encoding", "gzip, deflate, br")
                .addHeader(
                    "accept-language",
                    "en-GB,en-US;q=0.9,en;q=0.8,ca;q=0.7,es-ES;q=0.6,es;q=0.5,de;q=0.4"
                )
                .addHeader("cache-control", "no-cache")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("origin", baseUrl)
                .addHeader("pragma", "no-cache")
                .addHeader("referer", baseUrl)
                .addHeader("sec-ch-ua-platform", "Windows")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .postPayload {
                    add("q", input)
                }

            networkClient.call(request)
                .toDocument()
                .select("li")
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
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = true
                    )
                }
        }
    }
}
