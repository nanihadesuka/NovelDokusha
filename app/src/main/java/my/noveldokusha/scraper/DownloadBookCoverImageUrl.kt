package my.noveldokusha.scraper

suspend fun downloadBookCoverImageUrl(bookUrl: String): Response<String>
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
        scrap.getBookCoverImageUrl(doc)
            ?.let { Response.Success(it) }
            ?: Response.Error("")
    }
}