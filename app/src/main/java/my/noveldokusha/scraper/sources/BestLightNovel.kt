package my.noveldokusha.scraper.sources

import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class BestLightNovel : SourceInterface.Catalog {
    override val name = "BestLightNovel"
    override val baseUrl = "https://bestlightnovel.com/"
    override val catalogUrl = "https://bestlightnovel.com/novel_list"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst("#vung_doc")!!.let {
            textExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".info_image > img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String? {
        return doc.selectFirst("#noidungm")
            ?.let {
                it.select("h2").remove()
                textExtractor.get(it)
            }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc.select("div.chapter-list a[href]").map {
            ChapterMetadata(title = it.text(), url = it.attr("href"))
        }.reversed()
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        return tryConnect {

            val url = catalogUrl
                .toUrlBuilderSafe()
                .ifCase(page != 1) {
                    add("type", "newest")
                    add("category", "all")
                    add("state", "all")
                    add("page", page)
                }
            parseToBooks(url, index)
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank()) return Response.Success(PagedList.createEmpty(index = index))

        val page = index + 1
        return tryConnect {
            val url = baseUrl
                .toUrlBuilderSafe()
                .addPath("search_novels", input.replace(" ", "_"))
                .ifCase(page != 1) { add("page", page) }
            parseToBooks(url, index)
        }
    }

    suspend fun parseToBooks(url: Uri.Builder, index: Int): Response<PagedList<BookMetadata>> {
        val doc = fetchDoc(url)
        return doc.select(".update_item.list_category")
            .mapNotNull {
                val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                BookMetadata(
                    title = link.attr("title"),
                    url = baseUrl + link.attr("href"),
                    coverImageUrl = bookCover
                )
            }
            .let {
                Response.Success(
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = when (val nav = doc.selectFirst("div.phan-trang")) {
                            null -> true
                            else -> nav.children().takeLast(2).first()?.`is`(".pageselect") ?: true
                        }
                    )
                )
            }
    }
}
