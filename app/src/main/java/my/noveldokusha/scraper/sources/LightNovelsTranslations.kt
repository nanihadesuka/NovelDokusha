package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.add
import my.noveldokusha.utils.capitalize
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilder
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.Locale

/**
 * Novel main page (chapter list) example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/
 * Chapter url example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/the-sage-summoned-to-another-world-volume-1-chapter-1/
 */
class LightNovelsTranslations(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "light_novel_translations"
    override val name = "Light Novel Translations"
    override val baseUrl = "https://lightnovelstranslations.com/"
    override val catalogUrl = "https://lightnovelstranslations.com/"
    override val iconUrl =
        "https://c10.patreonusercontent.com/4/patreon-media/p/campaign/458169/797a2e9b03094435947635c4da0fc683/eyJ3IjoyMDB9/1.jpeg?token-time=2145916800&token-hash=2gkkI3EgQqRPh5dQe9uxrULjURfQVm60BHKUdh91MtE%3D"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(
        doc: Document
    ): String? = withContext(Dispatchers.Default) {
        doc.selectFirst("h1.entry-title")?.text()
    }

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst(".page, .type-page, .status-publish, .hentry")!!
            .selectFirst(".entry-content").run {
                this!!.select("#textbox").remove()
                TextExtractor.get(this)
            }
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument().selectFirst("div.entry-content")
                ?.selectFirst("img[src]")
                ?.attr("src")
                ?.replace("&#038;", "")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("div.entry-content")
                ?.select("p")
                ?.find { it.text().startsWith("Synopsis", ignoreCase = true) }
                ?.nextElementSiblings()?.asSequence()
                ?.takeWhile { it.`is`("p") }
                ?.joinToString("\n\n") { it.text() }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .select(".su-spoiler-content.su-u-clearfix.su-u-trim a[href]")
                .map {
                    val url = it.attr("href")
                    val decoded_url = URLDecoder.decode(url, "UTF-8")
                    val title: String =
                        Regex(""".+/(.+)/?$""").find(decoded_url)?.destructured?.run {
                            this.component1().replace("-", " ").removeSuffix("/")
                        } ?: it.text()

                    ChapterMetadata(title = title.trim().capitalize(Locale.ROOT), url = url)
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            if (page > 1)
                return@tryConnect PagedList.createEmpty(index = index)

            networkClient.get(catalogUrl).toDocument()
                .selectFirst("#prime_nav")!!
                .children()
                .subList(1, 4)
                .flatMap { it.select("a[href]") }
                .filter {
                    val url = it.attr("href")
                    val text = it.text()
                    return@filter url != "#" &&
                            !url.endsWith("-illustrations/") &&
                            !url.endsWith("-illustration/") &&
                            !url.endsWith("-illustration-page/") &&
                            text != "Novel Illustrations" &&
                            text != "Novels Illustrations"
                }
                .map { BookMetadata(title = it.text(), url = it.attr("href")) }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = true
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            val url = baseUrl.toUrlBuilder()!!.apply {
                add("order", "DESC")
                add("orderby", "relevance")
                add("s", input)
            }

            networkClient.get(url)
                .toDocument()
                .selectFirst(".jetpack-search-filters-widget__filter-list")!!
                .select("a[href]")
                .map {
                    val (name) = Regex("""^.*category_name=(.*)$""").find(it.attr("href"))!!.destructured
                    BookMetadata(
                        title = it.text(),
                        url = "https://lightnovelstranslations.com/${name}/"
                    )
                }.let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = true
                    )
                }
        }
    }
}