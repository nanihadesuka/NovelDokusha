package my.noveldokusha.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import my.noveldokusha.data.database.DAOs.ChapterBodyDao
import my.noveldokusha.data.database.DAOs.ChapterDao
import my.noveldokusha.data.database.DAOs.LibraryDao
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import java.io.InputStream

@Database(
    entities = [
        Book::class,
        Chapter::class,
        ChapterBody::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
{
    abstract fun libraryDao(): LibraryDao
    abstract fun chapterDao(): ChapterDao
    abstract fun chapterBodyDao(): ChapterBodyDao

    companion object
    {
        fun createRoom(ctx: Context, name: String) = Room
            .databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*migrations())
            .build()

        fun createRoomFromStream(ctx: Context, name: String, inputStream: InputStream) = Room
            .databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*migrations())
            .createFromInputStream { inputStream }
            .build()
    }

}

private fun migrations() = arrayOf(
    migration(1, 2) {
        it.execSQL("ALTER TABLE Chapter ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    },
    migration(2, 3) {
        it.execSQL("ALTER TABLE Book ADD COLUMN inLibrary INTEGER NOT NULL DEFAULT 0")
        it.execSQL("UPDATE Book SET inLibrary = 1")
    }
)

private fun migration(vi: Int, vf: Int, migrate: (SupportSQLiteDatabase) -> Unit) = object : Migration(vi, vf)
{
    override fun migrate(database: SupportSQLiteDatabase) = migrate(database)
}
