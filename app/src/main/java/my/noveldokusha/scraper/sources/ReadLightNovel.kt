package my.noveldokusha.scraper.sources

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.scraper.*
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://www.readlightnovel.org/goat-of-all-ghouls-1
 * Chapter url example:
 * https://www.readlightnovel.org/goat-of-all-ghouls-1/chapter-1
 */
class ReadLightNovel : SourceInterface.catalog
{
	
	override val name = "Read Light Novel"
	override val baseUrl = "https://www.readlightnovel.me/"
	override val catalogUrl = "https://www.readlightnovel.me/novel-list"
	override val language = "English"
	
	override suspend fun getChapterTitle(doc: Document): String? = doc.selectFirst(".chapter-content3 h4")?.text()
	
	override suspend fun getChapterText(doc: Document): String
	{
		doc.selectFirst(".chapter-content3 > .desc")!!.let {
			it.select("script").remove()
			it.select("a").remove()
			it.select(".ads-title").remove()
			it.select(".hidden").remove()
			return scrubber.getNodeStructuredText(it)
		}
	}

	override suspend fun getChapterList(doc: Document): List<ChapterMetadata>
	{
		return doc.select(".chapter-chs").select("a[href]").map { ChapterMetadata(title = it.text(), url = it.attr("href")) }
	}
	
	val catalogIndex by lazy { ("ABCDEFGHIJKLMNOPQRSTUVWXYZ").split("") }
	
	override suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
	{
		val url = catalogUrl.toUrlBuilder()!!.apply {
			if (index == 0) return@apply
			val letter = catalogIndex.elementAtOrNull(index - 1) ?: return Response.Success(listOf())
			appendPath(letter)
		}
		
		return tryConnect {
			fetchDoc(url)
				.selectFirst(".list-by-word-body")!!
				.child(0)
				.children()
				.mapNotNull { it.selectFirst("a[href]") }
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		if (input.isBlank() || index > 0)
			return Response.Success(listOf())
		
		return tryConnect {
			connect("https://www.readlightnovel.org/search/autocomplete")
				.addHeaderRequest()
				.data("q", input)
				.postIO()
				.select("a[href]")
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
}
