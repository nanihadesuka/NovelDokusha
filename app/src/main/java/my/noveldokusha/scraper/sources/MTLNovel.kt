package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MTLNovel : SourceInterface.Catalog {
    override val name = "MTLNovel"
    override val baseUrl = "https://www.mtlnovel.com/"
    override val catalogUrl = "https://www.mtlnovel.com/alltime-rank/"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst(".par.fontsize-16")!!.let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("amp-img.main-tmb[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String? {
        val text = doc.selectFirst(".desc") ?: return null
        val node = text.apply {
            select("h2").remove()
            select("p.descr").remove()
        }
        return textExtractor.get(node).trim()
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        // Needs to add "/" at the end
        val url = doc.location().toUrlBuilderSafe().addPath("chapter-list").toString() + "/"
        return fetchDoc(url)
            .select("a.ch-link[href]")
            .map {
                ChapterMetadata(
                    title = it.text(),
                    url = it.attr("href"),
                )
            }
            .reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        val url = catalogUrl.toUrlBuilderSafe().apply {
            if (page != 1) addPath("page", "$page")
        }

        return tryConnect {
            val doc = fetchDoc(url)
            doc.select(".box.wide")
                .mapNotNull {
                    val link = it.selectFirst("a.list-title[href]") ?: return@mapNotNull null
                    BookMetadata(
                        title = link.attr("aria-label"),
                        url = link.attr("href"),
                        coverImageUrl = it.selectFirst("amp-img[src]")?.attr("src") ?: ""
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = when (val nav = doc.selectFirst("div#pagination")) {
                                null -> true
                                else -> nav.children().last()?.`is`("span") ?: true
                            }
                        )
                    )
                }
        }
    }

    val extraHeaders = mapOf(
        ":authority:" to """www.mtlnovel.com""",
        ":method:" to """GET""",
        ":scheme:" to """https""",
        "accept:" to """application/json""",
        "accept-encoding:" to """gzip, deflate, br""",
        "accept-language:" to """en-GB,en-US;q=0.9,en;q=0.8,ca;q=0.7,es-ES;q=0.6,es;q=0.5,de;q=0.4""",
        "amp-same-origin:" to """true""",
        "cache-control:" to """no-cache""",
        "dnt:" to """1""",
        "pragma:" to """no-cache""",
        "referer:" to """https://www.mtlnovel.com/""",
        "sec-ch-ua:" to """" Not A;Brand";v="99", "Chromium";v="101", "Google Chrome";v="101"""",
        "sec-ch-ua-mobile:" to """?0""",
        "sec-ch-ua-platform:" to """Windows""",
        "sec-fetch-dest:" to """empty""",
        "sec-fetch-mode:" to """cors""",
        "sec-fetch-site:" to """same-origin""",
        "user-agent:" to """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36"""
    )

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        val url = """https://www.mtlnovel.com/wp-admin/admin-ajax.php""".toUrlBuilderSafe().apply {
            add("action", "autosuggest")
            add("q", input)
            add("__amp_source_origin", "https://www.mtlnovel.com")
        }.toString()

        return tryConnect {
            val json = connect(url)
                .addHeaderRequest()
                .headers(extraHeaders)
                .ignoreContentType(true)
                .getIO()
                .text()

            JsonParser
                .parseString(json)
                .asJsonObject["items"]
                .asJsonArray[0]
                .asJsonObject["results"]
                .asJsonArray
                .map { it.asJsonObject }
                .map {
                    BookMetadata(
                        title = Jsoup.parse(it["title"].asString).text(),
                        url = it["permalink"].asString,
                        coverImageUrl = it["thumbnail"].asString
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
