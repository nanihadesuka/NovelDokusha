package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class MoreNovel : SourceInterface.catalog
{
    override val name = "More Novel"
    override val baseUrl = "https://morenovel.net/"
    override val catalogUrl = "https://morenovel.net/novel/"
    override val language = "Indonesian"
    
    override suspend fun getChapterText(doc: Document): String
    {
        val chapter = doc.selectFirst("div.text-left")
        return if (chapter != null) {
            //chapter.selectFirst("p > b a[href]")?.remove()
            textExtractor.get(chapter)
        } else {
            ""
        }
            
    }
    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        val cover = doc.selectFirst("div.summary_image")
            ?.selectFirst("img[data-src]")
        return if (cover != null) cover.attr("data-src") else ""
    }

    override suspend fun getBookDescripton(doc: Document): String?
    {
        return doc.selectFirst(".summary__content.show-more")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        val url = doc
            .location()
            .toUrlBuilder()
            ?.addPath("ajax")
            ?.addPath("chapters")

        return connect(url.toString())
            .addHeaderRequest()
            .timeout(20000)
            .postIO()
            .select(".wp-manga-chapter > a[href]")
            .mapNotNull { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        return tryConnect {
            val url = catalogUrl
                .toUrlBuilderSafe()
                //.addPath("novel")
                .ifCase(page != 1){ addPath("page",page.toString()) }
                // .add("m_orderby","alphabet")

            val catalog = connect(url.toString())
                .timeout(20 * 1000)
                .executeIO()

            if (catalog.statusCode() == 200) {
                catalog
                    .parse()
                    .select(".page-item-detail")
                    .mapNotNull {
                        val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                        val bookCover = it.selectFirst("img[data-src]")?.attr("data-src") ?: ""
                        BookMetadata(
                            title = link.attr("title"),
                            url = link.attr("href"),
                            coverImageUrl = bookCover
                        )
                    }
                    .let { Response.Success(it) }
            } else {
                Response.Success(listOf())
            }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
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

            val catalog = connect(url.toString())
                .timeout(20 * 1000)
                .executeIO()
            
            if (catalog.statusCode() == 200) {
                catalog
                    .parse()
                    .select(".c-tabs-item__content")
                    .mapNotNull {
                        val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                        val bookCover = it.selectFirst("img[data-src]")?.attr("data-src") ?: ""
                        BookMetadata(
                            title = link.attr("title"),
                            url = link.attr("href"),
                            coverImageUrl = bookCover
                        )
                    }
                    .let { Response.Success(it) }
            } else {
                Response.Success(listOf())
            }
        }
    }
}
