package my.noveldokusha.features.reader.domain

/**
 * Only use it on definitions where the primitive data type
 * doesn't convey enough meaning
 */
typealias ChapterUrl = String
typealias ItemIndex = Int // refers to [items]
typealias ChapterIndex = Int // refers to [orderedChapters]
typealias ChapterItemPosition = Int

enum class ReaderState {
    IDLE,
    LOADING,
    INITIAL_LOAD
}

data class ChapterState(
    val chapterUrl: String,
    val chapterItemPosition: Int,
    val offset: Int
)

data class ReadingChapterPosStats(
    val chapterIndex: Int,
    val chapterCount: Int,
    val chapterItemPosition: Int,
    val chapterItemsCount: Int,
    val chapterTitle: String,
    val chapterUrl: String,
)
