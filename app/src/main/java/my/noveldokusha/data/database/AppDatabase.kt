package my.noveldokusha.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import my.noveldokusha.data.database.DAOs.ChapterBodyDao
import my.noveldokusha.data.database.DAOs.ChapterDao
import my.noveldokusha.data.database.DAOs.LibraryDao
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import java.io.InputStream


interface AppDatabaseOperations {
    /**
     * Execute the whole database calls as an atomic operation
     */
    suspend fun <T> transaction(block: suspend () -> T): T
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
abstract class AppDatabase : RoomDatabase(), AppDatabaseOperations {
    abstract fun libraryDao(): LibraryDao
    abstract fun chapterDao(): ChapterDao
    abstract fun chapterBodyDao(): ChapterBodyDao

    lateinit var name: String

    override suspend fun <T> transaction(block: suspend () -> T): T = withTransaction(block)

    companion object {
        fun createRoom(ctx: Context, name: String) = Room
            .databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*databaseMigrations())
            .build()
            .also { it.name = name }

        fun createRoomFromStream(ctx: Context, name: String, inputStream: InputStream) = Room
            .databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*databaseMigrations())
            .createFromInputStream { inputStream }
            .build()
            .also { it.name = name }
    }
}
