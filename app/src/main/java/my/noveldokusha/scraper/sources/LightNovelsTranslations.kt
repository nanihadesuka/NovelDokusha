package my.noveldokusha.scraper.sources

import my.noveldokusha.*
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.*

/**
 * Novel main page (chapter list) example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/
 * Chapter url example:
 * https://lightnovelstranslations.com/the-sage-summoned-to-another-world/the-sage-summoned-to-another-world-volume-1-chapter-1/
 */
class LightNovelsTranslations : scrubber.source_interface.catalog
{
	override val name = "Light Novel Translations"
	override val baseUrl = "https://lightnovelstranslations.com/"
	override val catalogUrl = "https://lightnovelstranslations.com/"
	
	override suspend fun getChapterTitle(doc: Document): String? = doc.selectFirst("h1.entry-title")?.text()
	
	override suspend fun getChapterText(doc: Document): String
	{
		return doc.selectFirst(".page, .type-page, .status-publish, .hentry")!!
			.selectFirst(".entry-content").run {
				this!!.select("#textbox").remove()
				scrubber.getNodeStructuredText(this)
			}
	}
	
	override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
	{
		return doc
			.select(".su-spoiler-content.su-u-clearfix.su-u-trim a[href]")
			.map {
				val url = it.attr("href")
				val decoded_url = URLDecoder.decode(url, "UTF-8")
				val title: String = Regex(""".+/(.+)/?$""").find(decoded_url)?.destructured?.run {
					this.component1().replace("-", " ").removeSuffix("/")
				} ?: it.text()
				
				ChapterMetadata(title = title.trim().capitalize(Locale.ROOT), url = url)
			}
	}
	
	override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
	{
		val page = index + 1
		if (page > 1)
			return Response.Success(listOf())
		
		return tryConnect {
			fetchDoc(catalogUrl)
				.selectFirst("#prime_nav")!!
				.children()
				.subList(1, 4)
				.flatMap { it.select("a[href]") }
				.filter {
					val url = it.attr("href")
					val text = it.text()
					return@filter url != "#" &&
					              !url.endsWith("-illustrations/") &&
					              !url.endsWith("-illustration/") &&
					              !url.endsWith("-illustration-page/") &&
					              text != "Novel Illustrations" &&
					              text != "Novels Illustrations"
				}
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		if (input.isBlank() || index > 0)
			return Response.Success(listOf())
		
		val url = baseUrl.toUrlBuilder()!!.apply {
			add("order", "DESC")
			add("orderby", "relevance")
			add("s", input)
		}
		
		return tryConnect {
			fetchDoc(url)
				.selectFirst(".jetpack-search-filters-widget__filter-list")!!
				.select("a[href]")
				.map {
					val (name) = Regex("""^.*category_name=(.*)$""").find(it.attr("href"))!!.destructured
					BookMetadata(title = it.text(), url = "https://lightnovelstranslations.com/${name}/")
				}.let { Response.Success(it) }
		}
	}
}