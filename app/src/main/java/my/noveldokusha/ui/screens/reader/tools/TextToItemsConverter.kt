package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.tools.BookTextMapper
import my.noveldokusha.ui.screens.reader.ReaderItem

suspend fun textToItemsConverter(
    chapterUrl: String,
    chapterPos: Int,
    initialChapterItemIndex: Int,
    text: String
): List<ReaderItem> = withContext(Dispatchers.Default) {
    val paragraphs = text
        .splitToSequence("\n\n")
        .filter { it.isNotBlank() }
        .toList()

    return@withContext paragraphs
        .mapIndexed { index, paragraph ->
            async {
                generateITEM(
                    chapterUrl = chapterUrl,
                    chapterIndex = chapterPos,
                    chapterItemIndex = index + initialChapterItemIndex,
                    text = paragraph,
                    location = when (index) {
                        0 -> ReaderItem.Location.FIRST
                        paragraphs.lastIndex -> ReaderItem.Location.LAST
                        else -> ReaderItem.Location.MIDDLE
                    }
                )
            }
        }.awaitAll()
}

private fun generateITEM(
    chapterUrl: String,
    chapterIndex: Int,
    chapterItemIndex: Int,
    text: String,
    location: ReaderItem.Location
): ReaderItem = when (val imgEntry = BookTextMapper.ImgEntry.fromXMLString(text)) {
    null -> ReaderItem.Body(
        chapterUrl = chapterUrl,
        chapterPosition = chapterIndex,
        chapterItemPosition = chapterItemIndex,
        text = text,
        location = location
    )
    else -> ReaderItem.Image(
        chapterUrl = chapterUrl,
        chapterPosition = chapterIndex,
        chapterItemPosition = chapterItemIndex,
        text = text,
        location = location,
        image = imgEntry
    )
}
