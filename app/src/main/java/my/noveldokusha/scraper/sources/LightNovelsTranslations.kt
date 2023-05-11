package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.add
import my.noveldokusha.utils.addPath
import my.noveldokusha.utils.toDocument
import my.noveldokusha.utils.toJson
import my.noveldokusha.utils.toUrlBuilderSafe

/**
 * Novel main page (chapter list) example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/
 * Chapter url example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/the-sage-summoned-to-another-world-volume-1-chapter-1/
 */
class LightNovelsTranslations(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "light_novel_translations"
    override val nameStrId = R.string.source_name_light_novel_translations
    override val baseUrl = "https://lightnovelstranslations.com/"
    override val catalogUrl = "https://lightnovelstranslations.com/"
    override val iconUrl =
        "https://c10.patreonusercontent.com/4/patreon-media/p/campaign/458169/797a2e9b03094435947635c4da0fc683/eyJ3IjoyMDB9/1.jpeg?token-time=2145916800&token-hash=2gkkI3EgQqRPh5dQe9uxrULjURfQVm60BHKUdh91MtE%3D"
    override val language = LanguageCode.ENGLISH

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .selectFirst(".novel-image img[src]")
                ?.attr("src")
                ?.toUrlBuilderSafe()
                ?.clearQuery()
                ?.toString()
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl).toDocument()
                .selectFirst(".novel_text")!!
                .let {
                    it.select(".alternate_titles").remove()
                    TextExtractor.get(it).trim()
                }
        }
    }

    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val url = bookUrl.toUrlBuilderSafe()
                .add("tab", "table_contents")

            networkClient.get(url)
                .toDocument()
                .select(".chapter-item a[href]")
                .map {
                    ChapterMetadata(
                        title = it.text(),
                        url = it.attr("href")
                    )
                }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            val page = index + 1

            val url = "https://lightnovelstranslations.com".toUrlBuilderSafe().apply {
                addPath("read")
                if (page > 1) {
                    addPath("page", page.toString())
                }
                add("sortby", "highest-rated")
            }

            networkClient.get(url).toDocument()
                .select(".read_list-story-item")
                .mapNotNull {
                    val titleElement = it.selectFirst(".read_list-story-item--title a[href]")
                        ?: return@mapNotNull null

                    BookMetadata(
                        title = titleElement.text(),
                        url = titleElement.attr("href"),
                        coverImageUrl = it.selectFirst(".item_thumb img[src]")
                            ?.attr("src") ?: "",
                        description = it.selectFirst(".read_list-story-item--short_description")
                            ?.text() ?: ""
                    )
                }
                .let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = it.isEmpty()
                    )
                }
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0)
                return@tryConnect PagedList.createEmpty(index = index)

            val url = "https://lightnovelstranslations.com/wp-admin/admin-ajax.php"
                .toUrlBuilderSafe()
                .apply {
                    add("action", "search_novel_header")
                    add("search_key", input)
                }

            networkClient.get(url)
                .toJson()
                .asJsonArray
                .map { it.asJsonArray }
                .map {
                    BookMetadata(
                        title = it[1].asString,
                        url = it[2].asString,
                        coverImageUrl = it[3].asString
                    )
                }.let {
                    PagedList(
                        list = it,
                        index = index,
                        isLastPage = true
                    )
                }
        }
    }
}