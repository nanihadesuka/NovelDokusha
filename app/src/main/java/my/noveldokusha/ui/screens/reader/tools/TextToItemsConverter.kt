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
                when (index) {
                    0 -> generateITEM(
                        chapterUrl = chapterUrl,
                        chapterPos = chapterPos,
                        pos = index + 1,
                        text = paragraph,
                        location = ReaderItem.LOCATION.FIRST
                    )
                    paragraphs.lastIndex -> generateITEM(
                        chapterUrl = chapterUrl,
                        chapterPos = chapterPos,
                        pos = index + 1,
                        text = paragraph,
                        location = ReaderItem.LOCATION.LAST
                    )
                    else -> generateITEM(
                        chapterUrl = chapterUrl,
                        chapterPos = chapterPos,
                        pos = index + 1,
                        text = paragraph,
                        location = ReaderItem.LOCATION.MIDDLE
                    )
                }
            }
        }
        .awaitAll()
}

private fun generateITEM(
    chapterUrl: String,
    chapterPos: Int,
    pos: Int,
    text: String,
    location: ReaderItem.LOCATION
): ReaderItem = when (val imgEntry = BookTextMapper.ImgEntry.fromXMLString(text)) {
    null -> ReaderItem.Body(
        chapterUrl = chapterUrl,
        chapterIndex = chapterPos,
        chapterItemIndex = pos,
        text = text,
        location = location
    )
    else -> ReaderItem.Image(
        chapterUrl = chapterUrl,
        chapterIndex = chapterPos,
        chapterItemIndex = pos,
        text = text,
        location = location,
        image = imgEntry
    )
}
