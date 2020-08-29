package my.noveldokusha

import org.json.JSONArray
import org.jsoup.Jsoup

object scrubber {

	interface WebsiteNovelInterface {
		val baseUrl: String
		fun getNovelChapersListPageUrl(novelName: String): String
		fun getNovelChapterPageUrl(novelName: String, chapterIndex: Int): String
		fun getChapter(novelName: String, chapterIndex: Int): Chapter
		fun getChaptersList(novelName: String): Array<Chapter>
	}

	data class Chapter(val text: String, val title: String, val index: Int)

	//    object lightNovelsTranslations : NovelInterface {
	//        val url =
	//            "https://lightnovelstranslations.com/road-to-kingdom/chapter-456-the-end-of-meldora/"
	//
	//        fun getChapter(number: Int): Chapter {
	//            val doc = Jsoup.connect(url).get()
	//            val title = doc.select(".entry-title").first().text().trim()
	//            val text = doc.select(".entry-content > p").map {
	//                it.select("br").append("**LINEBREAK**")
	//                it.text().split("**LINEBREAK**").map { it.trim() }.joinToString("\n")
	//            }.joinToString("\n\n")
	//
	//            return Chapter(text, title)
	//        }
	//    }

	/**
	 * Novel main page (chapter list) example:
	 * https://webnovelonline.com/novel/the_beginning_after_the_end/
	 * Chapter url example:
	 * https://webnovelonline.com/chapter/the_beginning_after_the_end/chapter-270
	 */
	object source_WebNovelOnline : WebsiteNovelInterface {
		override val baseUrl = "https://webnovelonline.com"

		override fun getNovelChapersListPageUrl(novelName: String): String {
			return "${baseUrl}/novel/${novelName}/"
		}

		override fun getNovelChapterPageUrl(novelName: String, chapterIndex: Int): String {
			return "${baseUrl}/chapter/${novelName}/chapter-${chapterIndex + 1}"
		}

		override fun getChapter(novelName: String, chapterIndex: Int): Chapter {
			val doc = Jsoup.connect(getNovelChapterPageUrl(novelName, chapterIndex)).get()
			val raw = doc.toString()

			val rawText = raw.substringAfter("\"chapter\":\"").substringBefore("},")
			val text =
				Jsoup.parse(rawText).select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
					.joinToString("\n\n")

			val title = doc.select(".chapter-info > h3").first().ownText().trim()

			return Chapter(text, title, chapterIndex)
		}

		override fun getChaptersList(novelName: String): Array<Chapter> {
			val doc = Jsoup.connect(getNovelChapersListPageUrl(novelName)).get()
			val raw = doc.toString()


			val rawJSON =
				raw.substringAfter("<script>window._INITIAL_DATA_ = ").substringBefore(";</script>")

			val novelObject = JSONArray(rawJSON).getJSONObject(3).getJSONObject("novel")
			val chapter_list = novelObject.getJSONArray("chapter_list")

			return Array<Chapter>(chapter_list.length()) {
				Chapter("", chapter_list.getJSONObject(it).getString("title"), it)
			}
		}
	}
}