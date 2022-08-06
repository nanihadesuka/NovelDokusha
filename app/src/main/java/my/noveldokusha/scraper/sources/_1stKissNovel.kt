package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

// CHAPTERS LIST FAILS
class _1stKissNovel : SourceInterface.Catalog {
    override val catalogUrl = "https://1stkissnovel.love/novel/?m_orderby=alphabet"
    override val name = "1stKissNovel"
    override val baseUrl = "https://1stkissnovel.love/"
    override val language = "English"


    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img[data-src]")
            ?.attr("data-src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { textExtractor.get(it) }
    }

    // TODO() not working, website blocking calls
    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = "https://1stkissnovel.love/wp-admin/admin-ajax.php"
        val id = doc.selectFirst("input.rating-post-id")!!.attr("value")
        return postRequest(url)
            .postScope {
                add("action", "manga_get_chapters")
                add("manga", id)
            }
            .let { client.call(it) }
            .toDocument()
            .select(".wp-manga-chapter > a[href]")
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

            val doc = fetchDoc(url, 20000)
            doc.select(".page-item-detail")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[data-src]")?.attr("data-src")
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

            val doc = fetchDoc(url, 20000)
            doc.select(".row.c-tabs-item__content")
                .mapNotNull { it.selectFirst("a[href]") }
                .map {
                    val coverImageUrl = it.selectFirst("img[data-src]")?.attr("data-src")
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