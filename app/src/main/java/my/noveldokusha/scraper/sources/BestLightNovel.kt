package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

class BestLightNovel : SourceInterface.catalog
{
	override val name = "BestLightNovel"
	override val baseUrl = "https://bestlightnovel.com/"
	override val catalogUrl = "https://bestlightnovel.com/novel_list"
	override val language = "English"
	
	override suspend fun getChapterTitle(doc: Document): String? = null
	
	override suspend fun getChapterText(doc: Document): String
	{
		return doc.selectFirst("#vung_doc")!!.let {
			scrubber.getNodeStructuredText(it)
		}
	}
	
	override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
	{
		return doc.select("div.chapter-list a[href]").map {
			ChapterMetadata(title = it.text(), url = it.attr("href"))
		}.reversed()
	}
	
	override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
	{
		return tryConnect {
			
			val url = catalogUrl.toUrlBuilder()!!.apply {
				add("type", "topview")
				add("category", "all")
				add("state", "all")
				add("page", index + 1)
			}
			fetchDoc(url)
				.select("h3.nowrap a[href]")
				.map { BookMetadata(title = it.text(), url = baseUrl + it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		if (input.isBlank() || index > 0)
			return Response.Success(listOf())
		
		return tryConnect {
			
			val url = baseUrl.toUrlBuilder()!!.apply {
				appendPath("search_novels")
				appendEncodedPath(input.replace(" ", "_"))
			}
			
			fetchDoc(url)
				.select("h3.nowrap a[href]")
				.filter { it.text().isNotBlank() }
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
}
