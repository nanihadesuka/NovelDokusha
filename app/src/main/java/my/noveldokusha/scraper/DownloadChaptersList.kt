package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.map
import my.noveldokusha.network.tryFlatConnect

suspend fun downloadChaptersList(
    scraper: Scraper,
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

    tryFlatConnect { scrap.getChapterList(bookUrl) }
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