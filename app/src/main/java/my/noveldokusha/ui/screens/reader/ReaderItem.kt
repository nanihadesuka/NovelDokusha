package my.noveldokusha.ui.screens.reader

import my.noveldokusha.tools.BookTextMapper

sealed interface ReaderItem {
    val chapterUrl: String

    /**
     * Index corresponding to items of the same chapter.
     */
    val chapterIndex: Int

    sealed interface Position : ReaderItem {
        /**
         * Index for the items of each [chapterIndex].
         */
        val chapterItemIndex: Int
    }

    enum class Location { FIRST, MIDDLE, LAST }
    sealed interface ParagraphLocation : ReaderItem {
        val location: Location
    }

    sealed interface Text : ReaderItem, Position {
        val text: String
        val textTranslated: String?
        val textToDisplay get() = textTranslated ?: text
    }

    data class GoogleTranslateAttribution(
        override val chapterUrl: String,
        override val chapterIndex: Int,
    ) : ReaderItem

    data class Title(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemIndex: Int,
        override val text: String,
        override val textTranslated: String? = null
    ) : ReaderItem, Text, Position

    data class Body(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemIndex: Int,
        override val text: String,
        override val location: Location,
        override val textTranslated: String? = null
    ) : ReaderItem, Text, Position, ParagraphLocation

    data class Image(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemIndex: Int,
        override val location: Location,
        val text: String,
        val image: BookTextMapper.ImgEntry
    ) : ReaderItem, Position, ParagraphLocation

    class Translating(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        val sourceLang: String,
        val targetLang: String
    ) : ReaderItem

    class Progressbar(override val chapterUrl: String, override val chapterIndex: Int) :
        ReaderItem

    class Divider(override val chapterUrl: String, override val chapterIndex: Int) : ReaderItem
    class BookEnd(override val chapterUrl: String, override val chapterIndex: Int) : ReaderItem
    class BookStart(override val chapterUrl: String, override val chapterIndex: Int) : ReaderItem
    class Error(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        val text: String
    ) : ReaderItem

    class Padding(override val chapterUrl: String, override val chapterIndex: Int) : ReaderItem
}

