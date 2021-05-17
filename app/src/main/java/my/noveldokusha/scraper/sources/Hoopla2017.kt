package my.noveldokusha.scraper.sources

import my.noveldokusha.scraper.scrubber
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * Doesn't have main page
 * Chapter url example: (redirected)
 * https://rtd.moe/kumo-desu-ga/kumo-desu-ga-nani-ka-final-battle-%E2%91%A3/
 */
class Hoopla2017 : scrubber.source_interface.base
{
	override val name = "hoopla2017"
	override val baseUrl = "https://hoopla2017.wordpress.com/"
	
	override suspend fun getChapterText(doc: Document): String
	{
		val title = scrubber.getNodeTextTransversal(doc.selectFirst(".entry-title"))
		val body = scrubber.getNodeTextTransversal(doc.selectFirst(".entry-content"))
		
		return (title + body).joinToString("\n\n")
	}
}