package my.noveldokusha.feature.local_database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.feature.local_database.DAOs.ChapterBodyDao
import my.noveldokusha.feature.local_database.DAOs.ChapterDao
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import my.noveldokusha.feature.local_database.tables.Book
import my.noveldokusha.feature.local_database.tables.Chapter
import my.noveldokusha.feature.local_database.tables.ChapterBody
import java.io.InputStream


interface AppDatabase {
    fun libraryDao(): LibraryDao
    fun chapterDao(): ChapterDao
    fun chapterBodyDao(): ChapterBodyDao
    val name: String

    fun closeDatabase()
    fun clearDatabase()
    suspend fun vacuum()

    /**
     * Execute the whole database calls as an atomic operation
     */
    suspend fun <T> transaction(block: suspend () -> T): T

    companion object {
        fun createRoom(ctx: Context, name: String): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .addMigrations(*databaseMigrations())
            .build()
            .also { it.name = name }

        fun createRoomFromStream(
            ctx: Context,
            name: String,
            inputStream: InputStream
        ): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .addMigrations(*databaseMigrations())
            .createFromInputStream { inputStream }
            .build()
            .also { it.name = name }
    }
}


@Database(
    entities = [
        Book::class,
        Chapter::class,
        ChapterBody::class
    ],
    version = 5,
    exportSchema = false
)
internal abstract class AppRoomDatabase : RoomDatabase(), AppDatabase {
    abstract override fun libraryDao(): LibraryDao
    abstract override fun chapterDao(): ChapterDao
    abstract override fun chapterBodyDao(): ChapterBodyDao

    override lateinit var name: String

    override suspend fun <T> transaction(block: suspend () -> T): T = withTransaction(block)

    override fun closeDatabase() {
        close()
    }

    override fun clearDatabase() {
        clearAllTables()
    }

    override suspend fun vacuum() {
        withContext(Dispatchers.IO) { openHelper.writableDatabase.execSQL("VACUUM") }
    }
}
