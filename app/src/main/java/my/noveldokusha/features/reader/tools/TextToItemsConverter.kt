package my.noveldokusha.features.reader.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldokusha.features.reader.ReaderItem
import my.noveldokusha.tools.BookTextMapper

suspend fun textToItemsConverter(
    chapterUrl: String,
    chapterIndex: Int,
    chapterItemPositionDisplacement: Int,
    text: String
): List<ReaderItem> = withContext(Dispatchers.Default) {
    val paragraphs = text
        .splitToSequence("\n\n")
        .filter { it.isNotBlank() }
        .toList()

    return@withContext paragraphs
        .mapIndexed { position, paragraph ->
            async {
                generateITEM(
                    chapterUrl = chapterUrl,
                    chapterIndex = chapterIndex,
                    chapterItemPosition = position + chapterItemPositionDisplacement,
                    text = paragraph,
                    location = when (position) {
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
    chapterItemPosition: Int,
    text: String,
    location: ReaderItem.Location
): ReaderItem = when (val imgEntry = BookTextMapper.ImgEntry.fromXMLString(text)) {
    null -> ReaderItem.Body(
        chapterUrl = chapterUrl,
        chapterIndex = chapterIndex,
        chapterItemPosition = chapterItemPosition,
        text = text,
        location = location
    )
    else -> ReaderItem.Image(
        chapterUrl = chapterUrl,
        chapterIndex = chapterIndex,
        chapterItemPosition = chapterItemPosition,
        text = text,
        location = location,
        image = imgEntry
    )
}
