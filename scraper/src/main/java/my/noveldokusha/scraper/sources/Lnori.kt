package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.addPath
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document
import java.net.URI

/**
 * Lnori — https://lnori.com
 *
 * A curated Japanese light novel library with a VOLUME-based structure (not
 * chapter-based). Each "series" has multiple "volumes" (books), and each volume
 * is a single readable unit containing multiple internal chapters (Prologue,
 * Chapter 1, Chapter 2, …, Epilogue).
 *
 * Site structure (verified 2026-06-30 by fetching live HTML):
 *
 *  Catalog (Browse):
 *    https://lnori.com/                       (home page — has a "Recently
 *                                              Updated" / "Popular" section)
 *    No dedicated catalog page found; the home page lists featured/recent
 *    novels. We use the home page as the catalog (single page).
 *
 *  Search:
 *    https://lnori.com/search?q=<query>
 *    Returns a list of matching series.
 *
 *  Series info page:
 *    https://lnori.com/series/<id>/<slug>
 *    - Title:       h1 (series title, e.g. "Overlord")
 *    - Cover:       meta[property="og:image"]  (e.g. https://cdn.lnori.com/series/2405.jpg)
 *                   or img[alt*="Cover"]       (e.g. https://cdn.lnori.com/cover/9177.webp)
 *    - Author:      .author
 *    - Genres:      a[href*="genre"]           (lowercase: action, adventure, …)
 *    - Description: .description
 *    - Volume list: a[href*="/book/"]           (each volume is a separate URL)
 *      e.g. /book/9177/overlord-vol-1-the-undead-king
 *
 *  Volume reader page (treated as a "chapter" by the app):
 *    https://lnori.com/book/<book-id>/<slug>
 *    - The page contains the ENTIRE volume as text, structured as multiple
 *      <section class="chapter"> elements inside <article class="content-body">.
 *    - Each section has an <h2 class="chapter-title"> (or .chapter-title-a)
 *      followed by <p> paragraphs.
 *    - Chapter title (for the app): the <h1> on the reader page (volume title,
 *      e.g. "Overlord, Vol. 1: The Undead King").
 *    - Chapter text: all text inside article.content-body, preserving the
 *      section/chapter headings as part of the flow.
 *    - No prev/next volume navigation links on the reader page itself; the
 *      volume list on the series info page is the only navigation.
 */
class Lnori(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "lnori"
    override val nameStrId = R.string.source_name_lnori
    override val baseUrl = "https://lnori.com/"
    override val catalogUrl = "https://lnori.com/"
    override val language = LanguageCode.ENGLISH
    override val iconUrl = "https://lnori.com/favicon.ico"

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            // The <h1> on the reader page is the volume title
            // (e.g. "Overlord, Vol. 1: The Undead King")
            doc.selectFirst("article.content-body h1")?.text()
                ?: doc.selectFirst("h1")?.text()
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            // The entire volume is inside <article class="content-body">.
            // It contains multiple <section class="chapter"> elements, each with
            // an <h2 class="chapter-title"> and <p> paragraphs.
            // We extract all text, preserving the chapter headings.
            val contentEl = doc.selectFirst("article.content-body")
                ?: doc.selectFirst("article.content")
                ?: throw NoSuchElementException("article.content-body not found on reader page")
            contentEl.select("script").remove()
            contentEl.select("style").remove()
            contentEl.select("nav").remove()
            contentEl.select(".share-buttons").remove()
            contentEl.select(".ads").remove()
            TextExtractor.get(contentEl)
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            // og:image gives the series cover (e.g. https://cdn.lnori.com/series/2405.jpg)
            doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst("img[alt*=Cover]")?.attr("src")
                ?: doc.selectFirst(".series-cover img[src]")?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            doc.selectFirst(".description")?.let {
                it.select("h2, h3, h4").remove()
                TextExtractor.get(it).trim()
            }
        }
    }

    /**
     * The "chapter list" for Lnori is actually a VOLUME list.
     * Each volume URL is treated as a chapter URL by the app.
     */
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            // Volume links: a[href*="/book/"] — e.g. /book/9177/overlord-vol-1-...
            // The link text is typically "Volume 1", "Volume 2", etc.
            // We also pick up the "Start Reading" link which points to vol 1.
            val volumes = doc.select("a[href*='/book/']")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val title = a.text().trim().ifBlank {
                        // Fallback: try to get a title from nearby elements
                        a.attr("title").ifBlank { return@mapNotNull null }
                    }
                    // Skip "Start Reading" duplicate — keep only volume titles
                    if (title.equals("Start Reading", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    ChapterResult(
                        title = title,
                        url = URI(baseUrl).resolve(href).toString()
                    )
                }
                .distinctBy { it.url }
                // Volume list is typically in order (Vol 1, Vol 2, ...) already
                .sortedBy { extractVolumeNumber(it.title) }

            // If no volume titles found, try the "Start Reading" link as a fallback
            if (volumes.isEmpty()) {
                val startLink = doc.selectFirst("a[href*='/book/']")?.attr("href")
                if (startLink != null) {
                    listOf(ChapterResult(
                        title = "Volume 1",
                        url = URI(baseUrl).resolve(startLink).toString()
                    ))
                } else {
                    emptyList()
                }
            } else {
                volumes
            }
        }
    }

    /**
     * Extract volume number from title like "Volume 1", "Vol. 2", etc.
     * for sorting purposes.
     */
    private fun extractVolumeNumber(title: String): Int {
        val match = Regex("""(?:Vol(?:ume)?\.?\s*)(\d+)""", RegexOption.IGNORE_CASE)
            .find(title)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index") {
            // Lnori's home page lists featured/recent series.
            // There's no pagination — we return the home page results on page 0
            // and an empty list on subsequent pages.
            if (index > 0) {
                return@tryConnect PagedList.createEmpty(index = index)
            }

            val doc = networkClient.get(catalogUrl).toDocument()
            // Series links: a[href*="/series/"]
            val books = doc.select("a[href*='/series/']")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val title = a.text().trim().ifBlank { return@mapNotNull null }
                    if (title.length < 2) return@mapNotNull null

                    val cover = a.selectFirst("img[src]")?.attr("src")?.let { c ->
                        if (c.startsWith("http")) c else URI(baseUrl).resolve(c).toString()
                    } ?: ""

                    BookResult(
                        title = title,
                        url = URI(baseUrl).resolve(href).toString(),
                        coverImageUrl = cover
                    )
                }
                .distinctBy { it.url }

            PagedList(
                list = books,
                index = index,
                isLastPage = true
            )
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0) {
                return@tryConnect PagedList.createEmpty(index = index)
            }

            val url = baseUrl.toUrlBuilderSafe().addPath("search").add("q", input)
            val doc = networkClient.get(url).toDocument()

            val books = doc.select("a[href*='/series/']")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val title = a.text().trim().ifBlank { return@mapNotNull null }
                    if (title.length < 2) return@mapNotNull null

                    val cover = a.selectFirst("img[src]")?.attr("src")?.let { c ->
                        if (c.startsWith("http")) c else URI(baseUrl).resolve(c).toString()
                    } ?: ""

                    BookResult(
                        title = title,
                        url = URI(baseUrl).resolve(href).toString(),
                        coverImageUrl = cover
                    )
                }
                .distinctBy { it.url }

            PagedList(
                list = books,
                index = index,
                isLastPage = true
            )
        }
    }
}
