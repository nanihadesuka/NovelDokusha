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
 * WuxiaBox — https://www.wuxiabox.com
 *
 * NOTE: As of 2026-06-30, the live site is behind a Cloudflare "managed
 * challenge" that blocks non-browser clients. The scraper is implemented based
 * on archived HTML (Wayback Machine snapshot 2026-05-13). It may work from
 * the Android app if Cloudflare does not challenge mobile app requests, or if
 * the site's Cloudflare configuration is relaxed in the future.
 *
 * Site structure (verified from archived HTML, 2026-05-13):
 *
 *  Catalog (Browse):
 *    https://www.wuxiabox.com/category/all.html         (all novels)
 *    https://www.wuxiabox.com/category/all/<page>.html  (paginated)
 *    The home page also lists popular/recent novels.
 *
 *  Search:
 *    https://www.wuxiabox.com/search.html?searchkey=<query>
 *
 *  Book info page:
 *    https://www.wuxiabox.com/novel/<slug>.html
 *    - Title:       h1
 *    - Cover:       img[src*="/d/file/cover"]  (near the h1, inside .novel-header)
 *    - Author:      .author (text like "Author: Coming for Koi" — strip "Author:" prefix)
 *    - Genres:      .categories a.property-item  (or a[href*="genre"])
 *    - Description: div.summary div.content (contains <p> tags with synopsis)
 *    - Chapter list: a[href*="_"] matching pattern <slug>_<N>.html
 *      (e.g. /novel/a-hundredfold-training-system-instantly-upgrades-999_1.html)
 *      Chapters are listed directly on the info page.
 *
 *  Chapter reader:
 *    https://www.wuxiabox.com/novel/<slug>_<N>.html
 *    - Chapter title: h1 or .chapter-title
 *    - Chapter text:  #content or .chapter-content or #chapter-content
 *    (Reader page structure inferred from site pattern; exact selectors may
 *     need adjustment based on live testing.)
 */
class WuxiaBox(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "wuxia_box"
    override val nameStrId = R.string.source_name_wuxia_box
    override val baseUrl = "https://www.wuxiabox.com/"
    override val catalogUrl = "https://www.wuxiabox.com/category/all.html"
    override val language = LanguageCode.ENGLISH
    override val iconUrl = "https://www.wuxiabox.com/favicon.ico"

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".chapter-title")?.text()
                ?: doc.selectFirst("h1")?.text()
                ?: doc.selectFirst("title")?.text()
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            // Try common WuxiaBox content selectors
            val contentEl = doc.selectFirst("#content")
                ?: doc.selectFirst(".chapter-content")
                ?: doc.selectFirst("#chapter-content")
                ?: doc.selectFirst("#chaptercontent")
                ?: doc.selectFirst(".read-content")
                ?: doc.selectFirst(".reading-content")
                ?: doc.selectFirst("article.content")
                ?: throw NoSuchElementException("Chapter content not found on reader page")
            contentEl.select("script").remove()
            contentEl.select("style").remove()
            contentEl.select("iframe").remove()
            contentEl.select(".ads").remove()
            contentEl.select(".ad").remove()
            contentEl.select(".pager").remove()
            contentEl.select(".chapter-nav").remove()
            TextExtractor.get(contentEl)
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            // Cover image is in src containing /d/file/cover
            doc.selectFirst("img[src*='/d/file/cover']")?.attr("src")
                ?: doc.selectFirst(".novel-header .cover img[src]")?.attr("src")
                ?: doc.selectFirst("figure.cover img[src]")?.attr("src")
                ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val desc = doc.selectFirst("div.summary div.content")
                ?: doc.selectFirst("div.summary")
                ?: doc.selectFirst("p.description")
                ?: return@tryConnect null
            desc.select("h4").remove()
            TextExtractor.get(desc).trim()
        }
    }

    /**
     * Chapter list is on the info page itself.
     * Chapter URLs follow the pattern: /novel/<slug>_<N>.html
     */
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            // Extract the book slug from the URL (e.g. "a-hundredfold-...-999")
            val bookSlug = bookUrl.trimEnd('/').substringAfterLast('/')
                .removeSuffix(".html")

            // Build a regex that matches chapter URLs: <slug>_<N>.html
            // We match against both the raw href and the resolved URL to handle
            // relative and absolute chapter links correctly.
            val chapterRegex = Regex("""${Regex.escape(bookSlug)}_(\d+)\.html$""")

            // Find all chapter links whose href matches the chapter pattern
            doc.select("a[href]")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val resolvedUrl = URI(baseUrl).resolve(href).toString()

                    // Must match the chapter URL pattern for THIS book's slug
                    val match = chapterRegex.find(href)
                        ?: chapterRegex.find(resolvedUrl)
                        ?: return@mapNotNull null

                    val chapterNumber = match.groupValues[1]
                    val title = a.text().trim().ifBlank { "Chapter $chapterNumber" }
                        .replace("\n", " ").replace(Regex("\\s+"), " ").trim()

                    // Skip pure navigation labels that have no real title
                    if (title.length < 2) return@mapNotNull null

                    ChapterResult(
                        title = title,
                        url = resolvedUrl
                    )
                }
                .distinctBy { it.url }
                // Sort by chapter number ascending (oldest first)
                .sortedBy { chapter ->
                    chapterRegex.find(chapter.url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index") {
            val page = index + 1
            val url = if (page == 1) {
                catalogUrl.toUrlBuilderSafe()
            } else {
                baseUrl.toUrlBuilderSafe()
                    .addPath("category", "all", "$page.html")
            }

            val doc = networkClient.get(url).toDocument()
            parseBooks(doc, index)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank()) {
                return@tryConnect PagedList.createEmpty(index = index)
            }

            val page = index + 1
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("search.html")
                .add("searchkey", input)
                .apply { if (page > 1) add("page", page.toString()) }

            val doc = networkClient.get(url).toDocument()
            parseBooks(doc, index)
        }
    }

    private fun parseBooks(doc: Document, index: Int): PagedList<BookResult> {
        // WuxiaBox catalog/search pages list novels as:
        // <a href="/novel/<slug>.html" title="..."> with cover img
        // Skip chapter links (those have _<N>.html in the URL)
        val books = doc.select("a[href*='/novel/']")
            .mapNotNull { a ->
                val href = a.attr("href").ifBlank { return@mapNotNull null }
                // Skip chapter links (they contain _<N>.html)
                if (Regex("""_\d+\.html$""").containsMatchIn(href)) {
                    return@mapNotNull null
                }
                val title = a.attr("title").ifBlank {
                    a.selectFirst("h2, h3, h4, .title, .novel-title")?.text()
                } ?: a.text().trim().ifBlank { return@mapNotNull null }
                if (title.length < 2) return@mapNotNull null

                val cover = a.selectFirst("img[data-src]")?.attr("data-src")
                    ?: a.selectFirst("img[src]")?.attr("src")
                    ?: ""
                val coverUrl = cover.let { c ->
                    when {
                        c.isBlank() || c.startsWith("data:") -> ""
                        c.startsWith("http") -> c
                        c.startsWith("/") -> URI(baseUrl).resolve(c).toString()
                        else -> ""
                    }
                }

                BookResult(
                    title = title.trim(),
                    url = URI(baseUrl).resolve(href).toString(),
                    coverImageUrl = coverUrl
                )
            }
            .distinctBy { it.url }

        val isLast = (doc.selectFirst("a.next[href]") == null) &&
                     (doc.selectFirst(".pagination a[rel=next]") == null)

        return PagedList(
            list = books,
            index = index,
            isLastPage = isLast || books.isEmpty()
        )
    }
}
