package my.noveldokusha.ui.screens.reader

import my.noveldokusha.data.database.tables.Chapter

enum class ReaderState {
    IDLE,
    LOADING,
    INITIAL_LOAD
}

data class ChapterStats(
    val itemsCount: Int,
    val chapter: Chapter,
    val chapterIndex: Int
)

data class ChapterState(
    val chapterUrl: String,
    val chapterItemIndex: Int,
    val offset: Int
)

data class ReadingChapterPosStats(
    val chapterIndex: Int,
    val chapterItemIndex: Int,
    val chapterItemsCount: Int,
    val chapterTitle: String,
)