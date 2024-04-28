package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import org.jsoup.nodes.Document

// NO LONGER EXISTS
class AT(
    private val networkClient: NetworkClient
) : SourceInterface.Base {
    override val id = "at_nu"
    override val nameStrId = R.string.source_name_at
    override val baseUrl = "https://a-t.nu/"

    override suspend fun getChapterTitle(doc: Document): String? = withContext(Dispatchers.Default) {
        doc.title().ifBlank { null }
    }

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        val raw = doc.selectFirst(".text-left style")!!.data()

        data class CSSData(val id: String, val type: String, val text: String)

        val cssData = """\.(\w+)::(before|after) \{content: \'(.+?)';\}"""
            .toRegex()
            .findAll(raw)
            .map {
                val (id, type, text) = it.destructured
                CSSData(id, type, text)
            }.groupBy {
                it.id
            }.mapValues { data -> data.value.associate { it.type to it.text } }

        doc.selectFirst("div.text-left")!!
            .apply {
                select("div.code-block.code-block-3").remove()
            }.select("p")
            .map {
                val spans = it.select("span")
                if (spans.isEmpty())
                    return@map it.text()

                spans.joinToString(" ") { span ->
                    val name = span.attr("class").trim()
                    listOf(
                        cssData[name]?.get("before") ?: "",
                        it.text(),
                        cssData[name]?.get("after")?.removePrefix("""\a0""") ?: "",
                    ).joinToString(" ") { text ->
                        text.removePrefix("""\a0""")
                    }
                }
            }.joinToString("\n\n")
    }
}