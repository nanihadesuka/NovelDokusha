package my.noveldokusha.scraper.databases

import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.*
import my.noveldokusha.scraper.scrubber.getNodeTextTransversal
import org.jsoup.nodes.Document

/**
 * Novel main page example:
 * https://www.novelupdates.com/series/mushoku-tensei/
 */
class NovelUpdates : scrubber.database_interface
{
	override val id = "novel_updates"
	override val name = "Novel Updates"
	override val baseUrl = "https://www.novelupdates.com/"
	
	override suspend fun getSearchAuthorSeries(index: Int, urlAuthorPage: String): Response<List<BookMetadata>>
	{
		if (index > 0) return Response.Success(listOf())
		
		return tryConnect {
			fetchDoc(urlAuthorPage)
				.select("div.search_title > a[href]")
				.map { BookMetadata(title = it.text(), url = it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getSearchGenres(): Response<Map<String, String>>
	{
		return searchGenresCache.fetch {
			tryConnect {
				fetchDoc("https://www.novelupdates.com/series-finder/")
					.select(".genreme")
					.associate { it.text().trim() to it.attr("genreid")!! }
					.let { Response.Success(it) }
			}
		}
	}
	
	override suspend fun getSearch(index: Int, input: String): Response<List<BookMetadata>>
	{
		val page = index + 1
		val url = baseUrl.toUrlBuilder().apply {
			if (page > 1) appendPath("page").appendPath(page.toString())
			appendPath("pagePath")
			add("s", input)
			add("post_type", "seriesplans")
		}
		
		return tryConnect("page: $page\nurl: $url") {
			fetchDoc(url)
				.select(".search_title")
				.map { it.selectFirst("a") }
				.map { BookMetadata(it.text(), it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override suspend fun getSearchAdvanced(index: Int, genresIncludedId: List<String>, genresExcludedId: List<String>):
			Response<List<BookMetadata>>
	{
		val page = index + 1
		val url = "https://www.novelupdates.com/series-finder/?sf=1".toUrlBuilder().apply {
			if (genresIncludedId.isNotEmpty()) add("gi", genresIncludedId.joinToString(",")).add("mgi", "and")
			if (genresExcludedId.isNotEmpty()) add("ge", genresExcludedId.joinToString(","))
			add("sort", "sdate")
			add("order", "desc")
			if (page > 1) add("pg", page)
		}
		
		return tryConnect("page: $page\nurl: $url") {
			fetchDoc(url)
				.select(".search_title")
				.map { it.selectFirst("a") }
				.map { BookMetadata(it.text(), it.attr("href")) }
				.let { Response.Success(it) }
		}
	}
	
	override fun getBookData(doc: Document): scrubber.database_interface.BookData
	{
		val relatedBooks = doc
			.select("h5")
			.find { it.hasClass("seriesother") && it.text() == "Related Series" }!!
			.nextElementSiblings().asSequence()
			.takeWhile { elem -> !elem.`is`("h5") }
			.filter { it.`is`("a[href]") }
			.map { BookMetadata(it.text(), it.attr("href")) }.toList()
		
		val similarRecommended = doc
			.select("h5")
			.find { it.hasClass("seriesother") && it.text() == "Recommendations" }!!
			.nextElementSiblings().asSequence()
			.takeWhile { elem -> !elem.`is`("h5") }
			.filter { it.`is`("a[href]") }
			.map { BookMetadata(it.text(), it.attr("href")) }.toList()
		
		val authors = doc
			.selectFirst("#showauthors")
			.select("a[href]")
			.map { scrubber.database_interface.BookAuthor(name = it.text(), url = it.attr("href")) }
		
		return scrubber.database_interface.BookData(
			title = doc.selectFirst(".seriestitlenu").text(),
			description = getNodeTextTransversal(doc.selectFirst("#editdescription")).joinToString("\n\n"),
			alternativeTitles = getNodeTextTransversal(doc.selectFirst("#editassociated")),
			relatedBooks = relatedBooks,
			similarRecommended = similarRecommended,
			bookType = doc.selectFirst(".genre, .type").text(),
			genres = doc.selectFirst("#seriesgenre").select("a").map { it.text() },
			tags = doc.selectFirst("#showtags").select("a").map { it.text() },
			authors = authors,
			coverImageUrl = doc.selectFirst("div.seriesimg > img[src]")?.attr("src")
		)
	}
}
