package my.noveldokusha.ui.screens.reader

import my.noveldokusha.data.BookTextUtils

sealed class ReaderItem {
    abstract val chapterUrl: String

    interface Position {
        val pos: Int
    }

    enum class LOCATION { FIRST, MIDDLE, LAST }

    data class GOOGLE_TRANSLATE_ATTRIBUTION(
        override val chapterUrl: String,
    ) : ReaderItem()

    data class TITLE(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String,
        val textTranslated: String? = null
    ) : ReaderItem(), Position {
        val textToDisplay get() = textTranslated ?: text
    }

    data class BODY(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String,
        val location: LOCATION,
        val textTranslated: String? = null
    ) : ReaderItem(), Position {
        val textToDisplay get() = textTranslated ?: text
    }

    data class BODY_IMAGE(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String,
        val location: LOCATION,
        val image: BookTextUtils.ImgEntry
    ) : ReaderItem(), Position

    class TRANSLATING(
        override val chapterUrl: String,
        val sourceLang: String,
        val targetLang: String
    ) : ReaderItem()

    class PROGRESSBAR(override val chapterUrl: String) : ReaderItem()

    class DIVIDER(override val chapterUrl: String) : ReaderItem()
    class BOOK_END(override val chapterUrl: String) : ReaderItem()
    class BOOK_START(override val chapterUrl: String) : ReaderItem()
    class ERROR(override val chapterUrl: String, val text: String) : ReaderItem()
    class PADDING(override val chapterUrl: String) : ReaderItem()
}

