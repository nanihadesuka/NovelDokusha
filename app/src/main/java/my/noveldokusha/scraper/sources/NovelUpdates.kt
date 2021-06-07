package my.noveldokusha.scraper.sources

import my.noveldokusha.BookMetadata
import my.noveldokusha.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.novelupdates.com/series/mushoku-tensei-old-dragons-tale/
 * Chapter url example:
 * (redirected url) Doesn't have chapters, assume it redirects to different website
 */
class NovelUpdates : scrubber.source_interface.catalog
{
	override val name = "Novel Updates"
	override val baseUrl = "https://www.novelupdates.com/"
	override val catalogUrl = "https://www.novelupdates.com/novelslisting/?sort=7&order=1&status=1"
	
	override suspend fun getChapterTitle(doc: Document): String? = null
	
	override suspend fun getChapterText(doc: Document): String
	{
		// Given novel updates only host the chapters list (they redirect to a diferent webpage) and
		// not the content, it will return nothing.
		return ""
	}
	
	override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
	{
		return Jsoup.connect("https://www.novelupdates.com/wp-admin/admin-ajax.php")
			.addUserAgent()
			.addHeaderRequest()
			.data("action", "nd_getchapters")
			.data("mygrr", doc.selectFirst("#grr_groups").attr("value"))
			.data("mygroupfilter", "")
			.data("mypostid", doc.selectFirst("#mypostid").attr("value"))
			.postIO()
			.select("a[href]")
			.asSequence()
			.filter { it.hasAttr("data-id") }
			.map {
				val title = it.selectFirst("span").attr("title")
				val url = "https:" + it.attr("href")
				ChapterMetadata(title = title, url = url)
			}.toList().reversed()
	}
	
	override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
	{
		val page = index + 1
		val url = catalogUrl.toUrlBuilder().apply {
			add("st", 1)
			if (page > 1) add("pg", page)
		}
		
		return tryConnect {
			fetchDoc(url)
				.select(".search_title")
				.map { it.selectFirst("a[href]") }
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		if (input.isBlank() || index > 0)
			return Response.Success(listOf())
		
		val url = baseUrl.toUrlBuilder().apply {
			add("s", input)
		}
		
		return tryConnect {
			Jsoup.connect(url.toString())
				.addUserAgent()
				.getIO()
				.select(".search_body_nu")
				.select(".search_title")
				.select("a[href]")
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
}
