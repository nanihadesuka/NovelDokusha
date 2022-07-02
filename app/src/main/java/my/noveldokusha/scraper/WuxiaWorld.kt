package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class WuxiaWorld : SourceInterface.catalog
{
    override val name = "WuxiaWorld"
    override val baseUrl = "https://wuxiaworld.site/"
    override val catalogUrl = "https://wuxiaworld.site/novel/?m_orderby=views"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

//    override suspend fun getChapterText(doc: Document): String
//    {
//        return doc.selectFirst(".reading-content p")!!.let { textExtractor.get(it) }
//    }

    override suspend fun getBookCoverImageUrl(doc: Document): String?
    {
        return doc.selectFirst("div.summary_image")
            ?.selectFirst("img")
            ?.attr("data-src")
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
            .postIO()
            .select(".wp-manga-chapter > a[href]")
            .map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("novel")
                .ifCase(page != 1){ addPath("page",page.toString()) }
                .add("m_orderby","views")

            fetchDoc(url)
                .select(".page-item-detail")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img")?.attr("data-src") ?: return@mapNotNull null
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { Response.Success(it) }
        }
    }

    val extraHeaders = mapOf(
        ":authority:" to """wuxiaworld.site""",
        ":method:" to """GET""",
        ":scheme:" to """https""",
        "accept:" to """application/json""",
        "accept-encoding:" to """gzip, deflate, br""",
        "accept-language:" to """en-GB,en-US;q=0.9,en;q=0.8,ca;q=0.7,es-ES;q=0.6,es;q=0.5,de;q=0.4""",
        "amp-same-origin:" to """true""",
        "cache-control:" to """no-cache""",
        "dnt:" to """1""",
        "pragma:" to """no-cache""",
        "referer:" to """https://wuxiaworld.site/""",
        "sec-ch-ua:" to """" Not A;Brand";v="99", "Chromium";v="101", "Google Chrome";v="101"""",
        "sec-ch-ua-mobile:" to """?0""",
        "sec-ch-ua-platform:" to """Windows""",
        "sec-fetch-dest:" to """empty""",
        "sec-fetch-mode:" to """cors""",
        "sec-fetch-site:" to """same-origin""",
        "user-agent:" to """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36"""
    )

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

            fetchDoc(url)
                .select(".c-tabs-item__content")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img")?.attr("data-src") ?: ""
                    BookMetadata(
                        title = link.attr("title"),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { Response.Success(it) }
        }
    }
}
