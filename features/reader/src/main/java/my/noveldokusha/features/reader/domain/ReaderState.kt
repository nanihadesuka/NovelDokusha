package my.noveldokusha.features.reader.domain

import kotlin.math.ceil

/**
 * Only use it on definitions where the primitive data type
 * doesn't convey enough meaning
 */
internal typealias ChapterUrl = String
internal typealias ItemIndex = Int // refers to [items]
internal typealias ChapterIndex = Int // refers to [orderedChapters]
internal typealias ChapterItemPosition = Int

internal enum class ReaderState {
    IDLE,
    LOADING,
    INITIAL_LOAD
}

internal data class ChapterState(
    val chapterUrl: String,
    val chapterItemPosition: Int,
    val offset: Int
)

internal data class ReadingChapterPosStats(
    val chapterIndex: Int,
    val chapterCount: Int,
    val chapterItemPosition: Int,
    val chapterItemsCount: Int,
    val chapterTitle: String,
    val chapterUrl: String,
)

internal fun ReadingChapterPosStats.chapterReadPercentage() = when (chapterItemsCount) {
    0 -> 100f
    else -> ceil((chapterItemPosition.toFloat() / chapterItemsCount.toFloat()) * 100f)
}
