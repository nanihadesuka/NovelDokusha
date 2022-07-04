package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class WebNovelLight : SourceInterface.Catalog {
    override val name = "Web Novel Light"
    override val baseUrl = "https://webnovellight.com/"
    override val catalogUrl = "https://webnovellight.com/series/?m_orderby=alphabet"
    override val language = "English"


    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String? {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = doc
            .location()
            .toUrlBuilder()
            ?.addPath("ajax")
            ?.addPath("chapters")

        return connect(url.toString())
            .addHeaderRequest()
            .postIO()
            .select(".wp-manga-chapter > a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        if (page > 1)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("novel")
                .ifCase(page != 1) { addPath("page", page.toString()) }
                .add("m_orderby", "alphabet")

            fetchDoc(url)
                .select(".page-item-detail")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { Response.Success(PagedList(list = it, index = index, isLastPage = false)) }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        val page = index + 1
        if (page > 1)
            return Response.Success(PagedList.createEmpty(index = index))

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

            fetchDoc(url)
                .select(".c-tabs-item__content")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { Response.Success(PagedList(list = it, index = index, isLastPage = false)) }
        }
    }
}
