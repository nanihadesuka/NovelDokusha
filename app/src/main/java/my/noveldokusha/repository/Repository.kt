package my.noveldokusha.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.AppDatabase
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.folderBooks
import javax.inject.Inject

class Repository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context,
    val name: String,
    private val libraryBooksRepository: LibraryBooksRepository,
    private val bookChaptersRepository: BookChaptersRepository,
    private val chapterBodyRepository: ChapterBodyRepository,
) {

    val libraryBooks = libraryBooksRepository
    val bookChapters = bookChaptersRepository
    val chapterBody = chapterBodyRepository

    val settings = Settings()
    val eventDataRestored = MutableSharedFlow<Unit>()

    suspend fun getDatabaseSizeBytes() = withContext(Dispatchers.IO) {
        context.getDatabasePath(name).length()
    }

    fun close() = db.close()
    fun delete() = context.deleteDatabase(name)
    fun clearAllTables() = db.clearAllTables()
    suspend fun vacuum() =
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("VACUUM") }

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
        val folderBooks = context.folderBooks
    }

}

fun isValid(book: Book): Boolean = book.url.matches("""^(https?|local)://.*""".toRegex())
fun isValid(chapter: Chapter): Boolean =
    chapter.url.matches("""^(https?|local)://.*""".toRegex())