package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter

data class ItemPosition(val chapterIndex: Int, val chapterItemIndex: Int, val itemOffset: Int)

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapterIndex: Int,
    chapter: Chapter,
): ItemPosition = coroutineScope {
    val titleChapterItemIndex = 0
    val book = async { repository.bookLibrary.get(bookUrl) }
    val position = ItemPosition(
        chapterIndex = chapterIndex,
        chapterItemIndex = chapter.lastReadPosition,
        itemOffset = chapter.lastReadOffset,
    )

    when {
        chapter.url == book.await()?.lastReadChapter -> position
        chapter.read -> ItemPosition(
            chapterIndex = chapterIndex,
            chapterItemIndex = titleChapterItemIndex,
            itemOffset = 0,
        )
        else -> position
    }
}