package my.noveldokusha.utils

import android.database.Cursor

fun Cursor?.asSequence() = sequence<Cursor> {
    if (this@asSequence != null) {
        while (moveToNext()) {
            yield(this@asSequence)
        }
        this@asSequence.close()
    }
}