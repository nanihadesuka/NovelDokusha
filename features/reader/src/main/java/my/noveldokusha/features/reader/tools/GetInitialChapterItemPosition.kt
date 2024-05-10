package my.noveldokusha.features.reader.tools

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import my.noveldoksuha.data.AppRepository
import my.noveldokusha.tooling.local_database.tables.Chapter

internal data class InitialPositionChapter(
    val chapterIndex: Int,
    val chapterItemPosition: Int,
    val chapterItemOffset: Int
)

internal suspend fun getInitialChapterItemPosition(
    appRepository: AppRepository,
    bookUrl: String,
    chapterIndex: Int,
    chapter: Chapter,
): InitialPositionChapter = coroutineScope {
    val titleChapterItemPosition = 0 // Hardcode or no?
    val book = async { appRepository.libraryBooks.get(bookUrl) }
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