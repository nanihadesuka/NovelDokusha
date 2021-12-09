package my.noveldokusha.data.database.DAOs

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.BookWithContext

@Dao
interface LibraryDao
{
    @Query("SELECT * FROM Book")
    suspend fun getAll(): List<Book>

    @Query("SELECT * FROM Book WHERE inLibrary == 1")
    suspend fun getAllInLibrary(): List<Book>

    @Query("SELECT * FROM Book WHERE inLibrary == 1")
    fun booksInLibraryFlow(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: Book)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: List<Book>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(book: List<Book>)

    @Delete
    suspend fun remove(book: Book)

    @Query("DELETE FROM Book WHERE Book.url = :bookUrl")
    suspend fun remove(bookUrl: String)

    @Update
    suspend fun update(book: Book)

    @Query("UPDATE Book SET coverImageUrl = :coverUrl WHERE url == :bookUrl")
    suspend fun updateCover(bookUrl: String, coverUrl: String)

    @Query("UPDATE Book SET description = :description WHERE url == :bookUrl")
    suspend fun updateDescription(bookUrl: String, description: String)

    @Query("SELECT * FROM Book WHERE url = :url")
    suspend fun get(url: String): Book?

    @Query("SELECT * FROM Book WHERE url = :url")
    fun getFlow(url: String): Flow<Book?>

    @Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url AND inLibrary == 1)")
    suspend fun existInLibrary(url: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url AND inLibrary == 1)")
    fun existInLibraryFlow(url: String): Flow<Boolean>

    @Query(
        """
        SELECT Book.*, COUNT(Chapter.read) AS chaptersCount, SUM(Chapter.read) AS chaptersReadCount
        FROM Book
        LEFT JOIN Chapter ON Chapter.bookUrl = Book.url
        WHERE Book.inLibrary == 1
        GROUP BY Book.url
    """
    )
    fun getBooksInLibraryWithContextFlow(): Flow<List<BookWithContext>>

    @Query("DELETE FROM Book WHERE inLibrary == 0")
    suspend fun removeAllNonLibraryRows()

}