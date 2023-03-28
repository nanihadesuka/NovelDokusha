package my.noveldokusha.ui.screens.chaptersList

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.Response
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

data class BookDataView(
    val title: String,
    val url: String,
    val completed: Boolean = false,
    val lastReadChapter: String? = null,
    val inLibrary: Boolean = false,
    val coverImageUrl: String? = null,
    val description: String = ""
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

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: AppCoroutineScope,
    private val networkClient: NetworkClient,
    private val scraper: Scraper,
    private val toasty: Toasty,
    val appPreferences: AppPreferences,
    state: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle {
    override var bookUrl by StateExtra_String(state)
    override var bookTitle by StateExtra_String(state)

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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (!repository.bookChapters.hasChapters(bookMetadata.url))
                updateChaptersList()

            val book = repository.libraryBooks.get(bookMetadata.url)
            if (book != null)
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

    val book by repository.libraryBooks.getFlow(bookUrl)
        .filterNotNull()
        .map(::BookDataView)
        .toState(
            viewModelScope,
            BookDataView(title = bookTitle, url = bookUrl, coverImageUrl = null)
        )

    var error by mutableStateOf("")
    val selectedChaptersUrl = mutableStateMapOf<String, Unit>()
    val chaptersWithContext = mutableStateListOf<ChapterWithContext>()
    var isRefreshing by mutableStateOf(false)
    var searchText by mutableStateOf("")

    init {
        viewModelScope.launch {
            repository.bookChapters.getChaptersWithContexFlow(bookMetadata.url)
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
                    snapshotFlow { searchText }.flowOn(Dispatchers.Main)
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
                    chaptersWithContext.clear()
                    chaptersWithContext.addAll(it)
                }
        }
    }

    fun reloadAll() {
        updateCover()
        updateDescription()
        updateChaptersList()
    }

    fun updateCover() = viewModelScope.launch(Dispatchers.IO) {
        val res = downloadBookCoverImageUrl(scraper, networkClient, bookUrl)
        if (res is Response.Success)
            repository.libraryBooks.updateCover(bookUrl, res.data)
    }

    fun updateDescription() = viewModelScope.launch(Dispatchers.IO) {
        val res = downloadBookDescription(scraper, networkClient, bookUrl)
        if (res is Response.Success)
            repository.libraryBooks.updateDescription(bookUrl, res.data)
    }

    private var loadChaptersJob: Job? = null
    fun updateChaptersList() {
        if (bookMetadata.url.startsWith("local://")) {
            toasty.show(R.string.local_book_nothing_to_update)
            isRefreshing = false
            return
        }

        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = appScope.launch {

            error = ""
            isRefreshing = true
            val url = bookMetadata.url
            val res = downloadChaptersList(scraper, networkClient, url)
            isRefreshing = false
            when (res) {
                is Response.Success -> {
                    if (res.data.isEmpty())
                        toasty.show(R.string.no_chapters_found)

                    withContext(Dispatchers.IO) {
                        repository.bookChapters.merge(res.data, url)
                    }
                }
                is Response.Error -> {
                    error = res.message
                }
            }
        }
    }

    fun toggleChapterSort() {
        appPreferences.CHAPTERS_SORT_ASCENDING.value =
            when (appPreferences.CHAPTERS_SORT_ASCENDING.value) {
                AppPreferences.TERNARY_STATE.active -> AppPreferences.TERNARY_STATE.inverse
                AppPreferences.TERNARY_STATE.inverse -> AppPreferences.TERNARY_STATE.active
                AppPreferences.TERNARY_STATE.inactive -> AppPreferences.TERNARY_STATE.active
            }
    }

    suspend fun toggleBookmark() = withContext(Dispatchers.IO) {
        repository.libraryBooks.toggleBookmark(bookMetadata)
        repository.libraryBooks.get(bookMetadata.url)?.inLibrary ?: false
    }

    suspend fun getLastReadChapter() = withContext(Dispatchers.IO) {
        repository.libraryBooks.get(bookUrl)?.lastReadChapter
            ?: repository.bookChapters.getFirstChapter(bookUrl)?.url
    }

    fun setSelectedAsUnread() {
        val list = selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            repository.bookChapters.setAsUnread(list.map { it.first })
        }
    }

    fun setSelectedAsRead() {
        val list = selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            repository.bookChapters.setAsRead(list.map { it.first })
        }
    }

    fun downloadSelected() {
        val list = selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            list.forEach { repository.chapterBody.fetchBody(it.first) }
        }
    }

    fun deleteDownloadSelected() {
        val list = selectedChaptersUrl.toList()
        appScope.launch(Dispatchers.IO) {
            repository.chapterBody.removeRows(list.map { it.first })
        }
    }

    fun closeSelectionMode() {
        selectedChaptersUrl.clear()
    }

    fun selectAll() {
        chaptersWithContext
            .toList()
            .map { it.chapter.url to Unit }
            .let { selectedChaptersUrl.putAll(it) }
    }

    fun selectAllAfterSelectedOnes() {
        chaptersWithContext
            .toList()
            .dropWhile { !selectedChaptersUrl.contains(it.chapter.url) }
            .map { it.chapter.url to Unit }
            .let { selectedChaptersUrl.putAll(it) }
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