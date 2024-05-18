package my.noveldoksuha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.Response
import my.noveldokusha.core.map
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.toDocument
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.tooling.local_database.tables.Chapter
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderRepository @Inject constructor(
    private val scraper: Scraper,
    private val networkClient: NetworkClient,
) {

    suspend fun bookCoverImageUrl(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.noveldokusha.network.tryFlatConnect {
            scrap.getBookCoverImageUrl(bookUrl)
        }
    }

    suspend fun bookDescription(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.noveldokusha.network.tryFlatConnect {
            scrap.getBookDescription(bookUrl)
        }
    }

    suspend fun bookChapter(
        chapterUrl: String,
    ): Response<my.noveldokusha.scraper.ChapterDownload> = withContext(Dispatchers.Default) {
        my.noveldokusha.network.tryFlatConnect {
            val request = my.noveldokusha.network.getRequest(chapterUrl)
            val realUrl = networkClient
                .call(request, followRedirects = true)
                .request.url
                .toString()


            val error by lazy {
                """
				Unable to load chapter from url:
				$chapterUrl
				
				Redirect url:
				$realUrl
				
				Source not supported
			""".trimIndent()
            }

            scraper.getCompatibleSource(realUrl)?.also { source ->
                val doc = networkClient.get(source.transformChapterUrl(realUrl)).toDocument()
                val data = my.noveldokusha.scraper.ChapterDownload(
                    body = source.getChapterText(doc) ?: return@also,
                    title = source.getChapterTitle(doc)
                )
                return@tryFlatConnect Response.Success(data)
            }

            // If no predefined source is found try extracting text with heuristic extraction
            val chapter =
                heuristicChapterExtraction(realUrl, networkClient.get(realUrl).toDocument())
            when (chapter) {
                null -> Response.Error(
                    error,
                    Exception("Unable to extract chapter data with heuristics")
                )
                else -> Response.Success(chapter)
            }
        }
    }

    suspend fun bookChaptersList(
        bookUrl: String,
    ): Response<List<Chapter>> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.noveldokusha.network.tryFlatConnect { scrap.getChapterList(bookUrl) }
            .map { chapters ->
                chapters.mapIndexed { index, it ->
                    Chapter(
                        title = it.title,
                        url = it.url,
                        bookUrl = bookUrl,
                        position = index
                    )
                }
            }
    }
}


private fun heuristicChapterExtraction(url: String, document: Document): my.noveldokusha.scraper.ChapterDownload? {
    Readability4JExtended(url, document).parse().also { article ->
        val content = article.articleContent ?: return null
        return my.noveldokusha.scraper.ChapterDownload(
            body = TextExtractor.get(content),
            title = article.title
        )
    }
}
