package my.noveldokusha.features.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.BookChaptersRepository
import my.noveldoksuha.data.LibraryBooksRepository
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.features.reader.domain.ChapterState
import my.noveldokusha.features.reader.domain.InitialPositionChapter
import my.noveldokusha.feature.local_database.tables.Chapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReaderRepository @Inject constructor(
    private val scope: AppCoroutineScope,
    private val database: AppDatabase,
    private val bookChaptersRepository: BookChaptersRepository,
    private val libraryBooksRepository: LibraryBooksRepository,
    private val appRepository: AppRepository,
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

    suspend fun getInitialChapterItemPosition(
        bookUrl: String,
        chapterIndex: Int,
        chapter: Chapter,
    ): InitialPositionChapter = coroutineScope {
        val titleChapterItemPosition = 0 // Hardcode or no?
        val book = async { appRepository.libraryBooks.get(bookUrl) }
        val position = InitialPositionChapter(
            chapterIndex = chapterIndex,
            chapterItemPosition = chapter.lastReadPosition,
            chapterItemOffset = chapter.lastReadOffset,
        )

        when {
            chapter.url == book.await()?.lastReadChapter -> position
            chapter.read -> InitialPositionChapter(
                chapterIndex = chapterIndex,
                chapterItemPosition = titleChapterItemPosition,
                chapterItemOffset = 0,
            )
            else -> position
        }
    }

    suspend fun downloadChapter(chapterUrl: String) =
        appRepository.chapterBody.fetchBody(chapterUrl)
}