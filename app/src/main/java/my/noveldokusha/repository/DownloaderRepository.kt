package my.noveldokusha.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.data.map
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.tryFlatConnect
import my.noveldokusha.scraper.ChapterDownload
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.TextExtractor
import my.noveldokusha.utils.toDocument
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

        tryFlatConnect {
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

        tryFlatConnect {
            scrap.getBookDescription(bookUrl)
        }
    }

    suspend fun bookChapter(
        chapterUrl: String,
    ): Response<ChapterDownload> = withContext(Dispatchers.Default) {
        tryFlatConnect {
            val request = getRequest(chapterUrl)
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
                val data = ChapterDownload(
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
    ): Response<List<my.noveldokusha.feature.local_database.tables.Chapter>> = withContext(Dispatchers.Default) {
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

        tryFlatConnect { scrap.getChapterList(bookUrl) }
            .map { chapters ->
                chapters.mapIndexed { index, it ->
                    my.noveldokusha.feature.local_database.tables.Chapter(
                        title = it.title,
                        url = it.url,
                        bookUrl = bookUrl,
                        position = index
                    )
                }
            }
    }
}


private fun heuristicChapterExtraction(url: String, document: Document): ChapterDownload? {
    Readability4JExtended(url, document).parse().also { article ->
        val content = article.articleContent ?: return null
        return ChapterDownload(
            body = TextExtractor.get(content),
            title = article.title
        )
    }
}
