package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class Wuxia : SourceInterface.Catalog {
    override val name = "Wuxia"
    override val baseUrl = "https://www.wuxia.blog/"
    override val catalogUrl = "https://www.wuxia.blog/listNovels"
    override val language = "English"

    override suspend fun getChapterTitle(doc: Document): String? = null

    override suspend fun getChapterText(doc: Document): String {
        return doc.selectFirst("div.panel-body.article")!!.also {
            it.select(".pager").remove()
            it.select(".fa.fa-calendar").remove()
            it.select("button.btn.btn-default").remove()
            it.select("div.recently-nav.pull-right").remove()
        }.let { textExtractor.get(it) }
    }

    override suspend fun getBookCoverImageUrl(doc: Document): String? {
        return doc.selectFirst(".imageCover")
            ?.selectFirst("img[src]")
            ?.attr("src")
    }

    override suspend fun getBookDescripton(doc: Document): String? {
        return doc.selectFirst("div[itemprop=description]")
            ?.let {
                it.select("h4").remove()
                textExtractor.get(it)
            }
    }

    override suspend fun getChapterList(doc: Document): List<ChapterMetadata> {
        val id = doc.selectFirst("#more")!!.attr("data-nid")
        val newChapters = doc.select("#chapters a[href]")
        val res = tryConnect {
            connect("https://wuxia.blog/temphtml/_tempChapterList_all_$id.html")
                .addHeaderRequest()
                .postIO()
                .select("a[href]")
                .let { Response.Success(it) }
        }

        val oldChapters = if (res is Response.Success) res.data else listOf()

        return (newChapters + oldChapters).reversed().map {
            ChapterMetadata(title = it.text(), url = it.attr("href"))
        }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookMetadata>> {
        if (index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        return tryConnect {
            fetchDoc(catalogUrl)
                .select("td.novel a[href]")
                .map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
                .let { Response.Success(PagedList(list = it, index = index, isLastPage = true)) }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        if (input.isBlank() || index > 0)
            return Response.Success(PagedList.createEmpty(index = index))

        val url = baseUrl.toUrlBuilderSafe().apply {
            add("search", input)
        }

        return tryConnect {
            fetchDoc(url)
                .selectFirst("#table")!!
                .children()
                .first()!!
                .children()
                .mapNotNull {
                    val link = it.selectFirst(".xxxx > a[href]") ?: return@mapNotNull null
                    val bookCover = it.selectFirst("img[src]")?.attr("src") ?: ""
                    BookMetadata(
                        title = link.text(),
                        url = link.attr("href"),
                        coverImageUrl = bookCover
                    )
                }
                .let { Response.Success(PagedList(list = it, index = index, isLastPage = true)) }
        }
    }
}
