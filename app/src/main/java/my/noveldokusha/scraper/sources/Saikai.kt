package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.StringReader
import java.net.URL

class Saikai : SourceInterface.catalog
{
    override val name = "Saikai"
    override val baseUrl = "https://saikaiscan.com.br/"
    override val catalogUrl = "https://saikaiscan.com.br/series"
    override val language = "Brazilian"

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
    {
        val url = "${doc.location()}?tab=capitulos"

        // no  cookies, header !! but works
        val chaptersDoc = Jsoup.parse(URL(url).openStream(), "utf-8", url)

        val firstChapterData = chaptersDoc
            .selectFirst("ul.__chapters")
            ?.selectFirst("li")!!

        val fullChapterUrl = firstChapterData.selectFirst("a")?.attr("href")!!
        val chapterTitle: String = firstChapterData.selectFirst(".__chapters--title")!!.text()
        val (bookpath, dispUrl, chapterUrl) = Regex("""^.*/(.+)/(\d+)/(.*)$""").find(fullChapterUrl)!!.destructured
        val firstChpater = ChapterMetadata(url = chapterUrl, title = chapterTitle)

        val initialVal = dispUrl.toInt()

        val preList = let {
            val script = chaptersDoc.select("script").find { it.data().startsWith("window.__NUXT__") }!!
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
                        it["chapter"].asNumber.runCatching { toInt() }.getOrNull() ?: return@mapNotNull null
                        ChapterMetadata(url = it["slug"].asString, title = it["title"].asString)
                    }
            }


        return (listOf(firstChpater) + preList).mapIndexed { index: Int, chapter: ChapterMetadata ->
            ChapterMetadata(
                title = chapter.title,
                url = "https://saikaiscan.com.br/ler/series/$bookpath/${initialVal + index}/${chapter.url}"
            )
        }
    }

    override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url =
            """https://api.saikai.com.br/api/stories?format=1&q=&status=null&genres=&country=null&sortProperty=title&sortDirection=asc&page=$page&per_page=24&relationships=language,type,format"""
        return tryConnect {
            val json = connect(url)
                .addHeaderRequest()
                .ignoreContentType(true)
                .getIO()
                .text()

            JsonParser
                .parseString(json)
                .asJsonObject["data"]
                .asJsonArray
                .map { it.asJsonObject }
                .map { BookMetadata(title = it["title"].asString, url = "https://saikaiscan.com.br/series/${it["slug"].asString}") }
                .let { Response.Success(it) }
        }
    }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
    {
        val page = index + 1
        val url = """https://api.saikai.com.br/api/stories?format=1&q=${input.urlEncode()}&status=null&genres=&country=null&sortProperty=title&sortDirection=asc&page=$page&per_page=24&relationships=language,type,format"""

        return tryConnect {
            val json = connect(url)
                .addHeaderRequest()
                .ignoreContentType(true)
                .getIO()
                .text()

            JsonParser
                .parseString(json)
                .asJsonObject["data"]
                .asJsonArray
                .map { it.asJsonObject }
                .map { BookMetadata(title = it["title"].asString, url = "https://saikaiscan.com.br/series/${it["slug"].asString}") }
                .let { Response.Success(it) }
        }
    }
}