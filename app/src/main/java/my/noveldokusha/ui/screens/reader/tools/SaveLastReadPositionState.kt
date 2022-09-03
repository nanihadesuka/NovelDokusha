package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.data.Repository
import my.noveldokusha.ui.screens.reader.ReaderViewModel

fun saveLastReadPositionState(
    repository: Repository,
    bookUrl: String,
    chapter: ReaderViewModel.ChapterState,
    oldChapter: ReaderViewModel.ChapterState? = null
) = CoroutineScope(Dispatchers.IO).launch {
    repository.withTransaction {
        repository.bookLibrary.updateLastReadChapter(
            bookUrl = bookUrl,
            lastReadChapterUrl = chapter.chapterUrl
        )

        if (oldChapter?.chapterUrl != null) repository.bookChapter.updatePosition(
            chapterUrl = oldChapter.chapterUrl,
            lastReadPosition = oldChapter.chapterItemIndex,
            lastReadOffset = oldChapter.offset
        )

        repository.bookChapter.updatePosition(
            chapterUrl = chapter.chapterUrl,
            lastReadPosition = chapter.chapterItemIndex,
            lastReadOffset = chapter.offset
        )
    }
}