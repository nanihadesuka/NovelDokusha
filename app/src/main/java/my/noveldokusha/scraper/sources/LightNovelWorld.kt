package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.postPayload
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilder
import my.noveldokusha.utils.toUrlBuilderSafe
import okhttp3.Headers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated
 * Chapter url example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated/1348-chapter-0
 */
class LightNovelWorld(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "light_novel_world"
    override val name = "Light Novel World"
    override val baseUrl = "https://www.lightnovelworld.com/"
    override val catalogUrl = "https://www.lightnovelworld.com/genre/all/popular/all/"
    override val iconUrl =
        "https://static.lightnovelworld.com/content/img/lightnovelworld/favicon.png"
    override val language = "English"

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst("#chapter-container")?.let(TextExtractor::get) ?: ""
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".cover > img[data-src]")
                ?.attr("data-src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".summary > .content")
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val list = mutableListOf<ChapterMetadata>()
            val baseChaptersUrl = bookUrl
                .toUrlBuilderSafe()
                .addPath("chapters")

            for (page in 1..Int.MAX_VALUE) {
                val urlBuilder = baseChaptersUrl.addPath("page-$page")

                val res = networkClient.get(urlBuilder)
                    .toDocument()
                    .select(".chapter-list > li > a")
                    .map {
                        ChapterMetadata(
                            title = it.attr("title"),
                            url = baseUrl + it.attr("href").removePrefix("/")
                        )
                    }

                if (res.isEmpty())
                    break
                list.addAll(res)
            }
            list
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = catalogUrl.toUrlBuilder()!!.apply {
                if (page > 1) addPath(page.toString())
            }
            getBooksList(networkClient.get(url).toDocument(), index)
        }
    }


    // TODO(): Doesn't seem to work anymore, 400 response error
    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            val request = postRequest(
                url = "https://www.lightnovelworld.com/lnsearchlive",
                headers = Headers.Builder().apply {
                    add("accept", """*/*""")
                    add("accept-encoding", """gzip, deflate, br""")
                    add(
                        "accept-language",
                        """en-GB,en-US;q=0.9,en;q=0.8,ca;q=0.7,es-ES;q=0.6,es;q=0.5,de;q=0.4"""
                    )
                    add("cache-control", """no-cache""")
                    add("content-length", """16""")
                    add("content-type", """application/x-www-form-urlencoded; charset=UTF-8""")
                    add(
                        "cookie",
                        """lncoreantifrg=CfDJ8I0f6r3I-mpDp8gg0LfwguD096gc7bPUCNwK5qYMQGeRvnTkCnDiq8ojv0o30LfGCRq3E5uAavI_vMrz3HKgtYWoxZG242sHov1_deI8blXoWhqCOAgdbwgF8ObSN8O76Op5lkigLECS4ZxWeCi2lCw"""
                    )
                    add("dnt", """1""")
                    add(
                        "lnrequestverifytoken",
                        """CfDJ8I0f6r3I-mpDp8gg0LfwguDqv1-Muj3LDHQZA6e0PXfX44NvoAizhMliI37Gkf3tKKsm7Tco5iQ5NxmuHFxT_joq_AIFHsxeJB0ZD7GkWYrTxow0lJo8vQAAQd5MdChlHTXI15seYyDTpUS0NsuxraU"""
                    )
                    add("origin", """https://www.lightnovelworld.com""")
                    add("pragma", """no-cache""")
                    add("referer", """https://www.lightnovelworld.com/search""")
                    add(
                        "sec-ch-ua",
                        """".Not/A)Brand";v="99", "Google Chrome";v="103", "Chromium";v="103""""
                    )
                    add("sec-ch-ua-mobile", """?0""")
                    add("sec-ch-ua-platform", """Windows""")
                    add("sec-fetch-dest", """empty""")
                    add("sec-fetch-mode", """cors""")
                    add("sec-fetch-site", """same-origin""")
                    add(
                        "user-agent",
                        """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36"""
                    )
                    add("x-requested-with", """XMLHttpRequest""")
                }.build()
            ).postPayload {
                add("inputContent", input)
            }

            // content-encoding:br
            // Needs okhttp brotli dependency to decode
            val json = networkClient.call(request)
                .body
                .string()

            JsonParser
                .parseString(json)
                .asJsonObject["resultview"]
                .asString
                .let { Jsoup.parse(it) }
                .let { getBooksList(it, index) }
        }
    }

    private fun getBooksList(doc: Document, index: Int) = doc
        .select(".novel-item")
        .mapNotNull {
            val coverUrl =
                it.selectFirst(".novel-cover > img[data-src]")?.attr("data-src") ?: ""
            val book = it.selectFirst("a[title]") ?: return@mapNotNull null
            BookMetadata(
                title = book.attr("title"),
                url = baseUrl + book.attr("href").removePrefix("/"),
                coverImageUrl = coverUrl
            )
        }.let {
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
