package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.*
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.*
import org.jsoup.nodes.Document

class _1stKissNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val catalogUrl = "https://1stkissnovel.love/novel/?m_orderby=alphabet"
    override val name = "1stKissNovel"
    override val baseUrl = "https://1stkissnovel.love/"
    override val iconUrl = "https://1stkissnovel.love/wp-content/uploads/2020/10/cropped-HINH-NEN-3-1-32x32.png"
    override val language = "English"


    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { TextExtractor.get(it) }
    }

    // TODO() not working, website blocking calls
    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = doc
            .selectFirst("meta[property=og:url]")!!
            .attr("content")
            .toUrlBuilderSafe()
            .addPath("ajax", "chapters")
            .toString()

        val request = postRequest(url)

        return networkClient
            .call(request)
            .toDocument()
            .select(".wp-manga-chapter a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {
            val url = when (page) {
                1 -> catalogUrl
                else -> baseUrl
                    .toUrlBuilderSafe()
                    .addPath("novel", "page", page.toString())
                    .add("m_orderby", "alphabet")
                    .toString()
            }

            val doc = networkClient.get(url).toDocument()
            doc.select(".page-item-detail")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[src]")?.attr("src")
                    BookMetadata(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = isLastPage(doc)
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
                .add("s", input)
                .add("post_type", "wp-manga")

            val doc = networkClient.get(url).toDocument()
            doc.select(".row.c-tabs-item__content")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[src]")?.attr("src")
                    BookMetadata(
                        title = it.attr("title"),
                        url = it.attr("href"),
                        coverImageUrl = coverImageUrl ?: "",
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = isLastPage(doc)
                        )
                    )
                }
        }
    }

    private fun isLastPage(doc: Document) = when (val nav = doc.selectFirst("div.wp-pagenavi")) {
        null -> true
        else -> nav.children().last()?.`is`(".current") ?: true
    }
}