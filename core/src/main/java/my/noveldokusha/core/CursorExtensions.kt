package my.noveldokusha.core

import android.database.Cursor

/**
 * Iterate a [Cursor] as a [Sequence], yielding one row at a time.
 *
 * The cursor is always closed — including when the consumer abandons the
 * sequence early (e.g. via `take(n)`, `first()`, or an exception in a
 * downstream operator). This is implemented with a try/finally inside the
 * sequence builder, which runs even if iteration is cut short by the
 * consumer no longer requesting the next item.
 *
 * Without this guarantee, callers like `AppLocalSources` that recursively
 * walk the SAF tree would leak open Cursor objects every time the user
 * cancelled a directory scan, eventually exhausting the ContentProvider's
 * open cursor budget.
 */
fun Cursor?.asSequence() = sequence {
    val cursor = this@asSequence ?: return@sequence
    try {
        while (cursor.moveToNext()) {
            yield(cursor)
        }
    } finally {
        cursor.close()
    }
}
