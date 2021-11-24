package my.noveldokusha.data

import androidx.room.Embedded
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter

data class BookMetadata(val title: String, val url: String)
{
    override fun equals(other: Any?): Boolean = if (other is BookMetadata) (url == other.url) else false
    override fun hashCode(): Int = url.hashCode()
}

data class ChapterMetadata(val title: String, val url: String)
{
    override fun equals(other: Any?): Boolean = if (other is ChapterMetadata) (url == other.url) else false
    override fun hashCode(): Int = url.hashCode()
}

data class BookWithContext(@Embedded val book: Book, val chaptersCount: Int, val chaptersReadCount: Int)
data class ChapterWithContext(@Embedded val chapter: Chapter, val downloaded: Boolean, val lastReadChapter: Boolean)