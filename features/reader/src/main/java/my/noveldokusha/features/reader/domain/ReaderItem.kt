package my.noveldokusha.features.reader.domain

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.atomic.AtomicInteger

private val counter = AtomicInteger(0)

internal sealed interface ReaderItem {
    /**
     * Corresponds to index in the ordered chapter list.
     * Unique by chapter row
     */
    val chapterIndex: Int

    val itemUniqueId: Int

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
    ) : ReaderItem, Text, Position {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Body(
        override val chapterUrl: String,
        override val chapterIndex: Int,
        override val chapterItemPosition: Int,
        override val text: String,
        override val location: Location,
        override val textTranslated: String? = null
    ) : ReaderItem, Text, Position, ParagraphLocation {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Image(
        override val chapterUrl: String,
        override val chapterItemPosition: Int,
        override val location: Location,
        override val chapterIndex: Int,
        val text: String,
        val image: ImgEntry,
    ) : ReaderItem, Position, ParagraphLocation {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Translating(
        override val chapterIndex: Int,
        val sourceLang: String,
        val targetLang: String,
        val visible: MutableState<Boolean> = mutableStateOf(true)
    ) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class GoogleTranslateAttribution(override val chapterIndex: Int) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Progressbar(
        override val chapterIndex: Int,
        val visible: MutableState<Boolean> = mutableStateOf(true)
    ) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Divider(override val chapterIndex: Int) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class BookEnd(override val chapterIndex: Int) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class BookStart(override val chapterIndex: Int) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Error(override val chapterIndex: Int, val text: String) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }

    data class Padding(override val chapterIndex: Int) : ReaderItem {
        override val itemUniqueId: Int = counter.incrementAndGet()
    }
}

