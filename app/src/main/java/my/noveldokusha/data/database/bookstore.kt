package my.noveldokusha.data.database

import android.content.Context
import my.noveldokusha.App
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.uiUtils.LiveEvent

object bookstore
{
    val db_context: Context by lazy { App.instance.applicationContext }
    val appDB by lazy {
        val name = "bookEntry"
        Repository(
            db = AppDatabase.createRoom(db_context, name),
            context = db_context,
            name = name
        )
    }

    val settings by lazy { appDB.settings }
    val bookLibrary by lazy { appDB.bookLibrary }
    val bookChapter by lazy { appDB.bookChapter }
    val bookChapterBody by lazy { appDB.bookChapterBody }
    val eventDataRestored = LiveEvent<Unit>()
}

