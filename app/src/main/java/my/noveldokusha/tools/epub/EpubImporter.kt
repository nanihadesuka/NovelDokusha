package my.noveldokusha.tools.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.utils.fileImporter

suspend fun epubImporter(
    storageFolderName: String,
    appRepository: AppRepository,
    appFileResolver: AppFileResolver,
    epub: EpubBook,
    addToLibrary: Boolean
): Unit = withContext(Dispatchers.IO) {
    val localBookUrl = appFileResolver.getLocalBookPath(storageFolderName)

    // First clean any previous entries from the book
    appRepository.bookChapters.chapters(localBookUrl)
        .map { it.url }
        .let { appRepository.chapterBody.removeRows(it) }
    appRepository.bookChapters.removeAllFromBook(localBookUrl)
    appRepository.libraryBooks.remove(localBookUrl)

    if (epub.coverImage != null) {
        fileImporter(
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
    ).let { appRepository.libraryBooks.insert(it) }

    epub.chapters.mapIndexed { i, chapter ->
        Chapter(
            title = chapter.title,
            url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
            bookUrl = localBookUrl,
            position = i
        )
    }.let { appRepository.bookChapters.insert(it) }

    epub.chapters.map { chapter ->
        ChapterBody(
            url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
            body = chapter.body
        )
    }.let { appRepository.chapterBody.insertReplace(it) }

    epub.images.map {
        async {
            fileImporter(
                targetFile = appFileResolver.getStorageBookImageFile(storageFolderName, it.absPath),
                imageData = it.image
            )
        }
    }.awaitAll()
}