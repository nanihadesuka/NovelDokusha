package my.noveldokusha.repository

import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.database.AppDatabaseOperations
import my.noveldokusha.data.database.DAOs.LibraryDao
import my.noveldokusha.data.database.tables.Book

class LibraryBooksRepository(
    private val libraryDao: LibraryDao,
    private val operations: AppDatabaseOperations,
) {
    val getBooksInLibraryWithContextFlow by lazy {
        libraryDao.getBooksInLibraryWithContextFlow()
    }

    fun existInLibraryFlow(url: String) = libraryDao.existInLibraryFlow(url)
    fun getFlow(url: String) = libraryDao.getFlow(url)
    suspend fun insert(book: Book) = if (isValid(book)) libraryDao.insert(book) else Unit
    suspend fun insert(books: List<Book>) = libraryDao.insert(books.filter(::isValid))
    suspend fun insertReplace(books: List<Book>) =
        libraryDao.insertReplace(books.filter(::isValid))

    suspend fun remove(bookUrl: String) = libraryDao.remove(bookUrl)
    suspend fun remove(book: Book) = libraryDao.remove(book)
    suspend fun update(book: Book) = libraryDao.update(book)
    suspend fun updateLastReadEpochTimeMilli(bookUrl: String, lastReadEpochTimeMilli: Long) =
        libraryDao.updateLastReadEpochTimeMilli(bookUrl, lastReadEpochTimeMilli)

    suspend fun updateCover(bookUrl: String, coverUrl: String) =
        libraryDao.updateCover(bookUrl, coverUrl)

    suspend fun updateDescription(bookUrl: String, description: String) =
        libraryDao.updateDescription(bookUrl, description)

    suspend fun get(url: String) = libraryDao.get(url)

    suspend fun updateLastReadChapter(bookUrl: String, lastReadChapterUrl: String) =
        libraryDao.updateLastReadChapter(
            bookUrl = bookUrl,
            chapterUrl = lastReadChapterUrl
        )

    suspend fun getAll() = libraryDao.getAll()
    suspend fun getAllInLibrary() = libraryDao.getAllInLibrary()
    suspend fun existInLibrary(url: String) = libraryDao.existInLibrary(url)
    suspend fun toggleBookmark(bookMetadata: BookMetadata) = operations.transaction {
        val book = get(bookMetadata.url)
        if (book == null)
            insert(Book(title = bookMetadata.title, url = bookMetadata.url, inLibrary = true))
        else
            update(book.copy(inLibrary = !book.inLibrary))
    }
}