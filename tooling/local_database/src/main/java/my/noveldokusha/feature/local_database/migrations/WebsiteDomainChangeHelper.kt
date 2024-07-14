package my.noveldokusha.feature.local_database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import my.noveldokusha.tooling.local_database.migrations.MigrationsList

@Suppress("UnusedReceiverParameter")
internal fun MigrationsList.websiteDomainChangeHelper(
    it: SupportSQLiteDatabase,
    oldDomain: String,
    newDomain: String,
) {
    // readlightnovel source changed its domain to "newDomain"
    fun replace(columnName: String) =
        """SET $columnName = REPLACE($columnName, "$oldDomain", "$newDomain")"""

    fun like(columnName: String) =
        """($columnName LIKE "%$oldDomain%")"""
    it.execSQL(
        """
            UPDATE Book
                ${replace("url")},
                ${replace("coverImageUrl")},
            WHERE
                ${like("chapterUrl")};
        """.trimIndent()
    )
    it.execSQL(
        """
            UPDATE Chapter
                ${replace("url")},
                ${replace("bookUrl")},
            WHERE
                ${like("bookUrl")};
        """.trimIndent()
    )
    it.execSQL(
        """
            UPDATE ChapterBody
                ${replace("url")},
            WHERE
                ${like("url")};
        """.trimIndent()
    )
}