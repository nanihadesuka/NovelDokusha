package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.Response
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.*
import org.jsoup.nodes.Document

class BestLightNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val name = "BestLightNovel"
    override val baseUrl = "https://bestlightnovel.com/"
    override val catalogUrl = "https://bestlightnovel.com/novel_list"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst("#vung_doc")!!.let {
            TextExtractor.get(it)
        }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".info_image > img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc.selectFirst("#noidungm")
            ?.let {
                it.select("h2").remove()
                TextExtractor.get(it)
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
            parseToBooks(networkClient.get(url).toDocument(), index)
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
            parseToBooks(networkClient.get(url).toDocument(), index)
        }
    }

    private fun parseToBooks(doc: Document, index: Int): Response<PagedList<BookMetadata>> {

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
