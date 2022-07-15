package my.noveldokusha.ui.screens.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.data.BookTextUtils

suspend fun textToItemsConverter(
    chapterUrl: String,
    text: String
): List<ReaderItem> = withContext(Dispatchers.Default) {
    val paragraphs = text
        .splitToSequence("\n\n")
        .filter { it.isNotBlank() }
        .toList()

    return@withContext paragraphs
        .mapIndexed { index, paragraph ->
            async {
                when (index) {
                    0 -> generateITEM(
                        chapterUrl,
                        index + 1,
                        paragraph,
                        ReaderItem.LOCATION.FIRST
                    )
                    paragraphs.lastIndex -> generateITEM(
                        chapterUrl,
                        index + 1,
                        paragraph,
                        ReaderItem.LOCATION.LAST
                    )
                    else -> generateITEM(
                        chapterUrl,
                        index + 1,
                        paragraph,
                        ReaderItem.LOCATION.MIDDLE
                    )
                }
            }
        }
        .awaitAll()
}

private fun generateITEM(
    chapterUrl: String,
    pos: Int,
    text: String,
    location: ReaderItem.LOCATION
): ReaderItem = when (val imgEntry = BookTextUtils.ImgEntry.fromXMLString(text)) {
    null -> ReaderItem.BODY(
        chapterUrl = chapterUrl,
        pos = pos,
        text = text,
        location = location
    )
    else -> ReaderItem.BODY_IMAGE(
        chapterUrl = chapterUrl,
        pos = pos,
        text = text,
        location = location,
        image = imgEntry
    )
}
