package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.tryFlatConnect

suspend fun downloadBookDescription(
    scraper: Scraper,
    networkClient: NetworkClient,
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