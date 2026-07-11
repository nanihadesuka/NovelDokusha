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
 * Direct Chinese source for timotxt.com.
 *
 * Fetching goes directly to `https://www.timotxt.com/` — the Cloudflare
 * bypass interceptors handle any CF challenges automatically (slow on the
 * first request, fast after the `cf_clearance` cookie is cached). Content
 * is shown as raw Chinese text — this source does NOT translate.
 *
 * WebView: `transformWebviewUrl()` converts timotxt.com URLs to the
 * translate.goog proxy with `_x_tr_*` params so the browser shows an
 * English-translated page (the proxy's JS handles translation).
 */
class TimoTxt(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "timotxt"
    override val nameStrId = R.string.source_name_timotxt
    override val baseUrl = "https://www.timotxt.com/"
    override val catalogUrl = "https://www.timotxt.com/bookstack/"
    override val language = LanguageCode.CHINESE

    companion object {
        private const val TRANSLATE_PARAMS =
            "_x_tr_sl=zh-CN&_x_tr_tl=en&_x_tr_hl=en&_x_tr_pto=wapp"

        val JUNK_PATTERNS = listOf(
            "溫馨提示",
            "手機小說閱讀網",
            "手機用戶",
            "最新更新時間",
            "提莫書屋",
            "timotxt.com"
        )
    }

    /** No transform for OkHttp — fetch directly from timotxt.com. */
    override suspend fun transformChapterUrl(url: String): String = url

    /** Convert timotxt.com → translate.goog with params for the browser. */
    override suspend fun transformWebviewUrl(url: String): String = toTranslateProxyUrl(url)

    private fun toTranslateProxyUrl(url: String): String {
        if (url.isBlank()) return url
        var cleanUrl = url
            .replace(Regex("[?&]_x_tr_(sl|tl|hl|pto|pto_ctx)=[^&]*"), "")
            .replace("?&", "?")
            .replace(Regex("[?&]$"), "")
            .trimEnd('&', '?')
        cleanUrl = cleanUrl.replace(
            "https://www.timotxt.com",
            "https://www-timotxt-com.translate.goog"
        )
        val separator = if (cleanUrl.contains("?")) "&" else "?"
        return "$cleanUrl$separator$TRANSLATE_PARAMS"
    }

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".chapter-content h1, .chapter-content .title, h1.chapter-title, h1.imgtext, h1")
                ?.text()
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            val contentEl = doc.selectFirst("div.chapter-content div.content")
                ?: doc.selectFirst("div.chapter-content")
                ?: doc.selectFirst("div.content")
                ?: return@withContext ""

            contentEl.also {
                it.select(".gadBlock").remove()
                it.select("script").remove()
                it.select("style").remove()
                it.select("iframe").remove()
            }

            TextExtractor.get(contentEl).cleanJunk()
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            doc.selectFirst(".cover img[src]")
                ?.attr("src")
                ?: doc.selectFirst("meta[property=og:image]")
                    ?.attr("content")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val metaDesc = doc.selectFirst("meta[name=description]")
                ?.attr("content")
                ?.trim()
            if (!metaDesc.isNullOrBlank()) metaDesc
            else doc.selectFirst(".intro")?.let { TextExtractor.get(it).trim() }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val dirUrl = bookUrl.toUrlBuilderSafe().addPath("dir").toString()
            val doc = networkClient.get(dirUrl).toDocument()

            // The /dir page has two <ul> inside .chaplist: a 12-link "latest"
            // sidebar (newest first) and an ".all" complete list (oldest
            // first). Target the .all list — already in reader order.
            val chapterLinks = doc.select(".chaplist ul.all li a[href]")
                .takeIf { it.isNotEmpty() }
                ?: doc.select(".chaplist ul li a[href]")

            chapterLinks.map {
                ChapterResult(
                    title = it.text().trim(),
                    url = URI(baseUrl).resolve(it.attr("href")).toString()
                )
            }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = catalogUrl.toUrlBuilderSafe()
                .add("page", page.toString())
                .toString()

            val doc = networkClient.get(url).toDocument()
            val books = doc.select("ul.list.flex > li").mapNotNull {
                val link = it.selectFirst("h3 a[href], a[href]") ?: return@mapNotNull null
                val cover = it.selectFirst("img[src]")?.attr("src") ?: ""
                val title = link.text().trim()
                if (title.isBlank()) return@mapNotNull null
                BookResult(
                    title = title,
                    url = URI(baseUrl).resolve(link.attr("href")).toString(),
                    coverImageUrl = cover
                )
            }

            val hasNextPage = doc.selectFirst("li.next.pagination-link:not(.disabled)") != null
            PagedList(list = books, index = index, isLastPage = !hasNextPage)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            if (input.startsWith("http") && input.contains("timotxt.com")) {
                val doc = networkClient.get(input).toDocument()
                val title = doc.selectFirst("h1, .book-name, .book-title")
                    ?.text()?.trim() ?: return@tryConnect PagedList.createEmpty(index)
                val cover = doc.selectFirst(".cover img[src]")
                    ?.attr("src")
                    ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
                    ?: ""
                return@tryConnect PagedList(
                    list = listOf(BookResult(title = title, url = input, coverImageUrl = cover)),
                    index = index,
                    isLastPage = true
                )
            }

            val url = baseUrl.toUrlBuilderSafe().addPath("search").addPath(input).toString()
            val doc = networkClient.get(url).toDocument()
            val books = doc.select("ul.list.flex > li").mapNotNull {
                val link = it.selectFirst("h3 a[href], a[href]") ?: return@mapNotNull null
                val cover = it.selectFirst("img[src]")?.attr("src") ?: ""
                val title = link.text().trim()
                if (title.isBlank()) return@mapNotNull null
                BookResult(
                    title = title,
                    url = URI(baseUrl).resolve(link.attr("href")).toString(),
                    coverImageUrl = cover
                )
            }

            PagedList(list = books, index = index, isLastPage = true)
        }
    }
}

/** Remove junk text patterns from extracted content */
fun String.cleanJunk(): String {
    var result = this
    for (pattern in TimoTxt.JUNK_PATTERNS) {
        result = result.replace(pattern, "")
    }
    return result.replace(Regex("\n{3,}"), "\n\n").trim()
}
