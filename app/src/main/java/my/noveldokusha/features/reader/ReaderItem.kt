package my.noveldokusha.features.reader

import my.noveldokusha.tools.BookTextMapper

sealed interface ReaderItem {
    /**
     * Corresponds to index in the ordered chapter list.
     * Unique by chapter row
     */
    val chapterIndex: Int

    sealed interface Chapter : ReaderItem {
        val chapterUrl: String
    }

    sealed interface Position : ReaderItem, Chapter {
        /**
         * Index for the items of each [chapterIndex].
         * Unique by chapter
         */
        val chapterItemPosition: Int
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

    data class Title(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemPosition: Int,
        override val text: String,
        override val textTranslated: String? = null
    ) : ReaderItem, Text, Position

    data class Body(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemPosition: Int,
        override val text: String,
        override val location: Location,
        override val textTranslated: String? = null
    ) : ReaderItem, Text, Position, ParagraphLocation

    data class Image(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemPosition: Int,
        override val location: Location,
        val text: String,
        val image: BookTextMapper.ImgEntry
    ) : ReaderItem, Position, ParagraphLocation

    data class Translating(
        override val chapterIndex: Int,
        val sourceLang: String,
        val targetLang: String
    ) : ReaderItem

    data class GoogleTranslateAttribution(override val chapterIndex: Int) : ReaderItem
    data class Progressbar(override val chapterIndex: Int) : ReaderItem
    data class Divider(override val chapterIndex: Int) : ReaderItem
    data class BookEnd(override val chapterIndex: Int) : ReaderItem
    data class BookStart(override val chapterIndex: Int) : ReaderItem
    data class Error(override val chapterIndex: Int, val text: String) : ReaderItem
    data class Padding(override val chapterIndex: Int) : ReaderItem
}

