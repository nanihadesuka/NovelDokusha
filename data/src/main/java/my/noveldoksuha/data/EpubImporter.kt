package my.noveldoksuha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.fileImporter
import my.noveldokusha.tooling.epub_parser.EpubBook
import my.noveldokusha.tooling.local_database.tables.ChapterBody

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

    val coverImage = epub.coverImage
    if (coverImage != null) {
        fileImporter(
            targetFile = appFileResolver.getStorageBookCoverImageFile(storageFolderName),
            imageData = coverImage.image
        )
    }

    // Insert new book data
    my.noveldokusha.tooling.local_database.tables.Book(
        title = storageFolderName,
        url = localBookUrl,
        coverImageUrl = appFileResolver.getLocalBookCoverPath(),
        inLibrary = addToLibrary
    ).let { appRepository.libraryBooks.insert(it) }

    epub.chapters.mapIndexed { i, chapter ->
        my.noveldokusha.tooling.local_database.tables.Chapter(
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