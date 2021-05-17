package my.noveldokusha.scraper.databases

import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.*
import my.noveldokusha.scraper.scrubber.getNodeTextTransversal
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class BakaUpdates : scrubber.database_interface
{
	override val id = "baka_updates"
	override val name = "Baka-Updates"
	override val baseUrl = "https://www.mangaupdates.com/"
	
	fun String.removeNovelTag() = this.removeSuffix("(Novel)").trim()
	
	override suspend fun getSearchGenres(): Response<Map<String, String>>
	{
		return searchGenresCache.fetch {
			tryConnect {
				fetchDoc("https://www.mangaupdates.com/series.html?act=genresearch")
					.select(".p-1.col-6.text")
					.map { it.text().trim() }
					.associateWith { it.replace(" ", "+") }
					.let { Response.Success(it) }
			}
		}
	}
	
	override suspend fun getSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		val page = index + 1
		val settings = mutableListOf<String>().apply {
			if (page > 1) add("page=$page")
			add("display=list")
			add("perpage=50")
			add("type=novel")
			add("search=${input.urlEncode()}")
		}
		val url = "https://www.mangaupdates.com/series.html?" + settings.joinToString("&")
		
		return tryConnect("page: $page\nurl: $url") {
			fetchDoc(url)
				.select("div.col-6.py-1.py-md-0.text")
				.map { it.selectFirst("a[href]") }
				.map { BookMetadata(it.text().removeNovelTag(), it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getSearchAdvanced(index: Int, genresIncludedId: List<String>, genresExcludedId: List<String>):
			Response<List<BookMetadata>>
	{
		val page = index + 1
		val settings = mutableListOf<String>().apply {
			if (page > 1) add("page=$page")
			add("display=list")
			if (genresIncludedId.isNotEmpty()) add("genre=" + genresIncludedId.joinToString("_"))
			if (genresExcludedId.isNotEmpty()) add("exclude_genre=" + genresExcludedId.joinToString("_"))
			add("type=novel")
			add("perpage=50")
		}
		val url = "https://www.mangaupdates.com/series.html?" + settings.joinToString("&")
		
		return tryConnect("page: $page\nurl: $url") {
			fetchDoc(url)
				.select("div.col-6.py-1.py-md-0.text")
				.map { it.selectFirst("a[href]") }
				.map { BookMetadata(it.text().removeNovelTag(), it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override fun getBookData(doc: Document): scrubber.database_interface.BookData
	{
		fun entry(header: String) = doc.selectFirst("div.sCat > b:containsOwn($header)").parent().nextElementSibling()
		
		val relatedBooks = entry("Category Recommendations")
			.select("a[href]")
			.map { BookMetadata(it.text().removeNovelTag(), "https://www.mangaupdates.com/" + it.attr("href")) }
			.toList()
		
		val similarRecommended = entry("Recommendations")
			.select("a[href]")
			.map { BookMetadata(it.text().removeNovelTag(), "https://www.mangaupdates.com/" + it.attr("href")) }
			.toList()
		
		val authors = entry("Author\\(s\\)")
			.select("a")
			.map { scrubber.database_interface.BookAuthor(name = it.text(), url = it.attr("href")) }
		
		val description = entry("Description").let {
			it.selectFirst("[id=div_desc_more]") ?: it.selectFirst("div")
		}.also { it.select("a").remove() }
			.let { getNodeTextTransversal(it) }
			.joinToString("\n\n")
		
		val tags = entry("Categories")
			.select("li > a")
			.map { it.text() }
		
		return scrubber.database_interface.BookData(
			title = doc.selectFirst(".releasestitle.tabletitle").text().removeNovelTag(),
			description = description,
			alternativeTitles = getNodeTextTransversal(entry("Associated Names")),
			relatedBooks = relatedBooks,
			similarRecommended = similarRecommended,
			bookType = entry("Type").text(),
			genres = entry("Genre").select("a").dropLast(1).map { it.text() },
			tags = tags,
			authors = authors
		)
	}
}