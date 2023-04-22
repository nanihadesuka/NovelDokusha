package my.noveldokusha.tools.epub

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.isContentUri
import my.noveldokusha.tools.BookTextMapper
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.invariantSeparatorsPathString

private val NodeList.elements get() = (0..length).asSequence().mapNotNull { item(it) as? Element }
private val Node.childElements get() = childNodes.elements
private fun Document.selectFirstTag(tag: String): Node? = getElementsByTagName(tag).item(0)
private fun Node.selectFirstChildTag(tag: String) = childElements.find { it.tagName == tag }
private fun Node.selectChildTag(tag: String) = childElements.filter { it.tagName == tag }
private fun Node.getAttributeValue(attribute: String): String? =
    attributes?.getNamedItem(attribute)?.textContent

private fun parseXMLFile(inputSteam: InputStream): Document? =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSteam)

private fun parseXMLFile(byteArray: ByteArray): Document? = parseXMLFile(byteArray.inputStream())
private fun parseXMLText(text: String): Document? = text.reader().runCatching {
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(this))
}.getOrNull()

private val String.decodedURL: String get() = URLDecoder.decode(this, "UTF-8")
private fun String.asFileName(): String = this.replace("/", "_")

private fun ZipInputStream.entries() = generateSequence { nextEntry }

data class EpubChapter(val url: String, val title: String, val body: String)
data class EpubImage(val absoluteFilePath: String, val image: ByteArray)
data class EpubBook(
    val title: String,
    val coverImagePath: String,
    val chapters: List<EpubChapter>,
    val images: List<EpubImage>
)

data class EpubFile(
    val absoluteFilePath: String,
    val data: ByteArray,
)

private suspend fun getZipFiles(
    inputStream: InputStream
): Map<String, EpubFile> = withContext(Dispatchers.IO) {
    ZipInputStream(inputStream).use { zipInputStream ->
        zipInputStream
            .entries()
            .filterNot { it.isDirectory }
            .map { EpubFile(absoluteFilePath = it.name, data = zipInputStream.readBytes()) }
            .associateBy { it.absoluteFilePath }
    }
}

@Throws(Exception::class)
suspend fun epubCoverParser(
    inputStream: InputStream
): EpubImage? = withContext(Dispatchers.Default) {
    val files = getZipFiles(inputStream)

    val container = files["META-INF/container.xml"]
        ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.data)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")

    val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

    val document = parseXMLFile(opfFile.data)
        ?: throw Exception(".opf file failed to parse data")
    val metadata = document.selectFirstTag("metadata")
        ?: throw Exception(".opf file metadata section missing")
    val manifest = document.selectFirstTag("manifest")
        ?: throw Exception(".opf file manifest section missing")

    val metadataCoverId = metadata
        .selectChildTag("meta")
        .find { it.getAttributeValue("name") == "cover" }
        ?.getAttributeValue("content")

    val hrefRootPath = File(opfFilePath).parentFile ?: File("")
    fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
        .toPath()
        .invariantSeparatorsPathString
        .removePrefix("/")

    data class EpubManifestItem(
        val id: String,
        val absoluteFilePath: String,
        val mediaType: String,
        val properties: String
    )

    val manifestItems = manifest
        .selectChildTag("item").map {
            EpubManifestItem(
                id = it.getAttribute("id"),
                absoluteFilePath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                mediaType = it.getAttribute("media-type"),
                properties = it.getAttribute("properties")
            )
        }.associateBy { it.id }

    manifestItems[metadataCoverId]
        ?.let { files[it.absoluteFilePath] }
        ?.let { EpubImage(absoluteFilePath = it.absoluteFilePath, image = it.data) }
}

@Throws(Exception::class)
suspend fun epubParser(
    inputStream: InputStream
): EpubBook = withContext(Dispatchers.Default) {
    val files = getZipFiles(inputStream)

    val container = files["META-INF/container.xml"]
        ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.data)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")

    val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

    val document = parseXMLFile(opfFile.data)
        ?: throw Exception(".opf file failed to parse data")
    val metadata = document.selectFirstTag("metadata")
        ?: throw Exception(".opf file metadata section missing")
    val manifest = document.selectFirstTag("manifest")
        ?: throw Exception(".opf file manifest section missing")
    val spine = document.selectFirstTag("spine")
        ?: throw Exception(".opf file spine section missing")

    val metadataTitle = metadata.selectFirstChildTag("dc:title")?.textContent
        ?: throw Exception(".opf metadata title tag missing")

    val metadataCoverId = metadata
        .selectChildTag("meta")
        .find { it.getAttributeValue("name") == "cover" }
        ?.getAttributeValue("content")

    val hrefRootPath = File(opfFilePath).parentFile ?: File("")
    fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
        .toPath()
        .invariantSeparatorsPathString
        .removePrefix("/")

    data class EpubManifestItem(
        val id: String,
        val absoluteFilePath: String,
        val mediaType: String,
        val properties: String
    )

    val manifestItems = manifest.selectChildTag("item").map {
        EpubManifestItem(
            id = it.getAttribute("id"),
            absoluteFilePath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
            mediaType = it.getAttribute("media-type"),
            properties = it.getAttribute("properties")
        )
    }.associateBy { it.id }

    data class TempEpubChapter(
        val url: String,
        val title: String?,
        val body: String,
        val chapterIndex: Int,
    )

    var chapterIndex = 0
    val chapterExtensions = listOf("xhtml", "xml", "html").map { ".$it" }
    val chapters = spine
        .selectChildTag("itemref")
        .mapNotNull { manifestItems[it.getAttribute("idref")] }
        .filter { item ->
            chapterExtensions.any {
                item.absoluteFilePath.endsWith(
                    it,
                    ignoreCase = true
                )
            }
        }
        .mapNotNull { files[it.absoluteFilePath] }
        .mapIndexed { index, file ->
            val res = EpubXMLFileParser(file.absoluteFilePath, file.data, files).parse()
            // A full chapter usually is split in multiple sequential entries,
            // try to merge them and extract the main title of each one.
            // Is is not perfect but better than dealing with a table of contents
            val chapterTitle = res.title ?: if (index == 0) metadataTitle else null
            if (chapterTitle != null)
                chapterIndex += 1

            TempEpubChapter(
                url = file.absoluteFilePath,
                title = chapterTitle,
                body = res.body,
                chapterIndex = chapterIndex,
            )
        }.groupBy {
            it.chapterIndex
        }.map { (_, list) ->
            EpubChapter(
                url = list.first().url,
                title = list.first().title!!,
                body = list.joinToString("\n\n") { it.body }
            )
        }.filter {
            it.body.isNotBlank()
        }

    val imageExtensions =
        listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg").map { ".$it" }
    val unlistedImages = files
        .asSequence()
        .filter { (_, file) ->
            imageExtensions.any { file.absoluteFilePath.endsWith(it, ignoreCase = true) }
        }
        .map { (_, file) ->
            EpubImage(absoluteFilePath = file.absoluteFilePath, image = file.data)
        }

    val listedImages = manifestItems.asSequence()
        .map { it.value }
        .filter { it.mediaType.startsWith("image") }
        .mapNotNull { files[it.absoluteFilePath] }
        .map { EpubImage(absoluteFilePath = it.absoluteFilePath, image = it.data) }

    val images = (listedImages + unlistedImages).distinctBy { it.absoluteFilePath }

    return@withContext EpubBook(
        title = metadataTitle,
        coverImagePath = manifestItems[metadataCoverId]?.absoluteFilePath ?: "",
        chapters = chapters.toList(),
        images = images.toList()
    )
}

fun String.addLocalPrefix() = "local://${this}"
fun String.replaceContentByLocal() = this.removePrefix("content://").addLocalPrefix()
fun String.urlIfContent(title: String) = if (isContentUri) title.addLocalPrefix() else this

typealias FullFilepath = String

private class EpubXMLFileParser(
    val fileAbsolutePath: String,
    val data: ByteArray,
    val zipFile: Map<FullFilepath, EpubFile>
) {
    data class Output(val title: String?, val body: String)

    val fileParentFolder: File = File(fileAbsolutePath).parentFile ?: File("")


    fun parse(): Output {
        val body = Jsoup.parse(data.inputStream(), "UTF-8", "").body()

        val title = body.selectFirst("h1, h2, h3, h4, h5, h6")?.text()
        body.selectFirst("h1, h2, h3, h4, h5, h6")?.remove()

        return Output(
            title = title,
            body = getNodeStructuredText(body)
        )
    }

    // Rewrites the image node to xml for the next stage.
    private fun declareImgEntry(node: org.jsoup.nodes.Node): String {
        val relPathEncoded = (node as? org.jsoup.nodes.Element)?.attr("src") ?: return ""
        val absolutePathImage = File(fileParentFolder, relPathEncoded.decodedURL)
            .canonicalFile
            .toPath()
            .invariantSeparatorsPathString
            .removePrefix("/")

        // Use run catching so it can be run locally without crash
        val bitmap = zipFile[absolutePathImage]?.data?.runCatching {
            BitmapFactory.decodeByteArray(this, 0, this.size)
        }?.getOrNull()

        val text = BookTextMapper.ImgEntry(
            path = absolutePathImage,
            yrel = bitmap?.let { it.height.toFloat() / it.width.toFloat() } ?: 1.45f
        ).toXMLString()

        return "\n\n$text\n\n"
    }

    private fun getPTraverse(node: org.jsoup.nodes.Node): String {
        fun innerTraverse(node: org.jsoup.nodes.Node): String =
            node.childNodes().joinToString("") { child ->
                when {
                    child.nodeName() == "br" -> "\n"
                    child.nodeName() == "img" -> declareImgEntry(child)
                    child.nodeName() == "image" -> declareImgEntry(child)
                    child is TextNode -> child.text()
                    else -> innerTraverse(child)
                }
            }

        val paragraph = innerTraverse(node).trim()
        return if (paragraph.isEmpty()) "" else innerTraverse(node).trim() + "\n\n"
    }

    private fun getNodeTextTraverse(node: org.jsoup.nodes.Node): String {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child.nodeName() == "image" -> declareImgEntry(child)
                child is TextNode -> {
                    val text = child.text().trim()
                    if (text.isEmpty()) "" else text + "\n\n"
                }
                else -> getNodeTextTraverse(child)
            }
        }
    }

    private fun getNodeStructuredText(node: org.jsoup.nodes.Node): String {
        val children = node.childNodes()
        if (children.isEmpty())
            return ""

        return children.joinToString("") { child ->
            when {
                child.nodeName() == "p" -> getPTraverse(child)
                child.nodeName() == "br" -> "\n"
                child.nodeName() == "hr" -> "\n\n"
                child.nodeName() == "img" -> declareImgEntry(child)
                child.nodeName() == "image" -> declareImgEntry(child)
                child is TextNode -> child.text().trim()
                else -> getNodeTextTraverse(child)
            }
        }
    }
}
