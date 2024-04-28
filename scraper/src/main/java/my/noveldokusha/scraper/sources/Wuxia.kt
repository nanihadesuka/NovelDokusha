package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.postRequest
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

class Wuxia(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "wuxia"
    override val nameStrId = R.string.source_name_wuxia
    override val baseUrl = "https://www.wuxia.blog/"
    override val catalogUrl = "https://www.wuxia.blog/listNovels"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(
        doc: Document
    ): String = withContext(Dispatchers.Default) {
        doc.selectFirst("div.panel-body.article")!!.also {
            it.select(".pager").remove()
            it.select(".fa.fa-calendar").remove()
            it.select("button.btn.btn-default").remove()
            it.select("div.recently-nav.pull-right").remove()
        }.let { TextExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".imageCover")
                ?.selectFirst("img[src]")
                ?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("div[itemprop=description]")
                ?.let {
                    it.select("h4").remove()
                    TextExtractor.get(it)
                }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val id = doc.selectFirst("#more")!!.attr("data-nid")
            val newChapters = doc.select("#chapters a[href]")
            val res = tryFlatConnect {
                val request =
                    postRequest("https://wuxia.blog/temphtml/_tempChapterList_all_$id.html")
                networkClient.call(request)
                    .toDocument()
                    .select("a[href]")
                    .let { Response.Success(it) }
            }

            val oldChapters = if (res is Response.Success) res.data else listOf()

            (newChapters + oldChapters)
                .reversed()
                .map {
                    ChapterResult(
                        title = it.text(),
                        url = it.attr("href")
                    )
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        if (index > 0)
            return@withContext Response.Success(PagedList.createEmpty(index = index))

        tryConnect {
            networkClient.get(catalogUrl)
                .toDocument()
                .select("td.novel a[href]")
                .map {
                    BookResult(
                        title = it.text(),
                        url = baseUrl + it.attr("href")
                    )
                }
                .let { PagedList(list = it, index = index, isLastPage = true) }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            val url = baseUrl.toUrlBuilderSafe().apply {
                add("search", input)
            }
            networkClient.get(url)
                .toDocument()
                .selectFirst("#table")!!
                .children()
                .first()!!
                .children()
                .mapNotNull {
                    val link = it.selectFirst(".xxxx > a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookResult(
                        title = link.text(),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { PagedList(list = it, index = index, isLastPage = true) }
        }
    }
}
