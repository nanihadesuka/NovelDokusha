package my.noveldokusha.scraper

import net.dankito.readability4j.Readability4J

suspend fun downloadChapter(chapterUrl: String): Response<ChapterDownload>
{
    return tryConnect {
        val con = connect(chapterUrl)
            .timeout(30 * 1000)
            .followRedirects(true)
            .executeIO()

        val realUrl = con.url().toString()

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

        // If no predefined source is found try extracting text with Readability4J library
        Readability4J(realUrl, fetchDoc(realUrl)).parse().also { article ->
            val content = article.articleContent ?: return@also
            val data = ChapterDownload(
                body = textExtractor.get(content),
                title = article.title
            )
            return@tryConnect Response.Success(data)
        }

        Response.Error(error)
    }
}