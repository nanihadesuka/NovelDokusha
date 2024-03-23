package my.noveldokusha.interactor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.isLocalUri
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.repository.DownloaderRepository
import my.noveldokusha.scraper.Scraper
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class LibraryUpdatesInteractions @Inject constructor(
    private val appRepository: AppRepository,
    private val scraper: Scraper,
    private val downloaderRepository: DownloaderRepository,
) {
    data class NewUpdate(
        val newChapters: List<Chapter>,
        val book: Book
    )

    data class CountingUpdating(
        val updated: Int,
        val total: Int
    )

    suspend fun updateLibraryBooks(
        completedOnes: Boolean,
        countingUpdating: MutableStateFlow<CountingUpdating?>,
        currentUpdating: MutableStateFlow<Set<Book>>,
        newUpdates: MutableStateFlow<Set<NewUpdate>>,
        failedUpdates: MutableStateFlow<Set<Book>>,
    ): Unit = withContext(Dispatchers.Default) {
        appRepository.libraryBooks.getAllInLibrary()
            .filter { it.completed == completedOnes }
            .filter { !it.url.isLocalUri }
            .also { list ->
                countingUpdating.update {
                    CountingUpdating(
                        updated = 0,
                        total = list.size
                    )
                }
            }
            .groupBy { it.url.toHttpUrlOrNull()?.host }
            .map { (_, books) ->
                async {
                    for (book in books) {
                        updateBook(
                            book = book,
                            currentUpdating = currentUpdating,
                            newUpdates = newUpdates,
                            failedUpdates = failedUpdates,
                            countingUpdating = countingUpdating
                        )
                    }
                }
            }
            .awaitAll()
    }


    private suspend fun updateBook(
        book: Book,
        countingUpdating: MutableStateFlow<CountingUpdating?>,
        currentUpdating: MutableStateFlow<Set<Book>>,
        newUpdates: MutableStateFlow<Set<NewUpdate>>,
        failedUpdates: MutableStateFlow<Set<Book>>,
    ): Unit = withContext(Dispatchers.Default) {
        currentUpdating.update { it + book }
        val oldChaptersList = async(Dispatchers.IO) {
            appRepository.bookChapters.chapters(book.url).map { it.url }.toSet()
        }

        downloaderRepository.bookChaptersList(bookUrl = book.url).onSuccess { chapters ->
            oldChaptersList.join()
            appRepository.bookChapters.merge(chapters, book.url)
            val newChapters = chapters.filter { it.url !in oldChaptersList.await() }
            if (newChapters.isEmpty()) return@onSuccess
            newUpdates.update { it + NewUpdate(book = book, newChapters = newChapters) }

        }.onError {
            failedUpdates.update { it + book }
        }
        currentUpdating.update { it - book }
        countingUpdating.update { it?.copy(updated = it.updated + 1) }
    }
}