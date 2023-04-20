package my.noveldokusha

import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.repository.Repository
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
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

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
data class EpubImage(val path: String, val image: ByteArray)
data class EpubBook(
    val bookUrl: String,
    val title: String,
    val coverImagePath: String,
    val chapters: List<EpubChapter>,
    val images: List<EpubImage>
)

@Throws(Exception::class)
suspend fun createEpubBook(
    fileName: String,
    inputStream: InputStream
): EpubBook = withContext(Dispatchers.Default) {
    val zipFile = withContext(Dispatchers.IO) {
        ZipInputStream(inputStream).use { zipInputStream ->
            zipInputStream
                .entries()
                .filterNot { it.isDirectory }
                .associate { it.name to (it to zipInputStream.readBytes()) }
        }
    }

    val container =
        zipFile["META-INF/container.xml"]
            ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.second)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")

    val opfFile = zipFile[opfFilePath] ?: throw Exception(".opf file missing")

    val docuemnt =
        parseXMLFile(opfFile.second) ?: throw Exception(".opf file failed to parse data")
    val metadata =
        docuemnt.selectFirstTag("metadata")
            ?: throw Exception(".opf file metadata section missing")
    val manifest =
        docuemnt.selectFirstTag("manifest")
            ?: throw Exception(".opf file manifest section missing")
    val spine =
        docuemnt.selectFirstTag("spine") ?: throw Exception(".opf file spine section missing")

    val bookTitle = metadata.selectFirstChildTag("dc:title")?.textContent
        ?: throw Exception(".opf metadata title tag missing")
    val rootPath = File(opfFilePath).parentFile ?: File("")
    fun String.absPath() = File(rootPath, this).path.replace("""\""", "/").removePrefix("/")

    data class EpubManifestItem(
        val id: String,
        val href: String,
        val mediaType: String,
        val properties: String
    )

    val items = manifest.selectChildTag("item").map {
        EpubManifestItem(
            id = it.getAttribute("id"),
            href = it.getAttribute("href").decodedURL,
            mediaType = it.getAttribute("media-type"),
            properties = it.getAttribute("properties")
        )
    }.associateBy { it.id }

    val idRef = spine.selectChildTag("itemref").map { it.getAttribute("idref") }

    data class TempEpubChapter(
        val url: String,
        val title: String?,
        val body: String,
        val chapterIndex: Int,
    )

    var chapterIndex = 0
    val chapterExtensions = listOf("xhtml", "xml", "html").map { ".$it" }
    val chapters = idRef
        .mapNotNull { items[it] }
        .filter { item -> chapterExtensions.any { item.href.endsWith(it, ignoreCase = true) } }
        .mapNotNull { zipFile[it.href.absPath()] }
        .mapIndexed { index, (entry, byteArray) ->
            val res = EpubXMLFileParser(entry.name, byteArray, zipFile).parse()
            // A full chapter usually is split in multiple sequential entries,
            // try to merge them and extract the main title of each one.
            // Is is not perfect but better than dealing with a table of contents
            val chapterTitle = res.title ?: if (index == 0) bookTitle else null
            if (chapterTitle != null)
                chapterIndex += 1

            TempEpubChapter(
                url = "$fileName/${entry.name}",
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

    val listedImages = items.values.asSequence()
        .filter { it.mediaType.startsWith("image/") }
        .mapNotNull { zipFile[it.href.absPath()] }
        .map { (entry, byteArray) -> EpubImage(path = entry.name, image = byteArray) }

    val imageExtensions =
        listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp").map { ".$it" }
    val unlistedImages = zipFile.values.asSequence()
        .filterNot { (entry, _) -> entry.isDirectory }
        .filter { (entry, _) ->
            imageExtensions.any {
                entry.name.endsWith(
                    it,
                    ignoreCase = true
                )
            }
        }
        .map { (entry, byteArray) -> EpubImage(path = entry.name, image = byteArray) }

    val images = (listedImages + unlistedImages).distinctBy { it.path }

    val coverImage = items.values.asSequence()
        .filter { it.mediaType.startsWith("image/") }
        .find { it.properties == "cover-image" }
        ?.let { zipFile[it.href.absPath()] }
        ?.let { (entry, byteArray) -> EpubImage(path = entry.name, image = byteArray) }

    return@withContext EpubBook(
        bookUrl = fileName,
        title = bookTitle,
        coverImagePath = coverImage?.path ?: "",
        chapters = chapters.toList(),
        images = images.toList()
    )
}

fun String.addLocalPrefix() = "local://${this}"
fun String.replaceContentByLocal() = this.removePrefix("content://").addLocalPrefix()
fun String.urlIfContent(title: String) = if (isContentUri) title.addLocalPrefix() else this


fun importEpubToRepository(repository: Repository, epub: EpubBook, addToLibrary: Boolean) =
    CoroutineScope(Dispatchers.IO).launch {
        // First clean any previous entries from the book

        val bookUrl = epub.bookUrl.addLocalPrefix()
        repository.bookChapters.chapters(bookUrl)
            .map { it.url }
            .let { repository.chapterBody.removeRows(it) }
        repository.bookChapters.removeAllFromBook(bookUrl)
        repository.libraryBooks.remove(bookUrl)

        // Insert new book data
        Book(
            title = epub.title,
            url = bookUrl,
            coverImageUrl = epub.coverImagePath.addLocalPrefix(),
            inLibrary = addToLibrary
        ).let { repository.libraryBooks.insert(it) }

        epub.chapters
            .mapIndexed { i, it ->
                Chapter(
                    title = it.title,
                    url = it.url.addLocalPrefix(),
                    bookUrl = bookUrl,
                    position = i
                )
            }
            .let { repository.bookChapters.insert(it) }

        epub.chapters
            .map { ChapterBody(url = it.url.addLocalPrefix(), body = it.body) }
            .let { repository.chapterBody.insertReplace(it) }

        epub.images.map {
            async {
                val imgFile =
                    Paths.get(repository.settings.folderBooks.path, epub.bookUrl, it.path).toFile()
                imgFile.parentFile?.also { parent ->
                    parent.mkdirs()
                    if (parent.exists())
                        imgFile.writeBytes(it.image)
                }
            }
        }.awaitAll()
    }

private class EpubXMLFileParser(
    val fileAbsolutePath: String,
    val data: ByteArray,
    val zipFile: Map<String, Pair<ZipEntry, ByteArray>>
) {
    data class Output(val title: String?, val body: String)

    val fileParentFolder: File = File(fileAbsolutePath).parentFile ?: File("")

    // Make all local references absolute to the root of the epub for consistent references
    val absBasePath: String = File("").canonicalPath

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
        val absPath = File(fileParentFolder, relPathEncoded.decodedURL).canonicalPath
            .removePrefix(absBasePath)
            .replace("""\""", "/")
            .removePrefix("/")

        // Use run catching so it can be run locally without crash
        val bitmap = zipFile[absPath]?.second?.runCatching {
            BitmapFactory.decodeByteArray(this, 0, this.size)
        }?.getOrNull()

        val text = BookTextMapper.ImgEntry(
            path = absPath,
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
                child is TextNode -> child.text().trim()
                else -> getNodeTextTraverse(child)
            }
        }
    }
}
