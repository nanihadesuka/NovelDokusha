package my.noveldokusha.ui.screens.chaptersList

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import kotlinx.coroutines.withContext
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.downloadBookCoverImageUrl
import my.noveldokusha.scraper.downloadBookDescription
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.StateExtra_String
import my.noveldokusha.utils.toState
import javax.inject.Inject

interface ChapterStateBundle {
    val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
    var bookUrl: String
    var bookTitle: String
}

data class ChapterScreenState(
    val book: State<BookState>,
    val error: MutableState<String>,
    val selectedChaptersUrl: SnapshotStateMap<String, Unit>,
    val chapters: SnapshotStateList<ChapterWithContext>,
    val isRefreshing: MutableState<Boolean>,
    val searchTextInput: MutableState<String>,
    val sourceCatalogName: MutableState<String>,
    val settingChapterSort: MutableState<AppPreferences.TERNARY_STATE>
) {

    val isInSelectionMode = derivedStateOf { selectedChaptersUrl.size != 0 }

    data class BookState(
        val title: String,
        val url: String,
        val completed: Boolean = false,
        val lastReadChapter: String? = null,
        val inLibrary: Boolean = false,
        val coverImageUrl: String? = null,
        val description: String = "",
    ) {
        constructor(book: Book) : this(
            title = book.title,
            url = book.url,
            completed = book.completed,
            lastReadChapter = book.lastReadChapter,
            inLibrary = book.inLibrary,
            coverImageUrl = book.coverImageUrl,
            description = book.description
        )
    }
}

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: AppCoroutineScope,
    private val networkClient: NetworkClient,
    private val scraper: Scraper,
    private val toasty: Toasty,
    val appPreferences: AppPreferences,
    stateHandle: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle {
    override var bookUrl by StateExtra_String(stateHandle)
    override var bookTitle by StateExtra_String(stateHandle)

    class IntentData : Intent {
        private var bookUrl by Extra_String()
        private var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(
            ctx,
            ChaptersActivity::class.java
        ) {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    private val source = scraper.getCompatibleSource(bookUrl)!!

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (!repository.bookChapters.hasChapters(bookMetadata.url))
                updateChaptersList()

            if (repository.libraryBooks.get(bookMetadata.url) != null)
                return@launch

            val coverUrl = async { downloadBookCoverImageUrl(scraper, networkClient, bookUrl) }
            val description = async { downloadBookDescription(scraper, networkClient, bookUrl) }

            repository.libraryBooks.insert(
                Book(
                    title = bookMetadata.title,
                    url = bookMetadata.url,
                    coverImageUrl = coverUrl.await().toSuccessOrNull()?.data ?: "",
                    description = description.await().toSuccessOrNull()?.data ?: ""
                )
            )
        }
    }

    private val book = repository.libraryBooks.getFlow(bookUrl)
        .filterNotNull()
        .map(ChapterScreenState::BookState)
        .toState(
            viewModelScope,
            ChapterScreenState.BookState(title = bookTitle, url = bookUrl, coverImageUrl = null)
        )

    val state = ChapterScreenState(
        book = book,
        error = mutableStateOf(""),
        chapters = mutableStateListOf(),
        selectedChaptersUrl = mutableStateMapOf(),
        isRefreshing = mutableStateOf(false),
        searchTextInput = mutableStateOf(""),
        sourceCatalogName = mutableStateOf(source.name),
        settingChapterSort = appPreferences.CHAPTERS_SORT_ASCENDING.state(viewModelScope)
    )

    init {
        viewModelScope.launch {
            repository.bookChapters.getChaptersWithContextFlow(bookMetadata.url)
                .map { removeCommonTextFromTitles(it) }
                // Sort the chapters given the order preference
                .combine(appPreferences.CHAPTERS_SORT_ASCENDING.flow()) { chapters, sorted ->
                    when (sorted) {
                        AppPreferences.TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
                        AppPreferences.TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
                        AppPreferences.TERNARY_STATE.inactive -> chapters
                    }
                }
                // Filter the chapters if search is active
                .combine(
                    snapshotFlow { state.searchTextInput.value }.flowOn(Dispatchers.Main)
                ) { chapters, searchText ->
                    if (searchText.isBlank()) chapters
                    else chapters.filter {
                        it.chapter.title.contains(
                            searchText,
                            ignoreCase = true
                        )
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
            repository.libraryBooks.toggleBookmark(bookMetadata)
            val isBookmarked = repository.libraryBooks.get(bookMetadata.url)?.inLibrary ?: false
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            toasty.show(msg)
        }
    }

    fun onPullRefresh() {
        toasty.show(R.string.updating_book_info)
        updateCover()
        updateDescription()
        updateChaptersList()
    }

    private fun updateCover() = viewModelScope.launch {
        downloadBookCoverImageUrl(scraper, networkClient, bookUrl)
            .onSuccess { repository.libraryBooks.updateCover(bookUrl, it) }
    }

    private fun updateDescription() = viewModelScope.launch {
        downloadBookDescription(scraper, networkClient, bookUrl).onSuccess {
            repository.libraryBooks.updateDescription(bookUrl, it)
        }
    }

    private var loadChaptersJob: Job? = null
    private fun updateChaptersList() {
        if (bookMetadata.url.startsWith("local://")) {
            toasty.show(R.string.local_book_nothing_to_update)
            state.isRefreshing.value = false
            return
        }

        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {

            state.error.value = ""
            state.isRefreshing.value = true
            val url = bookMetadata.url
            val res = downloadChaptersList(scraper, networkClient, url)
            state.isRefreshing.value = false
            res.onSuccess {
                if (it.isEmpty())
                    toasty.show(R.string.no_chapters_found)
                withContext(Dispatchers.IO) {
                    repository.bookChapters.merge(it, url)
                }
            }.onError {
                state.error.value = it.message
            }
        }
    }

    @Volatile
    private var lastSelectedChapterUrl: String? = null

    suspend fun getLastReadChapter(): String? {
        return repository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: repository.bookChapters.getFirstChapter(bookUrl)?.url
    }

    fun setAsUnreadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            repository.bookChapters.setAsUnread(list.map { it.first })
        }
    }

    fun setAsReadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            repository.bookChapters.setAsRead(list.map { it.first })
        }
    }

    fun downloadSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            list.forEach { repository.chapterBody.fetchBody(it.first) }
        }
    }

    fun deleteDownloadsSelected() {
        val list = state.selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
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
        appScope.launch(Dispatchers.IO) {
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
        val inverse = (allChaptersUrl - selectedUrl).asSequence().associateWith { Unit }
        state.selectedChaptersUrl.clear()
        state.selectedChaptersUrl.putAll(inverse)
    }
}

private fun removeCommonTextFromTitles(list: List<ChapterWithContext>): List<ChapterWithContext> {
    // Try removing repetitive title text from chapters
    if (list.size <= 1) return list
    val first = list.first().chapter.title
    val prefix =
        list.fold(first) { acc, e -> e.chapter.title.commonPrefixWith(acc, ignoreCase = true) }
    val suffix =
        list.fold(first) { acc, e -> e.chapter.title.commonSuffixWith(acc, ignoreCase = true) }

    // Kotlin Std Lib doesn't have optional ignoreCase parameter for removeSurrounding
    fun String.removeSurrounding(
        prefix: CharSequence,
        suffix: CharSequence,
        ignoreCase: Boolean = false
    ): String {
        if ((length >= prefix.length + suffix.length) && startsWith(prefix, ignoreCase) && endsWith(
                suffix,
                ignoreCase
            )
        ) {
            return substring(prefix.length, length - suffix.length)
        }
        return this
    }

    return list.map { data ->
        val newTitle = data
            .chapter.title.removeSurrounding(prefix, suffix, ignoreCase = true)
            .ifBlank { data.chapter.title }
        data.copy(chapter = data.chapter.copy(title = newTitle))
    }
}