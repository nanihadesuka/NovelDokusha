package my.noveldokusha.scraper

import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.nodes.Document

suspend fun downloadChapter(chapterUrl: String): Response<ChapterDownload> {
    return tryConnect {
        val realUrl = getRequest(chapterUrl)
            .let { clientRedirects.call(it) }
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
            val doc = fetchDoc(source.transformChapterUrl(realUrl))
            val data = ChapterDownload(
                body = source.getChapterText(doc) ?: return@also,
                title = source.getChapterTitle(doc)
            )
            return@tryConnect Response.Success(data)
        }

        // If no predefined source is found try extracting text with heuristic extraction
        val chapter = heuristicChapterExtraction(realUrl, fetchDoc(realUrl))
        return@tryConnect when (chapter) {
            null -> Response.Error(error)
            else -> Response.Success(chapter)
        }
    }
}

fun heuristicChapterExtraction(url: String, document: Document): ChapterDownload? {
    Readability4JExtended(url, document).parse().also { article ->
        val content = article.articleContent ?: return null
        return ChapterDownload(
            body = textExtractor.get(content),
            title = article.title
        )
    }
}