package my.noveldokusha.scraper.sources

import my.noveldokusha.scraper.scrubber
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.divinedaolibrary.com/category/the-undead-king-of-the-palace-of-darkness/
 * Chapter url example:
 * https://www.divinedaolibrary.com/the-undead-king-of-the-palace-of-darkness-chapter-22-the-merciful-grim-reaper/
 */
class DivineDaoLibrary : scrubber.source_interface.base
{
	override val name = "Divine Dao Library"
	override val baseUrl = "https://www.divinedaolibrary.com/"
	
	override suspend fun getChapterText(doc: Document): String
	{
		return doc.selectFirst(".entry-content")
			.also { it.select("a").remove() }
			.let { scrubber.getNodeTextTransversal(it).joinToString("\n\n") }
	}
}