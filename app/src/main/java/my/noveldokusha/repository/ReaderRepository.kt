package my.noveldokusha.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.features.reader.ChapterState
import my.noveldokusha.features.reader.domain.ChapterState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRepository @Inject constructor(
    private val scope: AppCoroutineScope,
    private val database: AppDatabase,
    private val bookChaptersRepository: BookChaptersRepository,
    private val libraryBooksRepository: LibraryBooksRepository,
) {

    fun saveBookLastReadPositionState(
        bookUrl: String,
        newChapter: ChapterState,
        oldChapter: ChapterState? = null
    ) {
        scope.launch(Dispatchers.IO) {
            database.transaction {
                libraryBooksRepository.updateLastReadChapter(
                    bookUrl = bookUrl,
                    lastReadChapterUrl = newChapter.chapterUrl
                )

                if (oldChapter?.chapterUrl != null) bookChaptersRepository.updatePosition(
                    chapterUrl = oldChapter.chapterUrl,
                    lastReadPosition = oldChapter.chapterItemPosition,
                    lastReadOffset = oldChapter.offset
                )

                bookChaptersRepository.updatePosition(
                    chapterUrl = newChapter.chapterUrl,
                    lastReadPosition = newChapter.chapterItemPosition,
                    lastReadOffset = newChapter.offset
                )
            }
        }
    }
}