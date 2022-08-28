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

    // We set pos := index + 1 as the title is pos := 0
    return@withContext paragraphs
        .mapIndexed { index, paragraph ->
            async {
                generateITEM(
                    chapterUrl = chapterUrl,
                    chapterIndex = chapterPos,
                    chapterItemIndex = index + initialChapterItemIndex,
                    text = paragraph,
                    location = when (index) {
                        0 -> ReaderItem.LOCATION.FIRST
                        paragraphs.lastIndex -> ReaderItem.LOCATION.LAST
                        else -> ReaderItem.LOCATION.MIDDLE
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
    location: ReaderItem.LOCATION
): ReaderItem = when (val imgEntry = BookTextMapper.ImgEntry.fromXMLString(text)) {
    null -> ReaderItem.Body(
        chapterUrl = chapterUrl,
        chapterIndex = chapterIndex,
        chapterItemIndex = chapterItemIndex,
        text = text,
        location = location
    )
    else -> ReaderItem.Image(
        chapterUrl = chapterUrl,
        chapterIndex = chapterIndex,
        chapterItemIndex = chapterItemIndex,
        text = text,
        location = location,
        image = imgEntry
    )
}
