package my.noveldokusha.ui.reader

import my.noveldokusha.data.BookTextUtils
import java.util.concurrent.atomic.AtomicInteger

sealed class ReaderItem
{
    abstract val chapterUrl: String
    abstract val key: String

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
    ) : ReaderItem(), Position {
        override val key = "$chapterUrl#TITLE#$pos"
    }

    data class BODY(
        override val chapterUrl: String,
        override val pos: Int,
        val text: String,
        val location: LOCATION
    ) : ReaderItem(), Position
    {
        val image by lazy { BookTextUtils.ImgEntry.fromXMLString(text) }
        val isImage by lazy { image != null }
        override val key = "$chapterUrl#BODY#$pos#$location"
    }

    companion object {
        val i = AtomicInteger(0)
    }

    class PROGRESSBAR(override val chapterUrl: String) : ReaderItem() {
        override val key = "$chapterUrl#PROGRESSBAR#${i.incrementAndGet()}"
    }
    class DIVIDER(override val chapterUrl: String) : ReaderItem() {
        override val key = "$chapterUrl#DIVIDER#${i.incrementAndGet()}"
    }
    class BOOK_END(override val chapterUrl: String) : ReaderItem() {
        override val key = "$chapterUrl#BOOK_END#${i.incrementAndGet()}"
    }
    class BOOK_START(override val chapterUrl: String) : ReaderItem() {
        override val key = "$chapterUrl#BOOK_START#${i.incrementAndGet()}"
    }
    class ERROR(override val chapterUrl: String, val text: String) : ReaderItem() {
        override val key = "$chapterUrl#ERROR#${i.incrementAndGet()}"
    }
}

