package my.noveldokusha.tooling.local_database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import my.noveldokusha.tooling.local_database.migrations.MigrationsList
import my.noveldokusha.tooling.local_database.migrations.readLightNovelDomainChange_1_today
import my.noveldokusha.tooling.local_database.migrations.readLightNovelDomainChange_2_meme

internal fun databaseMigrations() = arrayOf(
    migration(1, 2) {
        it.execSQL("ALTER TABLE Chapter ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    },
    migration(2, 3) {
        it.execSQL("ALTER TABLE Book ADD COLUMN inLibrary INTEGER NOT NULL DEFAULT 0")
        it.execSQL("UPDATE Book SET inLibrary = 1")
    },
    migration(3, 4) {
        it.execSQL("ALTER TABLE Book ADD COLUMN coverImageUrl TEXT NOT NULL DEFAULT ''")
        it.execSQL("ALTER TABLE Book ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    },
    migration(4, 5) {
        it.execSQL("ALTER TABLE Book ADD COLUMN lastReadEpochTimeMilli INTEGER NOT NULL DEFAULT 0")
    },
    migration(5, 6, MigrationsList::readLightNovelDomainChange_1_today),
    migration(6, 7, MigrationsList::readLightNovelDomainChange_2_meme)
)

internal fun migration(vi: Int, vf: Int, migrate: (SupportSQLiteDatabase) -> Unit) =
    object : Migration(vi, vf) {
        override fun migrate(database: SupportSQLiteDatabase) = migrate(database)
    }