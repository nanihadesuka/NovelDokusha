package my.noveldokusha.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.*
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChapter

class Repository(
    private val db : AppDatabase,
    private val context : Context,
    val name : String
    )
{
    val settings = Settings()
    val bookLibrary = BookLibrary()
    val bookChapter = BookChapter()
    val bookChapterBody = BookChapterBody()

    suspend fun getDatabaseSizeBytes() = withContext(Dispatchers.IO) { context.getDatabasePath(name).length() }
    fun close() = db.close()
    fun delete() = context.deleteDatabase(name)
    fun clearAllTables() = db.clearAllTables()
    suspend fun vacuum() = withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("VACUUM") }

    suspend fun <T> withTransaction(fn: suspend () -> T) = db.withTransaction(fn)

    fun isValid(book: Book): Boolean = book.url.matches("""^(https?|local)://.*""".toRegex())
    fun isValid(chapter: Chapter): Boolean = chapter.url.matches("""^(https?|local)://.*""".toRegex())

    inner class Settings
    {
        suspend fun clearNonLibraryData() = withContext(Dispatchers.IO)
        {
            db.libraryDao().removeAllNonLibraryRows()
            db.chapterDao().removeAllNonLibraryRows()
            db.chapterBodyDao().removeAllNonChapterRows()
        }
    }

    inner class BookLibrary
    {
        val booksInLibraryFlow by lazy { db.libraryDao().booksInLibraryFlow() }
        val getBooksInLibraryWithContextFlow by lazy { db.libraryDao().getBooksInLibraryWithContextFlow() }
        fun existInLibraryFlow(url: String) = db.libraryDao().existInLibraryFlow(url)
        suspend fun insert(book: Book) = if (isValid(book)) db.libraryDao().insert(book) else Unit
        suspend fun insert(books: List<Book>) = db.libraryDao().insert(books.filter(::isValid))
        suspend fun insertReplace(books: List<Book>) = db.libraryDao().insertReplace(books.filter(::isValid))
        suspend fun remove(bookUrl: String) = db.libraryDao().remove(bookUrl)
        suspend fun remove(book: Book) = db.libraryDao().remove(book)
        suspend fun update(book: Book) = db.libraryDao().update(book)
        suspend fun get(url: String) = db.libraryDao().get(url)
        suspend fun getAll() = db.libraryDao().getAll()
        suspend fun getAllInLibrary() = db.libraryDao().getAllInLibrary()
        suspend fun existInLibrary(url: String) = db.libraryDao().existInLibrary(url)
        suspend fun toggleBookmark(bookMetadata: BookMetadata) = db.withTransaction {
            val book = get(bookMetadata.url)
            if (book == null)
                insert(Book(title = bookMetadata.title, url = bookMetadata.url, inLibrary = true))
            else
                update(book.copy(inLibrary = !book.inLibrary))
        }
    }

    inner class BookChapter
    {
        fun numberOfUnreadChaptersFlow(bookUrl: String) = db.chapterDao().numberOfUnreadChaptersFlow(bookUrl)
        suspend fun update(chapter: Chapter) = db.chapterDao().update(chapter)
        suspend fun get(url: String) = db.chapterDao().get(url)
        suspend fun hasChapters(bookUrl: String) = db.chapterDao().hasChapters(bookUrl)
        suspend fun getAll() = db.chapterDao().getAll()
        suspend fun updateTitle(url: String, title: String) = db.chapterDao().updateTitle(url, title)
        suspend fun setAsRead(chaptersUrl: List<String>) = chaptersUrl.chunked(500).forEach { db.chapterDao().setAsRead(it) }
        suspend fun setAsUnread(chaptersUrl: List<String>) = chaptersUrl.chunked(500).forEach { db.chapterDao().setAsUnread(it) }
        suspend fun insert(chapters: List<Chapter>) = db.chapterDao().insert(chapters.filter(::isValid))
        suspend fun insertReplace(chapters: List<Chapter>) = db.chapterDao().insertReplace(chapters.filter(::isValid))
        suspend fun removeAllFromBook(bookUrl: String) = db.chapterDao().removeAllFromBook(bookUrl)
        suspend fun chapters(bookUrl: String) = db.chapterDao().chapters(bookUrl)
        suspend fun getFirstChapter(bookUrl: String) = db.chapterDao().getFirstChapter(bookUrl)
        fun getChaptersWithContexFlow(bookUrl: String) = db.chapterDao().getChaptersWithContextFlow(bookUrl)
        suspend fun merge(newChapters: List<Chapter>, bookUrl: String)
        {
            val current = chapters(bookUrl).associateBy { it.url }.toMutableMap()
            for (chapter in newChapters)
                current.merge(chapter.url, chapter) { old, new -> old.copy(position = new.position) }
            insertReplace(current.values.toList())
        }

    }

    inner class BookChapterBody
    {
        suspend fun getAll() = db.chapterBodyDao().getAll()
        suspend fun insertReplace(chapterBodies: List<ChapterBody>) = db.chapterBodyDao().insertReplace(chapterBodies)
        suspend fun insertReplace(chapterBody: ChapterBody) = db.chapterBodyDao().insertReplace(chapterBody)
        suspend fun removeRows(chaptersUrl: List<String>) =
            chaptersUrl.chunked(500).forEach { db.chapterBodyDao().removeChapterRows(it) }

        suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = db.withTransaction {
            insertReplace(chapterBody)
            if (title != null)
                bookChapter.updateTitle(chapterBody.url, title)
        }

        suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String>
        {
            if (tryCache) db.chapterBodyDao().get(urlChapter)?.let {
                return@fetchBody Response.Success(it.body)
            }

            if (urlChapter.startsWith("local://"))
            {
                return Response.Error(
                    """
                    Unable to load chapter from url:
                    $urlChapter
                    
                    Source is local but chapter content missing.
                """.trimIndent()
                )
            }

            return when (val res = downloadChapter(urlChapter))
            {
                is Response.Success ->
                {
                    insertWithTitle(ChapterBody(url = urlChapter, body = res.data.body), res.data.title)
                    return Response.Success(res.data.body)
                }
                is Response.Error ->
                {
                    Response.Error(res.message)
                }
            }
        }

    }
}