package my.noveldokusha.scraper.sources

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import java.io.StringReader

class Saikai(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "seikai"
    override val nameStrId = R.string.source_name_saikai
    override val baseUrl = "https://saikaiscan.com.br/"
    override val catalogUrl = "https://saikaiscan.com.br/series"
    override val language = LanguageCode.PORTUGUESE

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".story-header img[src]")
                ?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("#synopsis-content")
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val url = bookUrl
                .toUrlBuilderSafe()
                .add("tab", "capitulos")
                .toString()

            val chaptersDoc = networkClient.get(url).toDocument()
            val firstChapterData = chaptersDoc
                .selectFirst("ul.__chapters li")!!

            val fullChapterUrl = firstChapterData.selectFirst("a")?.attr("href")!!
            val chapterTitle: String = firstChapterData.selectFirst(".__chapters--title")!!.text()
            val (bookpath, dispUrl, chapterUrl) = Regex("""^.*/(.+)/(\d+)/(.*)$""").find(
                fullChapterUrl
            )!!.destructured
            val firstChapter = ChapterResult(
                url = chapterUrl,
                title = chapterTitle
            )

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
                            ChapterResult(
                                url = it["slug"].asString,
                                title = it["title"].asString
                            )
                        }
                }

            (listOf(firstChapter) + preList).mapIndexed { index: Int, chapter: ChapterResult ->
                ChapterResult(
                    title = chapter.title,
                    url = "https://saikaiscan.com.br/ler/series/$bookpath/${initialVal + index}/${chapter.url}"
                )
            }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        getPageBooks(index = index)
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        if (input.isBlank()) {
            return@withContext Response.Success(PagedList.createEmpty(index = index))
        }
        getPageBooks(index = index, input = input)
    }

    private suspend fun getPageBooks(
        index: Int,
        input: String = ""
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
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

            val json = networkClient.get(url).body.string()

            JsonParser
                .parseString(json)
                .asJsonObject["data"]
                .asJsonArray
                .map { it.asJsonObject }
                .map {
                    BookResult(
                        title = it["title"].asString,
                        url = "https://saikaiscan.com.br/series/${it["slug"].asString}",
                        coverImageUrl = "https://s3-alpha.saikai.com.br/${it["image"].asString}"
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = false
                    )
                }
        }
    }
}