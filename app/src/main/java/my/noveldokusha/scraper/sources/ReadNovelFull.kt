package my.noveldokusha.scraper.sources

import my.noveldokusha.BookMetadata
import my.noveldokusha.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://readnovelfull.com/i-was-a-sword-when-i-reincarnated.html
 * Chapter url example:
 * https://readnovelfull.com/i-was-a-sword-when-i-reincarnated.html
 */
class ReadNovelFull : scrubber.source_interface.catalog
{
	override val name = "Read Novel Full"
	override val baseUrl = "https://readnovelfull.com"
	override val catalogUrl = "https://readnovelfull.com/most-popular-novel"
	
	override suspend fun getChapterText(doc: Document): String
	{
		doc.selectFirst("#chr-content").let {
			return scrubber.getNodeTextTransversal(it).joinToString("\n\n")
		}
	}
	
	override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
	{
		val id = doc.selectFirst("#rating").attr("data-novel-id")
		return Jsoup.connect("https://readnovelfull.com/ajax/chapter-archive")
			.addUserAgent()
			.addHeaderRequest()
			.data("novelId", id)
			.getIO()
			.select("a[href]")
			.map { ChapterMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
	}
	
	override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
	{
		val page = index + 1
		val url = catalogUrl.toUrlBuilder().apply {
			if (page > 1) add("page", page)
		}
		
		return tryConnect {
			fetchDoc(url)
				.selectFirst("#list-page")
				.select(".row")
				.map { it.selectFirst("a[href]") }
				.map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		if (input.isBlank() || index > 0)
			return Response.Success(listOf())
		
		val url = baseUrl.toUrlBuilder().apply {
			appendPath("search")
			add("keyword", input)
		}
		
		return tryConnect {
			fetchDoc(url)
				.selectFirst(".col-novel-main, .archive")
				.select(".novel-title")
				.map { it.selectFirst("a[href]") }
				.map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
}
