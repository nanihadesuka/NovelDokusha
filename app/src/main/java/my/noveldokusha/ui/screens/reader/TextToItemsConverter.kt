package my.noveldokusha.ui.screens.reader

fun textToItemsConverter(chapterUrl: String, text: String): List<ReaderItem>
{
    val paragraphs = text
        .splitToSequence("\n\n")
        .filter { it.isNotBlank() }
        .withIndex().iterator()

    return sequence {
        for ((index, paragraph) in paragraphs)
        {
            val item = when
            {
                index == 0 -> ReaderItem.BODY(chapterUrl, index + 1, paragraph, ReaderItem.LOCATION.FIRST)
                !paragraphs.hasNext() -> ReaderItem.BODY(chapterUrl, index + 1, paragraph, ReaderItem.LOCATION.LAST)
                else -> ReaderItem.BODY(chapterUrl, index + 1, paragraph, ReaderItem.LOCATION.MIDDLE)
            }
            yield(item)
        }
    }.toList()
}