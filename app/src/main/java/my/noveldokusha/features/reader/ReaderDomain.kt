package my.noveldokusha.features.reader

import my.noveldokusha.tooling.local_database.tables.Chapter
import my.noveldokusha.features.reader.domain.ReadingChapterPosStats
import kotlin.math.ceil

data class ChapterStats(
    val itemsCount: Int,
    val chapter: Chapter,
    val orderedChaptersIndex: Int
)


fun ReadingChapterPosStats.chapterReadPercentage() = when (chapterItemsCount) {
    0 -> 100f
    else -> ceil((chapterItemPosition.toFloat() / chapterItemsCount.toFloat()) * 100f)
}