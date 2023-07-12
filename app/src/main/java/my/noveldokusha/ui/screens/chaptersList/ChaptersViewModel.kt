package my.noveldokusha.ui.screens.chaptersList

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.isContentUri
import my.noveldokusha.isLocalUri
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.downloadBookCoverImageUrl
import my.noveldokusha.scraper.downloadBookDescription
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.StateExtra_String
import my.noveldokusha.utils.toState
import javax.inject.Inject

interface ChapterStateBundle {
    val rawBookUrl: String
    val bookTitle: String
}

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: AppCoroutineScope,
    private val networkClient: NetworkClient,
    private val scraper: Scraper,
    private val toasty: Toasty,
    private val appPreferences: AppPreferences,
    private val appFileResolver: AppFileResolver,
    stateHandle: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle {

    override val rawBookUrl by StateExtra_String(stateHandle)
    override val bookTitle by StateExtra_String(stateHandle)

    val bookUrl = appFileResolver.getLocalIfContentType(rawBookUrl, bookFolderName = bookTitle)

    @Volatile
    private var loadChaptersJob: Job? = null

    @Volatile
    private var lastSelectedChapterUrl: String? = null
    private val source = scraper.getCompatibleSource(bookUrl)
    private val book = repository.libraryBooks.getFlow(bookUrl)
        .filterNotNull()
        .map(ChaptersScreenState::BookState)
        .toState(
            viewModelScope,
            ChaptersScreenState.BookState(title = bookTitle, url = bookUrl, coverImageUrl = null)
        )

    val state = ChaptersScreenState(
        book = book,
        error = mutableStateOf(""),
        chapters = mutableStateListOf(),
        selectedChaptersUrl = mutableStateMapOf(),
        isRefreshing = mutableStateOf(false),
        sourceCatalogNameStrRes = mutableStateOf(source?.nameStrId),
        settingChapterSort = appPreferences.CHAPTERS_SORT_ASCENDING.state(viewModelScope),
        isLocalSource = mutableStateOf(bookUrl.isLocalUri),
        isRefreshable = mutableStateOf(rawBookUrl.isContentUri || !bookUrl.isLocalUri)
    )

    init {
        appScope.launch {
            if (rawBookUrl.isContentUri && repository.libraryBooks.get(bookUrl) == null) {
                importUriContent()
            }
        }

        viewModelScope.launch {
            if (state.isLocalSource.value) return@launch

            if (!repository.bookChapters.hasChapters(bookUrl))
                updateChaptersList()

            if (repository.libraryBooks.get(bookUrl) != null)
                return@launch

            val coverUrl = async { downloadBookCoverImageUrl(scraper, networkClient, bookUrl) }
            val description = async { downloadBookDescription(scraper, networkClient, bookUrl) }

            repository.libraryBooks.insert(
                Book(
                    title = bookTitle,
                    url = bookUrl,
                    coverImageUrl = coverUrl.await().toSuccessOrNull()?.data ?: "",
                    description = description.await().toSuccessOrNull()?.data ?: ""
                )
            )
        }

        viewModelScope.launch {
            repository.bookChapters.getChaptersWithContextFlow(bookUrl)
                .map { removeCommonTextFromTitles(it) }
                // Sort the chapters given the order preference
                .combine(appPreferences.CHAPTERS_SORT_ASCENDING.flow()) { chapters, sorted ->
                    when (sorted) {
                        AppPreferences.TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
                        AppPreferences.TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
                        AppPreferences.TERNARY_STATE.inactive -> chapters
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    state.chapters.clear()
                    state.chapters.addAll(it)
                }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val isBookmarked = repository.toggleBookmark(bookTitle = bookTitle, bookUrl = bookUrl)
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            toasty.show(msg)
        }
    }

    fun onPullRefresh() {
        if (!state.isRefreshable.value) {
            toasty.show(R.string.local_book_nothing_to_update)
            state.isRefreshing.value = false
            return
        }
        toasty.show(R.string.updating_book_info)
        if (rawBookUrl.isContentUri) {
            importUriContent()
        } else if (!state.isLocalSource.value) {
            updateCover()
            updateDescription()
            updateChaptersList()
        }
    }

    private fun updateCover() = viewModelScope.launch {
        if (state.isLocalSource.value || book.value.coverImageUrl?.isLocalUri == true) return@launch
        downloadBookCoverImageUrl(scraper, networkClient, bookUrl).onSuccess {
            if (it == null) return@onSuccess
            repository.libraryBooks.updateCover(bookUrl, it)
        }
    }

    private fun updateDescription() = viewModelScope.launch {
        if (state.isLocalSource.value) return@launch
        downloadBookDescription(scraper, networkClient, bookUrl).onSuccess {
            if (it == null) return@onSuccess
            repository.libraryBooks.updateDescription(bookUrl, it)
        }
    }

    private fun importUriContent() {
        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {
            state.error.value = ""
            state.isRefreshing.value = true
            val rawBookUrl = rawBookUrl
            val bookTitle = bookTitle
            val isInLibrary = repository.libraryBooks.existInLibrary(bookUrl)
            repository.importEpubFromContentUri(
                contentUri = rawBookUrl,
                bookTitle = bookTitle,
                addToLibrary = isInLibrary
            ).onError {
                state.error.value = it.message
            }
            state.isRefreshing.value = false
        }
    }

    private fun updateChaptersList() {
        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {
            state.error.value = ""
            state.isRefreshing.value = true
            val url = bookUrl
            downloadChaptersList(
                scraper = scraper, bookUrl = url
            ).onSuccess {
                if (it.isEmpty())
                    toasty.show(R.string.no_chapters_found)
                repository.bookChapters.merge(newChapters = it, bookUrl = url)
            }.onError {
                state.error.value = it.message
            }
            state.isRefreshing.value = false

        }
    }

    suspend fun getLastReadChapter(): String? {
        return repository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: repository.bookChapters.getFirstChapter(bookUrl)?.url
    }

    fun setAsUnreadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            repository.bookChapters.setAsUnread(list.map { it.first })
        }
    }

    fun setAsReadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            repository.bookChapters.setAsRead(list.map { it.first })
        }
    }

    fun downloadSelected() {
        if (state.isLocalSource.value) return
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            list.forEach { repository.chapterBody.fetchBody(it.first) }
        }
    }

    fun deleteDownloadsSelected() {
        if (state.isLocalSource.value) return
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            repository.chapterBody.removeRows(list.map { it.first })
        }
    }

    fun onSelectionModeChapterClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        if (state.selectedChaptersUrl.containsKey(url)) {
            state.selectedChaptersUrl.remove(url)
        } else {
            state.selectedChaptersUrl[url] = Unit
        }
        lastSelectedChapterUrl = url
    }

    fun saveImageAsCover(uri: Uri) {
        repository.libraryBooks.saveImageAsCover(imageUri = uri, bookUrl = bookUrl)
    }

    fun onSelectionModeChapterLongClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        if (url != lastSelectedChapterUrl) {
            val indexOld = state.chapters.indexOfFirst { it.chapter.url == lastSelectedChapterUrl }
            val indexNew = state.chapters.indexOfFirst { it.chapter.url == url }
            val min = minOf(indexOld, indexNew)
            val max = maxOf(indexOld, indexNew)
            if (min >= 0 && max >= 0) {
                for (index in min..max) {
                    state.selectedChaptersUrl[state.chapters[index].chapter.url] = Unit
                }
                lastSelectedChapterUrl = state.chapters[indexNew].chapter.url
                return
            }
        }

        if (state.selectedChaptersUrl.containsKey(url)) {
            state.selectedChaptersUrl.remove(url)
        } else {
            state.selectedChaptersUrl[url] = Unit
        }
        lastSelectedChapterUrl = url
    }

    fun onChapterLongClick(chapter: ChapterWithContext) {
        val url = chapter.chapter.url
        state.selectedChaptersUrl[url] = Unit
        lastSelectedChapterUrl = url
    }

    fun onChapterDownload(chapter: ChapterWithContext) {
        if (state.isLocalSource.value) return
        appScope.launch {
            repository.chapterBody.fetchBody(chapter.chapter.url)
        }
    }

    fun unselectAll() {
        state.selectedChaptersUrl.clear()
    }

    fun selectAll() {
        state.chapters
            .toList()
            .map { it.chapter.url to Unit }
            .let { state.selectedChaptersUrl.putAll(it) }
    }

    fun invertSelection() {
        val allChaptersUrl = state.chapters.asSequence().map { it.chapter.url }.toSet()
        val selectedUrl = state.selectedChaptersUrl.asSequence().map { it.key }.toSet()
        val inverse = (allChaptersUrl - selectedUrl).asSequence().associateWith { }
        state.selectedChaptersUrl.clear()
        state.selectedChaptersUrl.putAll(inverse)
    }
}
