package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.repository.Repository

data class ItemPosition(val chapterPosition: Int, val chapterItemPosition: Int, val chapterItemOffset: Int)

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapterPosition: Int,
    chapter: Chapter,
): ItemPosition = coroutineScope {
    val titleChapterItemPosition = 0
    val book = async { repository.libraryBooks.get(bookUrl) }
    val position = ItemPosition(
        chapterPosition = chapterPosition,
        chapterItemPosition = chapter.lastReadPosition,
        chapterItemOffset = chapter.lastReadOffset,
    )

    when {
        chapter.url == book.await()?.lastReadChapter -> position
        chapter.read -> ItemPosition(
            chapterPosition = chapterPosition,
            chapterItemPosition = titleChapterItemPosition,
            chapterItemOffset = 0,
        )
        else -> position
    }
}