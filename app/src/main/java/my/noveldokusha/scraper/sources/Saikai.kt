package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document
import java.io.StringReader

// ALL OK
class Saikai : SourceInterface.Catalog {
    override val name = "Saikai"
    override val baseUrl = "https://saikaiscan.com.br/"
    override val catalogUrl = "https://saikaiscan.com.br/series"
    override val language = "Brazilian"

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".story-header img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst("#synopsis-content")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val url = doc
            .selectFirst("meta[property=og:url]")!!
            .attr("content")
            .replace("//series", "/series")
            .toUrlBuilderSafe()
            .add("tab", "capitulos")
            .toString()

        // no  cookies, header !! but works
        val chaptersDoc = getRequest(url)
            .let { client.call(it) }
            .toDocument()

        val firstChapterData = chaptersDoc
            .selectFirst("ul.__chapters li")!!

        val fullChapterUrl = firstChapterData.selectFirst("a")?.attr("href")!!
        val chapterTitle: String = firstChapterData.selectFirst(".__chapters--title")!!.text()
        val (bookpath, dispUrl, chapterUrl) = Regex("""^.*/(.+)/(\d+)/(.*)$""").find(fullChapterUrl)!!.destructured
        val firstChapter = ChapterMetadata(url = chapterUrl, title = chapterTitle)

        val initialVal = dispUrl.toInt()

        val preList = let {
            val script =
                chaptersDoc.select("script").find { it.data().startsWith("window.__NUXT__") }!!
            val head = "{return "
            val start = "{layout:"
            start + script.data().split(head + start).last()
        }.let { input ->
            val reader = JsonReader(StringReader(input))
            reader.isLenient = true
            JsonParser.parseReader(reader)
        }.asJsonObject["data"]
            .asJsonArray[0]
            .asJsonObject["story"]
            .asJsonObject["data"]
            .asJsonObject["separators"]
            .asJsonArray.flatMap { volume ->
                volume.asJsonObject["releases"]
                    .asJsonArray.map { it.asJsonObject }
                    .mapNotNull {
                        it["chapter"].asNumber.runCatching { toInt() }.getOrNull()
                            ?: return@mapNotNull null
                        ChapterMetadata(url = it["slug"].asString, title = it["title"].asString)
                    }
            }

        return (listOf(firstChapter) + preList).mapIndexed { index: Int, chapter: ChapterMetadata ->
            ChapterMetadata(
                title = chapter.title,
                url = "https://saikaiscan.com.br/ler/series/$bookpath/${initialVal + index}/${chapter.url}"
            )
        }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        return getPageBooks(index = index)
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank()) {
            return Response.Success(PagedList.createEmpty(index = index))
        }
        return getPageBooks(index = index, input = input)
    }

    private suspend fun getPageBooks(
        index: Int,
        input: String = ""
    ): Response<PagedList<BookMetadata>> {
        val page = index + 1
        val url = """https://api.saikai.com.br/api/stories"""
            .toUrlBuilderSafe()
            .add(
                "format" to "1",
                "q" to input,
                "status" to "null",
                "genres" to "",
                "country" to "null",
                "sortProperty" to "title",
                "sortDirection" to "asc",
                "page" to "$page",
                "per_page" to "12",
                "relationships" to "language,type,format",
            )
            .toString()

        return tryConnect {
            val json = getRequest(url)
                .let { client.call(it) }
                .body!!
                .string()

            JsonParser
                .parseString(json)
                .asJsonObject["data"]
                .asJsonArray
                .map { it.asJsonObject }
                .map {
                    BookMetadata(
                        title = it["title"].asString,
                        url = "https://saikaiscan.com.br/series/${it["slug"].asString}",
                        coverImageUrl = "https://s3-alpha.saikai.com.br/${it["image"].asString}"
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = false
                        )
                    )
                }
        }
    }
}