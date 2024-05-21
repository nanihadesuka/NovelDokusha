package my.noveldoksuha.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.Response
import my.noveldokusha.core.isContentUri
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.tooling.local_database.tables.Book
import my.noveldokusha.tooling.local_database.tables.Chapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context,
    val libraryBooks: LibraryBooksRepository,
    val bookChapters: BookChaptersRepository,
    val chapterBody: ChapterBodyRepository,
    private val appFileResolver: AppFileResolver,
    private val epubImporterRepository: EpubImporterRepository
) {
    val settings = Settings()
    val eventDataRestored = MutableSharedFlow<Unit>()

    suspend fun toggleBookmark(bookUrl: String, bookTitle: String): Boolean {
        val realUrl = appFileResolver.getLocalIfContentType(bookUrl, bookFolderName = bookTitle)
        return if (bookUrl.isContentUri && libraryBooks.get(realUrl) == null) {
            epubImporterRepository.importEpubFromContentUri(
                contentUri = bookUrl,
                bookTitle = bookTitle,
                addToLibrary = true
            ) is Response.Success
        } else {
            libraryBooks.toggleBookmark(bookUrl = realUrl, bookTitle = bookTitle)
        }
    }

    suspend fun getDatabaseSizeBytes() = withContext(Dispatchers.IO) {
        context.getDatabasePath(db.name).length()
    }

    fun close() = db.closeDatabase()
    fun delete() = context.deleteDatabase(db.name)
    suspend fun vacuum() = db.vacuum()

    suspend fun <T> withTransaction(fn: suspend () -> T) = db.transaction(fn)

    inner class Settings {
        suspend fun clearNonLibraryData() = withContext(Dispatchers.IO)
        {
            db.libraryDao().removeAllNonLibraryRows()
            db.chapterDao().removeAllNonLibraryRows()
            db.chapterBodyDao().removeAllNonChapterRows()
        }

        /**
         * Folder where additional book data like images is stored.
         * Each subfolder must be an unique folder for each book.
         * Each book folder can have an arbitrary structure internally.
         */
        val folderBooks = appFileResolver.folderBooks
    }

}

fun isValid(book: Book): Boolean = book.url.matches("""^(https?|local)://.*""".toRegex())
fun isValid(chapter: Chapter): Boolean =
    chapter.url.matches("""^(https?|local)://.*""".toRegex())