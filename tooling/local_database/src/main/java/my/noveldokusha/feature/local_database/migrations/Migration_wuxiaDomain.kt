@file:Suppress("FunctionName")

package my.noveldokusha.feature.local_database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 8 → 9: the legacy `wuxia.blog` source (dead since 2025) has been
 * replaced with `wuxia.click`. The source ID stays the same ("wuxia") so the
 * source slot is preserved in the UI, but any Book / Chapter / ChapterBody
 * rows whose URLs still point at the dead domain must be repointed at the new
 * domain so the user's saved library entries remain reachable.
 *
 * The path structure on the new Next.js-based site differs from the old one,
 * so the migrated URLs may not resolve to the exact same page — but at least
 * the rows remain associated with the wuxia source (otherwise they would be
 * orphaned and the user would see "invalid source" errors). The user can
 * re-fetch the chapter list / chapter text from the new source.
 */
internal fun MigrationsList.wuxiaDomainChange_blog_to_click(
    it: SupportSQLiteDatabase,
) = this.websiteDomainChangeHelper(
    it,
    oldDomain = "wuxia.blog",
    newDomain = "wuxia.click",
)
