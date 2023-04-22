package my.noveldokusha.tools.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.Repository
import timber.log.Timber
import java.nio.file.Paths

suspend fun epubCoverImporter(
    storageFolderName: String,
    appFileResolver: AppFileResolver,
    coverImage: EpubImage,
) = withContext(Dispatchers.IO) {
    val imgFile = appFileResolver.getLocalBookCoverFile(storageFolderName)
    imgFile.parentFile?.also { parent ->
        parent.mkdirs()
        if (parent.exists()) {
            imgFile.writeBytes(coverImage.image)
        } else {
            Timber.e("Failed to create folder ${parent.absolutePath}")
        }
    }
}

suspend fun epubImporter(
    storageFolderName: String,
    repository: Repository,
    epub: EpubBook,
    addToLibrary: Boolean
): Unit = withContext(Dispatchers.IO) {
    val databaseBookUrl = storageFolderName.addLocalPrefix()

    // First clean any previous entries from the book
    repository.bookChapters.chapters(databaseBookUrl)
        .map { it.url }
        .let { repository.chapterBody.removeRows(it) }
    repository.bookChapters.removeAllFromBook(databaseBookUrl)
    repository.libraryBooks.remove(databaseBookUrl)

    // Insert new book data
    Book(
        title = epub.title,
        url = databaseBookUrl,
        coverImageUrl = epub.coverImagePath.addLocalPrefix(),
        inLibrary = addToLibrary
    ).let { repository.libraryBooks.insert(it) }

    epub.chapters
        .mapIndexed { i, it ->
            Chapter(
                title = it.title,
                url = it.url.addLocalPrefix(),
                bookUrl = databaseBookUrl,
                position = i
            )
        }
        .let { repository.bookChapters.insert(it) }

    epub.chapters
        .map { ChapterBody(url = it.url.addLocalPrefix(), body = it.body) }
        .let { repository.chapterBody.insertReplace(it) }

    epub.images.map {
        async {
            val imgFile = Paths.get(
                repository.settings.folderBooks.path,
                storageFolderName,
                it.absoluteFilePath
            ).toFile()
            imgFile.parentFile?.also { parent ->
                parent.mkdirs()
                if (parent.exists()) {
                    imgFile.writeBytes(it.image)
                } else {
                    Timber.e("Failed to create folder ${parent.absolutePath}")
                }
            }
        }
    }.awaitAll()
}