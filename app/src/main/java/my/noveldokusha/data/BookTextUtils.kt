package my.noveldokusha.data

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory

private fun Document.selectFirstTag(tag: String): Node? = getElementsByTagName(tag).item(0)
private fun Node.getAttributeValue(attribute: String): String? = attributes?.getNamedItem(attribute)?.textContent
private fun parseXMLText(text: String): Document? = text.reader().runCatching {
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(this))
}.getOrNull()

object BookTextUtils
{
    // <img yrel="{float}"> {uri} </img>
    data class ImgEntry(val path: String, val yrel: Float)
    {
        companion object
        {
            val XMLForm = """^\W*<img .*>.+</img>\W*$""".toRegex()

            fun fromXMLString(text: String): ImgEntry?
            {
                // Fast discard filter
                if (!text.matches(XMLForm))
                    return null

                return parseXMLText(text)?.selectFirstTag("img")?.let {
                    ImgEntry(
                        path = it.textContent ?: return null,
                        yrel = it.getAttributeValue("yrel")?.toFloatOrNull() ?: return null
                    )
                }
            }
        }

        fun toXMLString() : String
        {
            return """<img yrel="${"%.2f".format(yrel)}">$path</img>"""
        }
    }


}