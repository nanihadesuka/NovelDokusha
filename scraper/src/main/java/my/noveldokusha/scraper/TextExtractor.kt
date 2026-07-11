package my.noveldokusha.scraper

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object TextExtractor {
    /**
     * Given an node, extract the text.
     *
     * @return string where each paragraph is
     * separated by two newlines
     */
    fun get(node: Node?): String {
        val children = node?.childNodes() ?: return ""
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child is TextNode -> child.text().trim()
                else -> getNodeTextTraverse(child)
            }
        }
    }

    private fun getPTraverse(node: Node): String {
        fun innerTraverse(node: Node): String = node.childNodes().joinToString("") { child ->
            when {
                child.nodeName() == "br" -> "\n"
                child is TextNode -> child.text()
                child.nodeName() == "img" -> declareImgEntry(child)
                else -> innerTraverse(child)
            }
        }

        val paragraph = innerTraverse(node).trim()
        return if (paragraph.isEmpty()) "" else innerTraverse(node).trim() + "\n\n"
    }

    private fun getNodeTextTraverse(node: Node): String {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child is TextNode -> {
                    val text = child.text().trim()
                    if (text.isEmpty()) "" else text + "\n\n"
                }
                else -> getNodeTextTraverse(child)
            }
        }
    }

    // Rewrites the image node to xml for the next stage.
    private fun declareImgEntry(node: Node): String {
        val relPathEncoded = (node as? Element)?.attr("src") ?: return ""
        val text = BookTextMapper.ImgEntry(
            path = relPathEncoded,
            yRel = 1.45f
        ).toXMLString()
        return "\n\n$text\n\n"
    }
}