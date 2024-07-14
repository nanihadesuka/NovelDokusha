package my.noveldokusha.core

import android.database.Cursor

fun Cursor?.asSequence() = sequence {
    if (this@asSequence != null) {
        while (moveToNext()) {
            yield(this@asSequence)
        }
        this@asSequence.close()
    }
}