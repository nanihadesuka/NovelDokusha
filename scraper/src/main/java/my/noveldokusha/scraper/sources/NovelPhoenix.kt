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
 * NovelPhoenix — https://novelphoenix.com
 *
 * Site structure (verified 2026-06-30 by fetching live HTML):
 *
 *  Catalog (Browse):
 *    https://novelphoenix.com/genre-all/sort-new/status-all/all-novel           (page 1)
 *    https://novelphoenix.com/genre-all/sort-new/status-all/all-novel?page=2     (page 2+)
 *    24 books per page, ~741 total pages. Same template as NovelFire.
 *
 *  Search:
 *    https://novelphoenix.com/search?keyword=<query>&page=<N>
 *    IMPORTANT: the `?q=` parameter is IGNORED by the site (returns all ~17,762
 *    novels unfiltered). Only `?keyword=` actually filters by title.
 *    Use ul.novel-list.chapters for actual search results (NOT .col6 sidebar).
 *
 *  Book info page:
 *    https://novelphoenix.com/novel/<slug>
 *    - Title:       h1.novel-title
 *    - Cover:       meta[property="og:image"]
 *    - Author:      .author a (or [itemprop="author"])
 *    - Genres:      div.categories ul li a.property-item
 *    - Description: div.summary div.content
 *
 *  Chapter list (separate PAGINATED page — unlike NovelFire which is single-page):
 *    https://novelphoenix.com/novel/<slug>/chapters         (page 1, 100 chapters)
 *    https://novelphoenix.com/novel/<slug>/chapters?page=2  (page 2+)
 *    - <ul class="chapter-list"> with up to 100 <li> per page.
 *    - Each <li> contains an <a href title>. Use the `title` attribute for the
 *      clean chapter title (link text has rank number + timestamp appended).
 *    - Order is already oldest-first (chapter-1 → chapter-N); no reversal needed.
 *    - Detect last page via ul.pagination last li.disabled.
 *
 *  Chapter reader:
 *    https://novelphoenix.com/novel/<slug>/chapter-<N>
 *    - Chapter title: .chapter-title (inside #chapter-article or h1)
 *    - Chapter text:  #content (only <p> children)
 *    - Next chapter:  a.nextchap[rel="next"] (check href != "javascript:;")
 *
 *  Cover images are lazy-loaded on catalog pages: <img class="lazy" data-src="...">.
 *  On info pages, cover is directly in src attribute.
 */
class NovelPhoenix(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "novel_phoenix"
    override val nameStrId = R.string.source_name_novel_phoenix
    override val baseUrl = "https://novelphoenix.com/"
    override val catalogUrl = "https://novelphoenix.com/genre-all/sort-new/status-all/all-novel"
    override val language = LanguageCode.ENGLISH
    override val iconUrl = "https://novelphoenix.com/logo.ico"

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.selectFirst("#chapter-article .chapter-title")?.text()
                ?: doc.selectFirst(".chapter-title")?.text()
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            val contentEl = doc.selectFirst("#content")
                ?: doc.selectFirst("#chapter-container .d-chapter-content")
                ?: throw NoSuchElementException("#content not found on chapter page")
            contentEl.select("script").remove()
            contentEl.select("style").remove()
            contentEl.select("iframe").remove()
            contentEl.select("ins").remove()
            contentEl.select(".ads").remove()
            TextExtractor.get(contentEl)
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst(".novel-header .cover img[src]")?.attr("src")
                ?: doc.selectFirst("figure.cover img[src]")?.attr("src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val summary = doc.selectFirst("div.summary div.content")
                ?: doc.selectFirst("div.summary")
                ?: return@tryConnect null
            summary.select("h4").remove()
            summary.select(".expand").remove()
            summary.select("button").remove()
            TextExtractor.get(summary).trim()
        }
    }

    /**
     * Chapter list is PAGINATED on NovelPhoenix (100 chapters per page),
     * unlike NovelFire which is a single page. We must fetch all pages.
     */
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val chaptersUrl = bookUrl.trimEnd('/') + "/chapters"
            val allChapters = mutableListOf<ChapterResult>()
            var page = 1
            var url = chaptersUrl.toUrlBuilderSafe()

            while (true) {
                val doc = networkClient.get(url).toDocument()
                val pageChapters = doc.select("ul.chapter-list a[href]")
                    .mapNotNull { a ->
                        val href = a.attr("href").ifBlank { return@mapNotNull null }
                        val title = a.attr("title").ifBlank { a.text() }
                        ChapterResult(
                            title = title.trim(),
                            url = URI(baseUrl).resolve(href).toString()
                        )
                    }
                    .distinctBy { it.url }

                allChapters.addAll(pageChapters)

                // Check if this is the last page
                if (isLastPage(doc) || pageChapters.isEmpty()) {
                    break
                }

                // Safety limit: 100 pages = 10,000 chapters max
                if (page >= 100) {
                    break
                }

                page++
                url = chaptersUrl.toUrlBuilderSafe().apply { add("page", page.toString()) }
            }

            allChapters
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index") {
            val page = index + 1
            val url = catalogUrl
                .toUrlBuilderSafe()
                .apply { if (page > 1) add("page", page.toString()) }

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
                .addPath("search")
                .add("keyword", input)
                .apply { if (page > 1) add("page", page.toString()) }

            val doc = networkClient.get(url).toDocument()
            parseSearchBooks(doc, index)
        }
    }

    private fun parseBooks(doc: Document, index: Int): PagedList<BookResult> {
        val books = doc.select("li.novel-item")
            .mapNotNull { parseNovelItem(it) }
            .distinctBy { it.url }

        return PagedList(
            list = books,
            index = index,
            isLastPage = isLastPage(doc) || books.isEmpty()
        )
    }

    private fun parseSearchBooks(doc: Document, index: Int): PagedList<BookResult> {
        val books = doc.select("ul.novel-list.chapters li.novel-item")
            .mapNotNull { parseNovelItem(it) }
            .distinctBy { it.url }

        return PagedList(
            list = books,
            index = index,
            isLastPage = isLastPage(doc) || books.isEmpty()
        )
    }

    private fun parseNovelItem(li: org.jsoup.nodes.Element): BookResult? {
        val link: org.jsoup.nodes.Element = li.selectFirst("a[href][title]")
            ?: li.selectFirst("a[href*='/novel/']")
            ?: return null

        val href = link.attr("href").ifBlank { return null }
        val title = link.attr("title").ifBlank {
            link.selectFirst(".novel-title, h4.novel-title, .title")?.text()
                ?: link.text().substringBefore(" Rank ").trim()
        }

        val cover = (li.selectFirst("img.lazy[data-src]")?.attr("data-src")
            ?: li.selectFirst("img[data-src]")?.attr("data-src")
            ?: li.selectFirst("img[src]")?.attr("src")
            ?: "").let { c ->
            if (c.isBlank() || c.startsWith("data:")) ""
            else if (c.startsWith("http")) c
            else URI(baseUrl).resolve(c).toString()
        }

        return BookResult(
            title = title.trim(),
            url = URI(baseUrl).resolve(href).toString(),
            coverImageUrl = cover
        )
    }

    private fun isLastPage(doc: Document): Boolean {
        val nav = doc.selectFirst("ul.pagination") ?: return true
        val lastLi = nav.children().last() ?: return true
        return lastLi.`is`(".disabled")
    }
}
