package my.noveldokusha.scraper

import my.noveldokusha.data.database.tables.Chapter

suspend fun downloadChaptersList(bookUrl: String): Response<List<Chapter>>
{
    val error by lazy {
        """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
    }

    // Return if can't find compatible source for url
    val scrap = scraper.getCompatibleSourceCatalog(bookUrl) ?: return Response.Error(error)

    return tryConnect {
        val doc = fetchDoc(bookUrl)
        scrap.getChapterList(doc)
            .mapIndexed { index, it -> Chapter(title = it.title, url = it.url, bookUrl = bookUrl, position = index) }
            .let { Response.Success(it) }
    }
}