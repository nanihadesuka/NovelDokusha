package my.noveldokusha.data.database.DAOs

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.database.tables.Chapter

@Dao
interface ChapterDao {
    @Query("SELECT * FROM Chapter")
    suspend fun getAll(): List<Chapter>

    @Query("SELECT * FROM Chapter WHERE bookUrl = :bookUrl")
    suspend fun chapters(bookUrl: String): List<Chapter>

    @Update
    suspend fun update(chapter: Chapter)

    @Query("SELECT EXISTS(SELECT * FROM Chapter WHERE Chapter.bookUrl = :bookUrl LIMIT 1)")
    suspend fun hasChapters(bookUrl: String): Boolean

    @Query(
        """
        SELECT * FROM Chapter
        WHERE Chapter.bookUrl = :bookUrl
        ORDER BY Chapter.position ASC
        LIMIT 1
    """
    )
    suspend fun getFirstChapter(bookUrl: String): Chapter?

    @Query("UPDATE Chapter SET read = 1 WHERE url in (:chaptersUrl)")
    suspend fun setAsRead(chaptersUrl: List<String>)

    @Query("UPDATE Chapter SET title = :title WHERE url == :url")
    suspend fun updateTitle(url: String, title: String)

    @Query("UPDATE Chapter SET read = 0 WHERE url in (:chaptersUrl)")
    suspend fun setAsUnread(chaptersUrl: List<String>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(chapters: List<Chapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(chapters: List<Chapter>)

    @Query("SELECT * FROM Chapter WHERE url = :url")
    suspend fun get(url: String): Chapter?

    @Query("SELECT COUNT(*) FROM Chapter WHERE bookUrl = :bookUrl AND read = 0")
    fun numberOfUnreadChaptersFlow(bookUrl: String): Flow<Int>

    @Query("DELETE FROM Chapter WHERE Chapter.bookUrl = :bookUrl")
    suspend fun removeAllFromBook(bookUrl: String)

    @Query("DELETE FROM Chapter WHERE Chapter.bookUrl NOT IN (SELECT Book.url FROM Book)")
    suspend fun removeAllNonLibraryRows()

    @Query(
        """
        SELECT Chapter.*, ChapterBody.url IS NOT NULL AS downloaded , Book.lastReadChapter IS NOT NULL AS lastReadChapter
        FROM Chapter
        LEFT JOIN ChapterBody ON ChapterBody.url = Chapter.url
        LEFT JOIN Book ON Book.url = :bookUrl AND Book.lastReadChapter == Chapter.url
        WHERE Chapter.bookUrl == :bookUrl
        ORDER BY position ASC
    """
    )
    fun getChaptersWithContextFlow(bookUrl: String): Flow<List<ChapterWithContext>>
}