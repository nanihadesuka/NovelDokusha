package my.noveldokusha.scraper.sources

import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class Aixdzs : SourceInterface.Catalog {
    override val name = "Aixdzs"
    override val baseUrl = "https://www.aixdzs.com/"
    override val catalogUrl = "https://www.aixdzs.com/sort/1/index.html"
    override val language = "Chinese"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc
            .selectFirst(".content")!!
            .let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc
            .selectFirst(".d_af.fdl")
            ?.selectFirst("img[src]")
            ?.attr("src")

    }


    override suspend fun getBookDescripton(doc: Document): String? {
        return doc
            .selectFirst(".d_co")
            ?.let { textExtractor.get(it) }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        return doc.select("div#i-chapter a[href]").map {
            ChapterMetadata(title = it.text(), url = baseUrl + it.attr("href"))
        }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        val page = index + 1
//        val url = baseUrl.toUrlBuilderSafe().apply {
//            if (page == 1) addPath("all.html")
//            else addPath("all-$page.html")
//        }

        return tryConnect {
            val doc = fetchDoc(catalogUrl)

            doc.select(".box_k.mt15 li")
                .mapNotNull {
                    val link = it.selectFirst("h2.b_name a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst(".list_img img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                        url =  baseUrl + link.attr("href"),
                        coverImageUrl = bookCover
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
            addPath("bsearch")
            add("q", input)
        }
        return tryConnect {
            val doc = fetchDoc(url)

            doc.select(".box_k li")
                .mapNotNull {
                    val link = it.selectFirst("h2.b_name a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst(".list_img img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                        url =  baseUrl + link.attr("href"),
                        coverImageUrl = bookCover
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
