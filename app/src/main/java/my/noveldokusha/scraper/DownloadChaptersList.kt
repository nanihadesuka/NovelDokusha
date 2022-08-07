package my.noveldokusha.scraper

import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.Response
import my.noveldokusha.network.tryConnect
import my.noveldokusha.utils.toDocument

suspend fun downloadChaptersList(
    scraper: Scraper,
    networkClient: NetworkClient,
    bookUrl: String,
): Response<List<Chapter>> {
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
        val doc = networkClient.get(bookUrl).toDocument()
        scrap.getChapterList(doc)
            .mapIndexed { index, it ->
                Chapter(
                    title = it.title,
                    url = it.url,
                    bookUrl = bookUrl,
                    position = index
                )
            }
            .let { Response.Success(it) }
    }
}