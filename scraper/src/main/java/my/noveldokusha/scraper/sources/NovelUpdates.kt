package my.noveldokusha.scraper.sources

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.add
import my.noveldokusha.network.postPayload
import my.noveldokusha.network.postRequest
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.toUrlBuilderSafe
import my.noveldokusha.network.tryConnect
import my.noveldokusha.network.tryFlatConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.novelupdates.com/series/mushoku-tensei-old-dragons-tale/
 * Chapter url example:
 * (redirected url) Doesn't have chapters, assume it redirects to different website
 */
class NovelUpdates(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "novel_updates"
    override val nameStrId = R.string.source_name_novel_updates
    override val baseUrl = "https://www.novelupdates.com/"
    override val catalogUrl = "https://www.novelupdates.com/novelslisting/?sort=7&order=1&status=1"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        // Given novel updates only host the chapters list (they redirect to a different webpage) and
        // not the content, it will return nothing.
        return ""
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("div.seriesimg > img[src]")
                ?.attr("src")
                ?.let { if (it == "https://www.novelupdates.com/img/noimagefound.jpg") null else it }
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst("#editdescription")
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val request = postRequest("https://www.novelupdates.com/wp-admin/admin-ajax.php")
                .postPayload {
                    add("action", "nd_getchapters")
                    add("mygrr", doc.selectFirst("#grr_groups")!!.attr("value"))
                    add("mygroupfilter", "")
                    add("mypostid", doc.selectFirst("#mypostid")!!.attr("value"))
                }

            networkClient.call(request)
                .toDocument()
                .select("a[href]")
                .asSequence()
                .filter { it.hasAttr("data-id") }
                .map {
                    val title = it.selectFirst("span")!!.attr("title")
                    val url = "https:" + it.attr("href")
                    ChapterResult(title = title, url = url)
                }.toList().reversed()
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryFlatConnect {
            val page = index + 1
            val url = catalogUrl.toUrlBuilderSafe().apply {
                add("st", 1)
                if (page > 1) add("pg", page)
            }
            getSearchList(index, url)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryFlatConnect {
            val page = index + 1
            val url = baseUrl.toUrlBuilderSafe().apply {
                if (page > 1) appendPath("page").appendPath(page.toString())
                add("s", input)
                add("post_type", "seriesplans")
            }
            getSearchList(index, url)
        }
    }

    private suspend fun getSearchList(index: Int, url: Uri.Builder) = tryConnect(
        extraErrorInfo = "index: $index\nurl: $url"
    ) {
        val doc = networkClient.get(url).toDocument()
        doc.select(".search_main_box_nu")
            .mapNotNull {
                val title = it.selectFirst(".search_title > a[href]") ?: return@mapNotNull null
                val image = it.selectFirst(".search_img_nu > img[src]")?.attr("src") ?: ""
                BookResult(
                    title = title.text(),
                    url = title.attr("href"),
                    coverImageUrl = image
                )
            }
            .let {
                PagedList(
                    list = it,
                    index = index,
                    isLastPage = isLastPage(doc)
                )
            }
    }

    private fun isLastPage(doc: Document) =
        when (val nav = doc.selectFirst("div.digg_pagination")) {
            null -> true
            else -> nav.children().last()?.`is`(".current") ?: true
        }
}
