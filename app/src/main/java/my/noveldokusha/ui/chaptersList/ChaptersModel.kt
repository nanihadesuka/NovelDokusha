package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.*
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast
import javax.inject.Inject

interface ChapterStateBundle
{
    val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
    var bookUrl: String
    var bookTitle: String
}

@HiltViewModel
class ChaptersModel @Inject constructor(
    private val repository: Repository,
    appPreferences: AppPreferences,
    state: SavedStateHandle,
) : BaseViewModel(), ChapterStateBundle
{
    override var bookUrl by StateExtra_String(state)
    override var bookTitle by StateExtra_String(state)

    class IntentData : Intent
    {
        val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
        private var bookUrl by Extra_String()
        private var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(ctx, ChaptersActivity::class.java)
        {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    init
    {
        viewModelScope.launch(Dispatchers.IO) {
            if (!repository.bookChapter.hasChapters(bookMetadata.url))
                updateChaptersList()

            val book = repository.bookLibrary.get(bookMetadata.url)
            if (book == null)
                repository.bookLibrary.insert(Book(title = bookMetadata.title, url = bookMetadata.url))
        }
    }

    val selectionModeVisible = MutableLiveData(false)
    val isInLibrary = repository.bookLibrary.existInLibraryFlow(bookMetadata.url).asLiveData()
    val onFetching = MutableLiveData<Boolean>()
    val onError = MutableLiveData<String>()
    val onErrorVisibility = MutableLiveData<Int>()
    val selectedChaptersUrl = mutableSetOf<String>()
    val chaptersFilterFlow = MutableStateFlow("")
    val chaptersWithContextLiveData = repository.bookChapter.getChaptersWithContexFlow(bookMetadata.url)
        .map {
            // Try removing repetitive title text from chapters
            if (it.size <= 1) return@map it
            val first = it.first().chapter.title
            val prefix = it.fold(first) { acc, e -> e.chapter.title.commonPrefixWith(acc, ignoreCase = true) }
            val suffix = it.fold(first) { acc, e -> e.chapter.title.commonSuffixWith(acc, ignoreCase = true) }

            // Kotlin Std Lib doesn't have optional ignoreCase parameter
            fun String.removeSurrounding(prefix: CharSequence, suffix: CharSequence, ignoreCase: Boolean = false): String
            {
                if ((length >= prefix.length + suffix.length) && startsWith(prefix, ignoreCase) && endsWith(suffix, ignoreCase))
                {
                    return substring(prefix.length, length - suffix.length)
                }
                return this
            }

            return@map it.map { data ->
                val newTitle = data.chapter.title.removeSurrounding(prefix, suffix, ignoreCase = true)
                    .ifBlank { data.chapter.title }
                data.copy(chapter = data.chapter.copy(title = newTitle))
            }
        }.combine(appPreferences.CHAPTERS_SORT_ASCENDING_flow()) { chapters, sorted ->
            when (sorted)
            {
                AppPreferences.TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
                AppPreferences.TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
                AppPreferences.TERNARY_STATE.inactive -> chapters
            }
        }
        .combine(chaptersFilterFlow.debounce(50)) { chapters, searchText ->
            if (searchText.isBlank()) chapters
            else chapters.filter { it.chapter.title.contains(searchText, ignoreCase = true) }
        }
        .flowOn(Dispatchers.Default).asLiveData()

    private var loadChaptersJob: Job? = null
    fun updateChaptersList()
    {
        if (bookMetadata.url.startsWith("local://"))
        {
            toast(R.string.local_book_nothing_to_update.stringRes())
            onFetching.postValue(false)
            return
        }

        if (loadChaptersJob?.isActive == true) return
        loadChaptersJob = CoroutineScope(Dispatchers.Main).launch {

            onErrorVisibility.value = View.GONE
            onFetching.value = true
            val url = bookMetadata.url
            val res = withContext(Dispatchers.IO) { downloadChaptersList(url) }
            onFetching.value = false
            when (res)
            {
                is Response.Success ->
                {
                    if (res.data.isEmpty())
                        toast(R.string.no_chapters_found.stringRes())

                    withContext(Dispatchers.IO) {
                        repository.bookChapter.merge(res.data, url)
                    }
                }
                is Response.Error ->
                {
                    onErrorVisibility.value = View.VISIBLE
                    onError.value = res.message
                }
            }
        }
    }

    fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
        repository.bookLibrary.toggleBookmark(bookMetadata)
    }

    suspend fun getLastReadChapter() = withContext(Dispatchers.IO) {
        repository.bookLibrary.get(bookUrl)?.lastReadChapter ?: repository.bookChapter.getFirstChapter(bookUrl)?.url
    }

    suspend fun getIsBookInLibrary() = withContext(Dispatchers.IO) {
        repository.bookLibrary.existInLibrary(bookUrl)
    }

    fun setSelectedAsUnread()
    {
        val list = selectedChaptersUrl.toList()
        CoroutineScope(Dispatchers.IO).launch { repository.bookChapter.setAsUnread(list) }
    }

    fun setSelectedAsRead()
    {
        val list = selectedChaptersUrl.toList()
        CoroutineScope(Dispatchers.IO).launch { repository.bookChapter.setAsRead(list) }
    }

    fun downloadSelected()
    {
        val list = selectedChaptersUrl.toList()
        CoroutineScope(Dispatchers.IO).launch {
            list.forEach { repository.bookChapterBody.fetchBody(it) }
        }
    }

    fun deleteDownloadSelected()
    {
        val list = selectedChaptersUrl.toList()
        CoroutineScope(Dispatchers.IO).launch {
            repository.bookChapterBody.removeRows(list)
        }
    }

    fun closeSelectionMode()
    {
        selectedChaptersUrl.clear()
        updateSelectionModeBarState()
    }

    fun updateSelectionModeBarState()
    {
        selectionModeVisible.postValue(selectedChaptersUrl.isNotEmpty())
    }
}