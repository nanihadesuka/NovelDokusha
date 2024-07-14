@file:Suppress("FunctionName")

package my.noveldokusha.feature.local_database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

internal fun MigrationsList._1stKissNovelDomainChange_1_org(
    it: SupportSQLiteDatabase,
) = this.websiteDomainChangeHelper(
    it,
    oldDomain = "1stkissnovel.love",
    newDomain = "1stkissnovel.org",
)