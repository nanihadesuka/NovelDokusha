package my.noveldoksuha.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.tooling.local_database.AppDatabase
import my.noveldokusha.tooling.local_database.DAOs.LibraryDao
import my.noveldokusha.tooling.local_database.tables.Book
import my.noveldokusha.utils.fileImporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryBooksRepository @Inject constructor(
    private val libraryDao: LibraryDao,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context,
    private val appFileResolver: AppFileResolver,
    private val appCoroutineScope: AppCoroutineScope,
) {
    val getBooksInLibraryWithContextFlow by lazy {
        libraryDao.getBooksInLibraryWithContextFlow()
    }

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
    suspend fun toggleBookmark(
        bookUrl: String,
        bookTitle: String
    ): Boolean = appDatabase.transaction {
        when (val book = get(bookUrl)) {
            null -> {
                insert(Book(title = bookTitle, url = bookUrl, inLibrary = true))
                true
            }
            else -> {
                update(book.copy(inLibrary = !book.inLibrary))
                !book.inLibrary
            }
        }
    }

    fun saveImageAsCover(imageUri: Uri, bookUrl: String) {
        appCoroutineScope.launch {
            val imageData = context.contentResolver.openInputStream(imageUri)
                ?.use { it.readBytes() } ?: return@launch
            val bookFolderName = appFileResolver.getLocalBookFolderName(
                bookUrl = bookUrl
            )
            val bookCoverFile = appFileResolver.getStorageBookCoverImageFile(
                bookFolderName = bookFolderName
            )
            fileImporter(targetFile = bookCoverFile, imageData = imageData)
            delay(timeMillis = 1_000)
            updateCover(bookUrl = bookUrl, coverUrl = appFileResolver.getLocalBookCoverPath())
        }
    }
}