package my.noveldokusha.epub_parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.invariantSeparatorsPathString

internal suspend fun getZipFiles(
    inputStream: InputStream
): Map<String, EpubFile> = withContext(Dispatchers.IO) {
    ZipInputStream(inputStream).use { zipInputStream ->
        zipInputStream
            .entries()
            .filterNot { it.isDirectory }
            .map { EpubFile(absPath = it.name, data = zipInputStream.readBytes()) }
            .associateBy { it.absPath }
    }
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
        val absPath: String,
        val mediaType: String,
        val properties: String
    )

    val manifestItems = manifest.selectChildTag("item").map {
        EpubManifestItem(
            id = it.getAttribute("id"),
            absPath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
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
                item.absPath.endsWith(it, ignoreCase = true)
            } || item.mediaType.startsWith("image/")
        }
        .mapNotNull { files[it.absPath]?.let { file -> it to file } }
        .mapIndexed { index, (item, file) ->
            val parser = EpubXMLFileParser(file.absPath, file.data, files)
            if (item.mediaType.startsWith("image/")) {
                TempEpubChapter(
                    url = "image_${file.absPath}",
                    title = null,
                    body = parser.parseAsImage(item.absPath),
                    chapterIndex = chapterIndex,
                )
            } else {
                val res = parser.parseAsDocument()
                // A full chapter usually is split in multiple sequential entries,
                // try to merge them and extract the main title of each one.
                // Is is not perfect but better than dealing with a table of contents
                val chapterTitle = res.title ?: if (index == 0) metadataTitle else null
                if (chapterTitle != null)
                    chapterIndex += 1

                TempEpubChapter(
                    url = file.absPath,
                    title = chapterTitle,
                    body = res.body,
                    chapterIndex = chapterIndex,
                )
            }
        }.groupBy {
            it.chapterIndex
        }.map { (index, list) ->
            EpubBook.Chapter(
                absPath = list.first().url,
                title = list.first().title ?: "Chapter $index",
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
            imageExtensions.any { file.absPath.endsWith(it, ignoreCase = true) }
        }
        .map { (_, file) ->
            EpubBook.Image(absPath = file.absPath, image = file.data)
        }

    val listedImages = manifestItems.asSequence()
        .map { it.value }
        .filter { it.mediaType.startsWith("image") }
        .mapNotNull { files[it.absPath] }
        .map { EpubBook.Image(absPath = it.absPath, image = it.data) }

    val images = (listedImages + unlistedImages).distinctBy { it.absPath }

    val coverImage = manifestItems[metadataCoverId]
        ?.let { files[it.absPath] }
        ?.let { EpubBook.Image(absPath = it.absPath, image = it.data) }

    return@withContext EpubBook(
        coverImage = coverImage,
        chapters = chapters.toList(),
        images = images.toList()
    )
}
