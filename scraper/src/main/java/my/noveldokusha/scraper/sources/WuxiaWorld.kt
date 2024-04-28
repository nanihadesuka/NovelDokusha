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
import my.noveldokusha.network.toUrlBuilder
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult

class WuxiaWorld(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "wuxia_world"
    override val nameStrId = R.string.source_name_wuxia_world
    override val baseUrl = "https://wuxiaworld.site/"
    override val catalogUrl = "https://wuxiaworld.site/novel/?m_orderby=trending"
    override val iconUrl = "https://wuxiaworld.site/wp-content/uploads/2019/04/favicon-1.ico"
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
                ?.let { TextExtractor.get(it).trim() }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val url = bookUrl
                .toUrlBuilder()
                ?.addPath("ajax")
                ?.addPath("chapters")

            val request = postRequest(url.toString())
            networkClient.call(request)
                .toDocument()
                .select(".wp-manga-chapter > a[href]")
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
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("novel")
                .ifCase(page != 1) { addPath("page", page.toString()) }
                .add("m_orderby", "alphabet")

            val doc = networkClient.get(url).toDocument()
            doc.select(".page-item-detail")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it
                        .selectFirst("img[src]")
                        ?.attr("src") ?: ""

                    BookResult(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = doc.selectFirst("div.nav-previous.float-left") == null
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            if (page != 1)
                return@tryConnect PagedList.createEmpty(index = index)
            val url = baseUrl
                .toUrlBuilderSafe()
                .ifCase(page > 1) { addPath("page", page.toString()) }
                .add(
                    "s" to input,
                    "post_type" to "wp-manga",
                    "op" to "",
                    "author" to "",
                    "artist" to "",
                    "release" to "",
                    "adult" to ""
                )

            val doc = networkClient.get(url).toDocument()
            doc.select(".c-tabs-item__content")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")
                        ?.attr("src") ?: ""

                    BookResult(
                        title = link.attr("title"),
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
