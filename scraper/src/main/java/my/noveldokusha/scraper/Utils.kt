package my.noveldokusha.scraper

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

internal fun Document.selectFirstTag(tag: String): Node? = getElementsByTagName(tag).item(0)
internal fun Node.getAttributeValue(attribute: String): String? =
    attributes?.getNamedItem(attribute)?.textContent

internal fun parseXMLText(text: String): Document? = text.reader().runCatching {
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(this))
}.getOrNull()