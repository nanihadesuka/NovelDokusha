package my.noveldokusha.features.chapterslist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.DownloaderRepository
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.appPreferences.TernaryState
import my.noveldokusha.feature.local_database.tables.Book
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChaptersRepository @Inject constructor(
    private val appRepository: AppRepository,
    private val downloaderRepository: DownloaderRepository,
    private val appPreferences: AppPreferences,
) {

    suspend fun downloadBookMetadata(bookUrl: String, bookTitle: String) = coroutineScope {
        val coverUrl = async { downloaderRepository.bookCoverImageUrl(bookUrl = bookUrl) }
        val description = async { downloaderRepository.bookDescription(bookUrl = bookUrl) }

        appRepository.libraryBooks.insert(
            Book(
                title = bookTitle,
                url = bookUrl,
                coverImageUrl = coverUrl.await().toSuccessOrNull()?.data ?: "",
                description = description.await().toSuccessOrNull()?.data ?: ""
            )
        )
    }


    fun getChaptersSortedFlow(bookUrl: String) = appRepository.bookChapters
        .getChaptersWithContextFlow(bookUrl = bookUrl)
        .map(::removeCommonTextFromTitles)
        // Sort the chapters given the order preference
        .combine(appPreferences.CHAPTERS_SORT_ASCENDING.flow()) { chapters, sorted ->
            when (sorted) {
                TernaryState.Active -> chapters.sortedBy { it.chapter.position }
                TernaryState.Inverse -> chapters.sortedByDescending { it.chapter.position }
                TernaryState.Inactive -> chapters
            }
        }
        .flowOn(Dispatchers.Default)

    suspend fun getLastReadChapter(bookUrl: String): String? =
        appRepository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: appRepository.bookChapters.getFirstChapter(bookUrl)?.url

}