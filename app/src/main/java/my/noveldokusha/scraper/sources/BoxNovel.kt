package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.*
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.*
import org.jsoup.nodes.Document

class BoxNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val name = "Box Novel"
    override val baseUrl = "https://boxnovel.com/"
    override val catalogUrl = "https://boxnovel.com/novel/?m_orderby=alphabet"
    override val iconUrl = "https://boxnovel.com/wp-content/uploads/2018/04/box-icon-150x150.png"
    override val language = "English"

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.summary_image img[data-src]")
            ?.attr("data-src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { TextExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = doc
            .selectFirst("meta[property=og:url] ")!!
            .attr("content")
            .toUrlBuilderSafe()
            .addPath("ajax")
            .addPath("chapters")
            .toString()

        return networkClient.call(postRequest(url))
            .toDocument()
            .select(".wp-manga-chapter > a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("novel")
                .ifCase(page != 1) { addPath("page", page.toString()) }
                .add("m_orderby", "alphabet")

            val doc = networkClient.get(url).toDocument()
            doc.select(".page-item-detail")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[data-src]")?.attr("data-src") ?: ""
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst("div.nav-previous.float-left") == null
                        )
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .ifCase(page != 1) { addPath("page", page.toString()) }
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
                    val bookCover = it.selectFirst("img[data-src]")?.attr("data-src") ?: ""
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst("div.nav-previous.float-left") == null
                        )
                    )
                }
        }
    }
}
