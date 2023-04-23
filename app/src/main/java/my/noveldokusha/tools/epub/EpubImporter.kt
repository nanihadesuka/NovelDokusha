package my.noveldokusha.tools.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.isContentUri
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.Repository
import timber.log.Timber
import java.io.File


suspend fun epubImageImporter(
    targetFile: File,
    imageData: ByteArray,
) = withContext(Dispatchers.IO) {
    targetFile.parentFile?.also { parent ->
        parent.mkdirs()
        if (parent.exists()) {
            targetFile.writeBytes(imageData)
        } else {
            Timber.e("epubCoverImporter: Failed to create folder ${parent.absolutePath}")
        }
    }
}

suspend fun epubImporter(
    storageFolderName: String,
    repository: Repository,
    appFileResolver: AppFileResolver,
    epub: EpubBook,
    addToLibrary: Boolean
): Unit = withContext(Dispatchers.IO) {
    val localBookUrl = appFileResolver.getLocalBookPath(storageFolderName)

    // First clean any previous entries from the book
    repository.bookChapters.chapters(localBookUrl)
        .map { it.url }
        .let { repository.chapterBody.removeRows(it) }
    repository.bookChapters.removeAllFromBook(localBookUrl)
    repository.libraryBooks.remove(localBookUrl)

    if (epub.coverImage != null) {
        epubImageImporter(
            targetFile = appFileResolver.getStorageBookCoverImageFile(storageFolderName),
            imageData = epub.coverImage.image
        )
    }

    // Insert new book data
    Book(
        title = storageFolderName,
        url = localBookUrl,
        coverImageUrl = appFileResolver.getLocalBookCoverPath(),
        inLibrary = addToLibrary
    ).let { repository.libraryBooks.insert(it) }

    epub.chapters.mapIndexed { i, chapter ->
        Chapter(
            title = chapter.title,
            url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
            bookUrl = localBookUrl,
            position = i
        )
    }.let { repository.bookChapters.insert(it) }

    epub.chapters.map { chapter ->
        ChapterBody(
            url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
            body = chapter.body
        )
    }.let { repository.chapterBody.insertReplace(it) }

    epub.images.map {
        async {
            epubImageImporter(
                targetFile = appFileResolver.getStorageBookImageFile(storageFolderName, it.absPath),
                imageData = it.image
            )
        }
    }.awaitAll()
}