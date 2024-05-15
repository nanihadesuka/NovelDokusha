package my.noveldokusha.features.chapterslist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.DownloaderRepository
import my.noveldokusha.core.AppPreferences
import my.noveldokusha.tooling.local_database.tables.Book
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
                AppPreferences.TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
                AppPreferences.TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
                AppPreferences.TERNARY_STATE.inactive -> chapters
            }
        }
        .flowOn(Dispatchers.Default)

    suspend fun getLastReadChapter(bookUrl: String): String? =
        appRepository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: appRepository.bookChapters.getFirstChapter(bookUrl)?.url

}