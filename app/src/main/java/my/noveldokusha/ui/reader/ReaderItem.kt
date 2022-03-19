package my.noveldokusha.ui.reader

import my.noveldokusha.data.BookTextUtils

sealed class ReaderItem
{
    abstract val chapterUrl: String

    interface Position
    {
        val pos: Int
    }

    enum class LOCATION
    { FIRST, MIDDLE, LAST }

    data class TITLE(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String
    ) : ReaderItem(), Position

    data class BODY(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String,
        val location: LOCATION
    ) : ReaderItem(), Position
    {
        val image by lazy { BookTextUtils.ImgEntry.fromXMLString(text) }
        val isImage by lazy { image != null }
    }

    class PROGRESSBAR(override val chapterUrl: String) : ReaderItem()
    class DIVIDER(override val chapterUrl: String) : ReaderItem()
    class BOOK_END(override val chapterUrl: String) : ReaderItem()
    class BOOK_START(override val chapterUrl: String) : ReaderItem()
    class ERROR(override val chapterUrl: String, val text: String) : ReaderItem()
    class PADDING(override val chapterUrl: String) : ReaderItem()
}

