package my.noveldoksuha.data

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.fileImporter
import my.noveldokusha.core.tryAsResponse
import my.noveldokusha.epub_tooling.epubParser
import my.noveldokusha.tooling.epub_parser.EpubBook
import my.noveldokusha.tooling.local_database.tables.Book
import my.noveldokusha.tooling.local_database.tables.Chapter
import my.noveldokusha.tooling.local_database.tables.ChapterBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubImporterRepository @Inject constructor(
    private val libraryBooks: LibraryBooksRepository,
    private val bookChapters: BookChaptersRepository,
    private val chapterBody: ChapterBodyRepository,
    private val appFileResolver: AppFileResolver,
    @ApplicationContext private val context: Context,
) {
    suspend fun importEpubFromContentUri(
        contentUri: String,
        bookTitle: String,
        addToLibrary: Boolean = false
    ) = tryAsResponse {
        val inputStream = context.contentResolver.openInputStream(contentUri.toUri())
            ?: return@tryAsResponse
        val epub = inputStream.use { epubParser(inputStream = inputStream) }
        epubImporter(
            storageFolderName = bookTitle,
            epub = epub,
            addToLibrary = addToLibrary
        )
    }

    suspend fun epubImporter(
        storageFolderName: String,
        epub: EpubBook,
        addToLibrary: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        val localBookUrl = appFileResolver.getLocalBookPath(storageFolderName)

        // First clean any previous entries from the book
        bookChapters.chapters(localBookUrl)
            .map { it.url }
            .let { chapterBody.removeRows(it) }
        bookChapters.removeAllFromBook(localBookUrl)
        libraryBooks.remove(localBookUrl)

        val coverImage = epub.coverImage
        if (coverImage != null) {
            fileImporter(
                targetFile = appFileResolver.getStorageBookCoverImageFile(storageFolderName),
                imageData = coverImage.image
            )
        }

        // Insert new book data
        Book(
            title = storageFolderName,
            url = localBookUrl,
            coverImageUrl = appFileResolver.getLocalBookCoverPath(),
            inLibrary = addToLibrary
        ).let { libraryBooks.insert(it) }

        epub.chapters.mapIndexed { i, chapter ->
            Chapter(
                title = chapter.title,
                url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
                bookUrl = localBookUrl,
                position = i
            )
        }.let { bookChapters.insert(it) }

        epub.chapters.map { chapter ->
            ChapterBody(
                url = appFileResolver.getLocalBookChapterPath(storageFolderName, chapter.absPath),
                body = chapter.body
            )
        }.let { chapterBody.insertReplace(it) }

        epub.images.map {
            async {
                fileImporter(
                    targetFile = appFileResolver.getStorageBookImageFile(
                        storageFolderName,
                        it.absPath
                    ),
                    imageData = it.image
                )
            }
        }.awaitAll()
    }
}