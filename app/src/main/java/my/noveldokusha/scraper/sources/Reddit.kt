package my.noveldokusha.scraper.sources

import my.noveldokusha.scraper.scrubber
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.toUrlBuilder
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * Doesn't have main page
 * Chapter url example: (redirected)
 * https://www.reddit.com/r/mushokutensei/comments/g50ry7/translation_old_dragons_tale_chapter_1_dragon_and/
 */
class Reddit : SourceInterface.base
{
	override val name = "Reddit"
	override val baseUrl = "https://www.reddit.com/"
	
	override suspend fun transformChapterUrl(url: String): String
	{
		return url.toUrlBuilder()!!.authority("old.reddit.com").toString()
	}
	
	override suspend fun getChapterTitle(doc: Document): String? = doc.title().ifBlank { null }
	
	override suspend fun getChapterText(doc: Document): String
	{
		return doc.selectFirst(".linklisting")!!
			.selectFirst(".usertext-body, .may-blank-within, .md-container")!!
			.let {
				it.select("table").remove()
				it.select("blockquote").remove()
				scrubber.getNodeStructuredText(it)
			}
	}
}