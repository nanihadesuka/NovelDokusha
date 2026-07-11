package my.noveldokusha.feature.local_database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import my.noveldokusha.feature.local_database.migrations.MigrationsList
import my.noveldokusha.feature.local_database.migrations._1stKissNovelDomainChange_1_org
import my.noveldokusha.feature.local_database.migrations.readLightNovelDomainChange_1_today
import my.noveldokusha.feature.local_database.migrations.readLightNovelDomainChange_2_meme
import my.noveldokusha.feature.local_database.migrations.wuxiaDomainChange_blog_to_click

internal fun databaseMigrations() = arrayOf(
    migration(1) {
        it.execSQL("ALTER TABLE Chapter ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    },
    migration(2) {
        it.execSQL("ALTER TABLE Book ADD COLUMN inLibrary INTEGER NOT NULL DEFAULT 0")
        it.execSQL("UPDATE Book SET inLibrary = 1")
    },
    migration(3) {
        it.execSQL("ALTER TABLE Book ADD COLUMN coverImageUrl TEXT NOT NULL DEFAULT ''")
        it.execSQL("ALTER TABLE Book ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    },
    migration(4) {
        it.execSQL("ALTER TABLE Book ADD COLUMN lastReadEpochTimeMilli INTEGER NOT NULL DEFAULT 0")
    },
    migration(5, MigrationsList::readLightNovelDomainChange_1_today),
    migration(6, MigrationsList::readLightNovelDomainChange_2_meme),
    migration(7, MigrationsList::_1stKissNovelDomainChange_1_org),
    migration(8, MigrationsList::wuxiaDomainChange_blog_to_click),
    migration(9) {
        // Create ChapterTranslation table for cloud translation caching
        it.execSQL("""
            CREATE TABLE ChapterTranslation (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                chapterUrl TEXT NOT NULL,
                sourceLang TEXT NOT NULL,
                targetLang TEXT NOT NULL,
                translatedParagraphs TEXT NOT NULL,
                titleTranslation TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        it.execSQL("""
            CREATE UNIQUE INDEX index_ChapterTranslation_chapterUrl_sourceLang_targetLang
            ON ChapterTranslation(chapterUrl, sourceLang, targetLang)
        """.trimIndent())
    },
)

internal fun migration(vi: Int, migrate: (SupportSQLiteDatabase) -> Unit) =
    object : Migration(vi, vi + 1) {
        override fun migrate(db: SupportSQLiteDatabase) = migrate(db)
    }