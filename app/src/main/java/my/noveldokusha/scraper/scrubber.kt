package my.noveldokusha.scraper

import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.*
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object scrubber
{
    val databasesList = setOf<DatabaseInterface>(
        NovelUpdates(),
        BakaUpdates()
    )

    val sourcesList = setOf<SourceInterface>(
        LightNovelsTranslations(),
        ReadLightNovel(),
        ReadNovelFull(),
        my.noveldokusha.scraper.sources.NovelUpdates(),
        Reddit(),
        AT(),
        Wuxia(),
        BestLightNovel(),
        _1stKissNovel(),
        Sousetsuka(),
        Saikai(),
    )

    val sourcesListCatalog = sourcesList.filterIsInstance<SourceInterface.catalog>().toSet()
    val sourcesLanguages = sourcesList.filterIsInstance<SourceInterface.catalog>().map { it.language }.toSortedSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean
    {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? = sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleSourceCatalog(url: String): SourceInterface.catalog? = sourcesListCatalog.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
    fun getCompatibleDatabase(url: String): DatabaseInterface? = databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl)}

    fun getNodeStructuredText(node: Node): String
    {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when
            {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child is TextNode -> child.text().trim()
                else -> getNodeTextTraverse(child)
            }
        }
    }

    private fun getPTraverse(node: Node): String
    {
        fun innerTraverse(node: Node): String = node.childNodes().joinToString("") { child ->
            when
            {
                child.nodeName() == "br" -> "\n"
                child is TextNode -> child.text()
                else -> innerTraverse(child)
            }
        }

        val paragraph = innerTraverse(node).trim()
        return if (paragraph.isEmpty()) "" else innerTraverse(node).trim() + "\n\n"
    }

    private fun getNodeTextTraverse(node: Node): String
    {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when
            {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child is TextNode ->
                {
                    val text = child.text().trim()
                    if (text.isEmpty()) "" else text + "\n\n"
                }
                else -> getNodeTextTraverse(child)
            }
        }
    }
}
