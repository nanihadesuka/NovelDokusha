package my.noveldokusha.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import my.noveldokusha.feature.local_database.tables.ChapterBody

@Dao
interface ChapterBodyDao {
    @Query("SELECT * FROM ChapterBody")
    suspend fun getAll(): List<ChapterBody>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(chapterBody: ChapterBody)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(chapterBody: List<ChapterBody>)

    @Query("SELECT * FROM ChapterBody WHERE url = :url")
    suspend fun get(url: String): ChapterBody?

    @Query("DELETE FROM ChapterBody WHERE ChapterBody.url NOT IN (SELECT Chapter.url FROM Chapter)")
    suspend fun removeAllNonChapterRows()

    @Query("DELETE FROM ChapterBody WHERE ChapterBody.url IN (:chaptersUrl)")
    suspend fun removeChapterRows(chaptersUrl: List<String>)
}