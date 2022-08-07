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
        repository.bookLibrary.get(bookUrl)?.let {
            repository.bookLibrary.update(it.copy(lastReadChapter = chapter.url))
        }

        if (oldChapter?.url != null) repository.bookChapter.get(oldChapter.url)?.let {
            repository.bookChapter.update(
                it.copy(
                    lastReadPosition = oldChapter.position,
                    lastReadOffset = oldChapter.offset
                )
            )
        }

        repository.bookChapter.get(chapter.url)?.let {
            repository.bookChapter.update(
                it.copy(
                    lastReadPosition = chapter.position,
                    lastReadOffset = chapter.offset
                )
            )
        }
    }
}