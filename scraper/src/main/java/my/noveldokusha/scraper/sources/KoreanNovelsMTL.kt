package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.koreanmtl.online/p/is-this-hero-for-real.html
 * Chapter url example:
 * https://www.koreanmtl.online/2020/05/running-away-from-hero-chapter-17.html
 */
class KoreanNovelsMTL(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "korean_novels_mtl"
    override val nameStrId = R.string.source_name_korean_novels_mtl
    override val baseUrl = "https://www.koreanmtl.online/"
    override val catalogUrl = "https://www.koreanmtl.online/p/novels-listing.html?m=1"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? = null
    override suspend fun getChapterText(doc: Document): String? = null

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        Response.Success("")

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .select(".post-body.entry-content.float-container > p")
                .drop(1)
                .joinToString("\n\n") { it.text() }
                .trim()
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .select(".post-body.entry-content.float-container li a[href]")
                .map {
                    val url = it.attr("href")
                    val title = it.text()
                    ChapterResult(title = title, url = url)
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            if (page > 1)
                return@tryConnect PagedList.createEmpty(index = index)
            networkClient.get(catalogUrl)
                .toDocument()
                .select(".post-body.entry-content.float-container li a[href]")
                .map {
                    val url = it.attr("href")
                    val text = it.text()
                    BookResult(title = text, url = url)
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

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            networkClient.get(catalogUrl).toDocument()
                .select(".post-body.entry-content.float-container a[href]")
                .map {
                    val url = it.attr("href")
                    val text = it.text()
                    BookResult(title = text, url = url)
                }
                .filter { it.title.contains(input, ignoreCase = true) }
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