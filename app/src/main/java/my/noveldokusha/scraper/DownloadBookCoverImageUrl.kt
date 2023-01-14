package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.tryConnect
import my.noveldokusha.utils.toDocument

suspend fun downloadBookCoverImageUrl(
    scraper: Scraper,
    networkClient: NetworkClient,
    bookUrl: String,
) = withContext(Dispatchers.IO) {
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

    tryConnect {
        val doc = networkClient.get(bookUrl).toDocument()
        scrap.getBookCoverImageUrl(doc)
            ?.let { Response.Success(it) }
            ?: Response.Error("", Exception())
    }
}