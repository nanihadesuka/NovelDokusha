package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.repository.Repository

data class InitialPositionChapter(
    val chapterIndex: Int,
    val chapterItemPosition: Int,
    val chapterItemOffset: Int
)

suspend fun getInitialChapterItemPosition(
    repository: Repository,
    bookUrl: String,
    chapterIndex: Int,
    chapter: Chapter,
): InitialPositionChapter = coroutineScope {
    val titleChapterItemPosition = 0 // Hardcode or no?
    val book = async { repository.libraryBooks.get(bookUrl) }
    val position = InitialPositionChapter(
        chapterIndex = chapterIndex,
        chapterItemPosition = chapter.lastReadPosition,
        chapterItemOffset = chapter.lastReadOffset,
    )

    when {
        chapter.url == book.await()?.lastReadChapter -> position
        chapter.read -> InitialPositionChapter(
            chapterIndex = chapterIndex,
            chapterItemPosition = titleChapterItemPosition,
            chapterItemOffset = 0,
        )
        else -> position
    }
}