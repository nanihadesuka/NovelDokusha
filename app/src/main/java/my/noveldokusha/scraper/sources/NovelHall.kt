package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

// ALL OK
class NovelHall : SourceInterface.Catalog {
    override val name = "NovelHall"
    override val baseUrl = "https://www.novelhall.com/"
    override val catalogUrl = "https://www.novelhall.com/all.html"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc
            .selectFirst("div#htmlContent")!!
            .let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc
            .selectFirst(".book-img.hidden-xs")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescription(doc: Document): String? {
        return doc
            .selectFirst("span.js-close-wrap")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc.select("#morelist a[href]").map {
            ChapterMetadata(title = it.text(), url = baseUrl + it.attr("href"))
        }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
        val url = baseUrl.toUrlBuilderSafe().apply {
            if (page == 1) addPath("all.html")
            else addPath("all-$page.html")
        }

        return tryConnect {
            val doc = fetchDoc(url)
            doc.select("li.btm")
                .mapNotNull {
                    val link = it.selectFirst("a[href]") ?: return@mapNotNull null
                    BookMetadata(
                        title = link.text(),
                        url = baseUrl + link.attr("href").removePrefix("/"),
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = isLastPage(doc)
                        )
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank())
            return Response.Success(PagedList.createEmpty(index = index))

        val url = baseUrl.toUrlBuilderSafe().apply {
            addPath("index.php")
            add("s", "so")
            add("module", "book")
            add("keyword", input)
        }
        return tryConnect {
            val doc = fetchDoc(url)
            doc.selectFirst(".section3.inner.mt30 > table")
                ?.select("tr > td:nth-child(2) > a[href]")
                .let { it ?: listOf() }
                .map { link ->
                    BookMetadata(
                        title = link.text(),
                        url = baseUrl + link.attr("href").removePrefix("/"),
                    )
                }
                .let {
                    Response.Success(
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = isLastPage(doc)
                        )
                    )
                }
        }
    }

    private fun isLastPage(doc: Document) = when (val nav = doc.selectFirst("div.page-nav")) {
        null -> true
        else -> nav.children().last()?.`is`("span") ?: true
    }
}
