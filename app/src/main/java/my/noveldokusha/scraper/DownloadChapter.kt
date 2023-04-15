package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.getRequest
import my.noveldokusha.network.tryFlatConnect
import my.noveldokusha.utils.toDocument
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.nodes.Document

suspend fun downloadChapter(
    scraper: Scraper,
    networkClient: NetworkClient,
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
        val chapter = heuristicChapterExtraction(realUrl, networkClient.get(realUrl).toDocument())
        when (chapter) {
            null -> Response.Error(error, Exception("Unable to extract chapter data with heuristics"))
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