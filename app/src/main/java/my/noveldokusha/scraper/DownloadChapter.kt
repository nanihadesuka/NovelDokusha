package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.Response
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.tryConnect
import my.noveldokusha.utils.call
import my.noveldokusha.utils.toDocument
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.nodes.Document

suspend fun downloadChapter(
    scraper: Scraper,
    networkClient: NetworkClient,
    chapterUrl: String,
): Response<ChapterDownload> = withContext(Dispatchers.IO) {
    tryConnect {
        val request = getRequest(chapterUrl)
        val realUrl = networkClient
            .clientWithRedirects
            .call(request)
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
            return@tryConnect Response.Success(data)
        }

        // If no predefined source is found try extracting text with heuristic extraction
        val chapter = heuristicChapterExtraction(realUrl, networkClient.get(realUrl).toDocument())
        when (chapter) {
            null -> Response.Error(error)
            else -> Response.Success(chapter)
        }
    }
}

fun heuristicChapterExtraction(url: String, document: Document): ChapterDownload? {
    Readability4JExtended(url, document).parse().also { article ->
        val content = article.articleContent ?: return null
        return ChapterDownload(
            body = TextExtractor.get(content),
            title = article.title
        )
    }
}