package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.Response
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.utils.toDocument
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
    override val name = "Korean Novels MTL"
    override val baseUrl = "https://www.koreanmtl.online/"
    override val catalogUrl = "https://www.koreanmtl.online/p/novels-listing.html?m=1"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null
    override suspend fun getChapterText(doc: Document): String? = null

    override suspend fun getBookCoverImageUrl(doc: Document): String = ""

    override suspend fun getBookDescription(doc: Document): String {
        return doc.select(".post-body.entry-content.float-container > p")
            .drop(1)
            .joinToString("\n\n") { it.text() }
            .trim()
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc.select(".post-body.entry-content.float-container li a[href]")
            .map {
                val url = it.attr("href")
                val title = it.text()
                ChapterMetadata(title = title, url = url)
            }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        if (page > 1)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            networkClient.get(catalogUrl)
                .toDocument()
                .select(".post-body.entry-content.float-container li a[href]")
                .map {
                    val url = it.attr("href")
                    val text = it.text()
                    BookMetadata(title = text, url = url)
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = true
                        )
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            networkClient.get(catalogUrl).toDocument()
                .select(".post-body.entry-content.float-container a[href]")
                .map {
                    val url = it.attr("href")
                    val text = it.text()
                    BookMetadata(title = text, url = url)
                }
                .filter { it.title.contains(input, ignoreCase = true) }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = true
                        )
                    )
                }
        }
    }
}