package my.noveldokusha.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun downloadBookCoverImageUrl(bookUrl: String) = withContext(Dispatchers.IO) {
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

    return@withContext tryConnect {
        val doc = fetchDoc(bookUrl)
        scrap.getBookCoverImageUrl(doc)
            ?.let { Response.Success(it) }
            ?: Response.Error("")
    }
}