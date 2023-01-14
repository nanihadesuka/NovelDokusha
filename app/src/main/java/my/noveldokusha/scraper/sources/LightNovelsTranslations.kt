package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.add
import my.noveldokusha.utils.capitalize
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilder
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.*

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
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? =
        doc.selectFirst("h1.entry-title")?.text()

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst(".page, .type-page, .status-publish, .hentry")!!
            .selectFirst(".entry-content").run {
                this!!.select("#textbox").remove()
                TextExtractor.get(this)
            }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst("div.entry-content")
            ?.selectFirst("img[src]")
            ?.attr("src")
            ?.replace("&#038;", "")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst("div.entry-content")
            ?.select("p")
            ?.find { it.text().startsWith("Synopsis", ignoreCase = true) }
            ?.nextElementSiblings()?.asSequence()
            ?.takeWhile { it.`is`("p") }
            ?.joinToString("\n\n") { it.text() }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc
            .select(".su-spoiler-content.su-u-clearfix.su-u-trim a[href]")
            .map {
                val url = it.attr("href")
                val decoded_url = URLDecoder.decode(url, "UTF-8")
                val title: String = Regex(""".+/(.+)/?$""").find(decoded_url)?.destructured?.run {
                    this.component1().replace("-", " ").removeSuffix("/")
                } ?: it.text()

                ChapterMetadata(title = title.trim().capitalize(Locale.ROOT), url = url)
            }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        if (page > 1)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
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

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        val url = baseUrl.toUrlBuilder()!!.apply {
            add("order", "DESC")
            add("orderby", "relevance")
            add("s", input)
        }

        return tryConnect {
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