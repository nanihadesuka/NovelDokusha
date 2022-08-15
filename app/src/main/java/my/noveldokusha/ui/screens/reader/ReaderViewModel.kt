package my.noveldokusha.ui.screens.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.screens.reader.tools.ChaptersIsReadRoutine
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation
import my.noveldokusha.ui.screens.reader.tools.saveLastReadPositionState
import my.noveldokusha.utils.StateExtra_String
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.set
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val liveTranslation: LiveTranslation,
) : BaseViewModel(), ReaderStateBundle {
    enum class ReaderState { IDLE, LOADING, INITIAL_LOAD }

    data class ChapterState(val url: String, val position: Int, val offset: Int)
    data class ChapterStats(val itemCount: Int, val chapter: Chapter, val index: Int)

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    val textFont by appPreferences.READER_FONT_FAMILY.state(viewModelScope)
    val textSize by appPreferences.READER_FONT_SIZE.state(viewModelScope)
    val isTextSelectable by appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope)

    val translator get() = liveTranslation.translator
    val liveTranslationSettingState get() = liveTranslation.settingsState
    var onTranslatorChanged
        get() = liveTranslation.onTranslatorChanged
        set(value) {
            liveTranslation.onTranslatorChanged = value
        }

    var currentChapter: ChapterState by Delegates.observable(
        ChapterState(
            chapterUrl,
            0,
            0
        )
    ) { _, old, new ->
        chapterUrl = new.url
        if (old.url != new.url) saveLastReadPositionState(repository, bookUrl, new, old)
    }

    var showReaderInfoView by mutableStateOf(false)
    var orderedChapters: List<Chapter> = listOf()
        private set

    var readingPosStats by mutableStateOf<Pair<ChapterStats, Int>?>(null)

    var initJob: Job
        private set

    init {
        initJob = viewModelScope.launch {

            val chapter = viewModelScope.async(Dispatchers.IO) {
                repository.bookChapter.get(chapterUrl)
            }
            val bookChapters = viewModelScope.async(Dispatchers.IO) {
                repository.bookChapter.chapters(bookUrl)
            }

            currentChapter = ChapterState(
                url = chapterUrl,
                position = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            orderedChapters = bookChapters.await()
            liveTranslation.init()
        }
        
        viewModelScope.launch {
            repository.bookLibrary.updateLastReadEpochTimeMilli(bookUrl, System.currentTimeMillis())
        }
    }

    val items = ArrayList<ReaderItem>()
    val readRoutine = ChaptersIsReadRoutine(repository)
    var readerState = ReaderState.INITIAL_LOAD

    private val chaptersStats = mutableMapOf<String, ChapterStats>()
    private val initialLoadDone = AtomicBoolean(false)

    override fun onCleared() {
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    fun reloadReader() {
        readerState = ReaderState.INITIAL_LOAD
        items.clear()
    }

    fun initialLoad(fn: () -> Unit) {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    fun getNextChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index + 1

    fun getPreviousChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index - 1

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int) {
        val chapter = chaptersStats[chapterUrl] ?: return
        readingPosStats = Pair(chapter, itemPos)
    }

    fun addChapterStats(chapter: Chapter, itemCount: Int, index: Int) {
        chaptersStats[chapter.url] = ChapterStats(itemCount, chapter, index)
    }

    suspend fun fetchChapterBody(chapterUrl: String) =
        repository.bookChapterBody.fetchBody(chapterUrl)

    suspend fun getChapterInitialPosition(): Pair<Int, Int>? {
        val stats = chaptersStats[currentChapter.url] ?: return null
        return my.noveldokusha.ui.screens.reader.tools.getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapter = stats.chapter,
            items = items
        )
    }
}
