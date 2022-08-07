package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.Response
import my.noveldokusha.network.tryConnect
import my.noveldokusha.utils.toDocument

suspend fun downloadBookDescription(
    scraper: Scraper,
    networkClient: NetworkClient,
    bookUrl: String,
): Response<String> = withContext(Dispatchers.IO) {
    val error by lazy {
        """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
    }

    // Return if can't find compatible source for url
    val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
        ?: return@withContext Response.Error(error)

    tryConnect {
        val doc = networkClient.get(bookUrl).toDocument()
        scrap.getBookDescription(doc)
            ?.let { Response.Success(it) }
            ?: Response.Error("")
    }
}