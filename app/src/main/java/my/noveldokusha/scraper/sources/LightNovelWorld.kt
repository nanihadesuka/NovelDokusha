package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.data.Response
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toUrlBuilder
import my.noveldokusha.utils.toUrlBuilderSafe
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated
 * Chapter url example:
 * https://www.lightnovelworld.com/novel/the-devil-does-not-need-to-be-defeated/1348-chapter-0
 */
class LightNovelWorld(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "light_novel_world"
    override val nameStrId = R.string.source_name_light_novel_world
    override val baseUrl = "https://www.lightnovelworld.com/"
    override val catalogUrl = "https://www.lightnovelworld.com/genre/all/popular/all/"
    override val iconUrl =
        "https://static.lightnovelworld.com/content/img/lightnovelworld/favicon.png"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst("#chapter-container")?.let(TextExtractor::get) ?: ""
    }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".cover > img[data-src]")
                ?.attr("data-src")
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".summary > .content")
                ?.let { TextExtractor.get(it) }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<my.noveldokusha.feature.local_database.ChapterMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val list = mutableListOf<my.noveldokusha.feature.local_database.ChapterMetadata>()
            val baseChaptersUrl = bookUrl
                .toUrlBuilderSafe()
                .addPath("chapters")

            for (page in 1..Int.MAX_VALUE) {
                val urlBuilder = baseChaptersUrl.addPath("page-$page")

                val res = networkClient.get(urlBuilder)
                    .toDocument()
                    .select(".chapter-list > li > a")
                    .map {
                        my.noveldokusha.feature.local_database.ChapterMetadata(
                            title = it.attr("title"),
                            url = baseUrl + it.attr("href").removePrefix("/")
                        )
                    }

                if (res.isEmpty())
                    break
                list.addAll(res)
            }
            list
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1
            val url = catalogUrl.toUrlBuilder()!!.apply {
                if (page > 1) addPath(page.toString())
            }
            getBooksList(networkClient.get(url).toDocument(), index)
        }
    }


    // TODO(): too much to do
    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> {
        return Response.Success(PagedList.createEmpty(index = index))
    }

    private fun getBooksList(doc: Document, index: Int) = doc
        .select(".novel-item")
        .mapNotNull {
            val coverUrl =
                it.selectFirst(".novel-cover > img[data-src]")?.attr("data-src") ?: ""
            val book = it.selectFirst("a[title]") ?: return@mapNotNull null
            BookMetadata(
                title = book.attr("title"),
                url = baseUrl + book.attr("href").removePrefix("/"),
                coverImageUrl = coverUrl
            )
        }.let {
            PagedList(
                list = it,
                index = index,
                isLastPage = when (val nav = doc.selectFirst("ul.pagination")) {
                    null -> true
                    else -> nav.children().last()?.`is`(".active") ?: true
                }
            )
        }
}
