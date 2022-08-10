package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.*
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.*
import org.jsoup.nodes.Document

class WuxiaWorld(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {

    override val name = "Wuxia World"
    override val baseUrl = "https://wuxiaworld.site/"
    override val catalogUrl = "https://wuxiaworld.site/novel/?m_orderby=trending"
    override val iconUrl = "https://wuxiaworld.site/wp-content/uploads/2019/04/favicon-1.ico"
    override val language = "English"

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { TextExtractor.get(it).trim() }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = doc
            .selectFirst("meta[property=og:url]")!!
            .attr("content")
            .toUrlBuilder()
            ?.addPath("ajax")
            ?.addPath("chapters")

        val request = postRequest(url.toString())
        return networkClient.call(request)
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
                    val bookCover = it
                        .selectFirst("img[src]")
                        ?.attr("src") ?: ""

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
        if (page != 1)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
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
                            isLastPage = true
                        )
                    )
                }
        }
    }
}
