package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.screens.reader.ReaderItem

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapter: Chapter,
    items: ArrayList<ReaderItem>
): Pair<Int, Int> = coroutineScope {

    val book = async(Dispatchers.IO) { repository.bookLibrary.get(bookUrl) }
    val titlePos = async(Dispatchers.Default) {
        items.indexOfFirst { it is ReaderItem.TITLE }
    }
    val position = async(Dispatchers.Default) {
        items.indexOfFirst {
            it is ReaderItem.Position && it.pos == chapter.lastReadPosition
        }.let { index ->
            if (index == -1) Pair(titlePos.await(), 0)
            else Pair(index, chapter.lastReadOffset)
        }
    }

    when {
        chapter.url == book.await()?.lastReadChapter -> position.await()
        chapter.read -> Pair(titlePos.await(), 0)
        else -> position.await()
    }.let { Pair(it.first.coerceAtLeast(titlePos.await()), it.second) }
}