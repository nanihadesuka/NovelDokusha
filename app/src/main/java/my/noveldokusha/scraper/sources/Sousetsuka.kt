package my.noveldokusha.scraper.sources

import my.noveldokusha.scraper.scrubber
import my.noveldokusha.scraper.source_interface
import org.jsoup.nodes.Document

class Sousetsuka : source_interface.base
{
	override val name = "Sousetsuka"
	override val baseUrl = "https://www.sousetsuka.com/"
	
	override suspend fun getChapterTitle(doc: Document): String? = doc.title().ifBlank { null }
	
	override suspend fun getChapterText(doc: Document): String
	{
		return doc.selectFirst(".post-body.entry-content")!!
			.let { scrubber.getNodeStructuredText(it) }
	}
}