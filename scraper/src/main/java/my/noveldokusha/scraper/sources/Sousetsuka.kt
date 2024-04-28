package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.TextExtractor
import org.jsoup.nodes.Document

class Sousetsuka(
    private val networkClient: NetworkClient
) : SourceInterface.Base {
    override val id = "sousetsuka"
    override val nameStrId = R.string.source_name_sousetsuka
    override val baseUrl = "https://www.sousetsuka.com/"

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.title().ifBlank { null }
        }

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst(".post-body.entry-content")!!
            .let { TextExtractor.get(it) }
    }
}