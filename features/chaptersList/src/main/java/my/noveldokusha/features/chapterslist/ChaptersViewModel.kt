package my.noveldokusha.features.chapterslist

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.BaseViewModel
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.DownloaderRepository
import my.noveldoksuha.data.EpubImporterRepository
import my.noveldokusha.chapterslist.R
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.Toasty
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.isContentUri
import my.noveldokusha.core.isLocalUri
import my.noveldokusha.core.utils.StateExtra_String
import my.noveldokusha.core.utils.toState
import my.noveldokusha.scraper.Scraper
import javax.inject.Inject

interface ChapterStateBundle {
    val rawBookUrl: String
    val bookTitle: String
}

@HiltViewModel
internal class ChaptersViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appScope: AppCoroutineScope,
    scraper: Scraper,
    private val toasty: Toasty,
    appPreferences: AppPreferences,
    appFileResolver: AppFileResolver,
    private val downloaderRepository: DownloaderRepository,
    private val chaptersRepository: ChaptersRepository,
    private val epubImporterRepository: EpubImporterRepository,
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
    private val book = appRepository.libraryBooks.getFlow(bookUrl)
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
            if (rawBookUrl.isContentUri && appRepository.libraryBooks.get(bookUrl) == null) {
                importUriContent()
            }
        }

        viewModelScope.launch {
            if (state.isLocalSource.value) return@launch

            if (!appRepository.bookChapters.hasChapters(bookUrl))
                updateChaptersList()

            if (appRepository.libraryBooks.get(bookUrl) != null)
                return@launch

            chaptersRepository.downloadBookMetadata(bookUrl = bookUrl, bookTitle = bookTitle)
        }

        viewModelScope.launch {
            chaptersRepository.getChaptersSortedFlow(bookUrl = bookUrl).collect {
                state.chapters.clear()
                state.chapters.addAll(it)
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val isBookmarked =
                appRepository.toggleBookmark(bookTitle = bookTitle, bookUrl = bookUrl)
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
        downloaderRepository.bookCoverImageUrl(bookUrl = bookUrl).onSuccess {
            if (it == null) return@onSuccess
            appRepository.libraryBooks.updateCover(bookUrl, it)
        }
    }

    private fun updateDescription() = viewModelScope.launch {
        if (state.isLocalSource.value) return@launch
        downloaderRepository.bookDescription(bookUrl = bookUrl).onSuccess {
            if (it == null) return@onSuccess
            appRepository.libraryBooks.updateDescription(bookUrl, it)
        }
    }

    private fun importUriContent() {
        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {
            state.error.value = ""
            state.isRefreshing.value = true
            val isInLibrary = appRepository.libraryBooks.existInLibrary(bookUrl)
            epubImporterRepository.importEpubFromContentUri(
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
            downloaderRepository.bookChaptersList(bookUrl = url)
                .onSuccess {
                    if (it.isEmpty())
                        toasty.show(R.string.no_chapters_found)
                    appRepository.bookChapters.merge(newChapters = it, bookUrl = url)
                }.onError {
                    state.error.value = it.message
                }
            state.isRefreshing.value = false

        }
    }

    suspend fun getLastReadChapter(): String? =
        chaptersRepository.getLastReadChapter(bookUrl = bookUrl)

    fun setAsUnreadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.bookChapters.setAsUnread(list.map { it.first })
        }
    }

    fun setAsReadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.bookChapters.setAsRead(list.map { it.first })
        }
    }

    fun downloadSelected() {
        if (state.isLocalSource.value) return
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            list.forEach { appRepository.chapterBody.fetchBody(it.first) }
        }
    }

    fun deleteDownloadsSelected() {
        if (state.isLocalSource.value) return
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.Default) {
            appRepository.chapterBody.removeRows(list.map { it.first })
        }
    }

    fun onSelectionModeChapterClick(chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) {
        val url = chapter.chapter.url
        if (state.selectedChaptersUrl.containsKey(url)) {
            state.selectedChaptersUrl.remove(url)
        } else {
            state.selectedChaptersUrl[url] = Unit
        }
        lastSelectedChapterUrl = url
    }

    fun saveImageAsCover(uri: Uri) {
        appRepository.libraryBooks.saveImageAsCover(imageUri = uri, bookUrl = bookUrl)
    }

    fun onSelectionModeChapterLongClick(chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) {
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

    fun onChapterLongClick(chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) {
        val url = chapter.chapter.url
        state.selectedChaptersUrl[url] = Unit
        lastSelectedChapterUrl = url
    }

    fun onChapterDownload(chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) {
        if (state.isLocalSource.value) return
        appScope.launch {
            appRepository.chapterBody.fetchBody(chapter.chapter.url)
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
