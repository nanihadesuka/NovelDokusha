package my.noveldokusha.ui.screens.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.tools.TextToSpeechManager
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.screens.reader.tools.*
import my.noveldokusha.utils.StateExtra_String
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.ceil
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
    private val textToSpeechManager: TextToSpeechManager,
) : BaseViewModel(), ReaderStateBundle {

    enum class ReaderState { IDLE, LOADING, INITIAL_LOAD }
    data class ChapterState(
        val chapterUrl: String,
        val chapterItemIndex: Int,
        val offset: Int
    )

    data class ChapterStats(val itemCount: Int, val chapter: Chapter, val chapterIndex: Int)

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    val textFont by appPreferences.READER_FONT_FAMILY.state(viewModelScope)
    val textSize by appPreferences.READER_FONT_SIZE.state(viewModelScope)
    val isTextSelectable by appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope)

    val liveTranslationSettingState get() = liveTranslation.settingsState
    val textToSpeechSettingData get() = readerSpeaker.settings
    var onTranslatorChanged
        get() = liveTranslation.onTranslatorChanged
        set(value) {
            liveTranslation.onTranslatorChanged = value
        }

    var currentChapter: ChapterState by Delegates.observable(
        ChapterState(
            chapterUrl = chapterUrl,
            chapterItemIndex = 0,
            offset = 0
        )
    ) { _, old, new ->
        chapterUrl = new.chapterUrl
        if (old.chapterUrl != new.chapterUrl) saveLastReadPositionState(
            repository,
            bookUrl,
            new,
            old
        )
    }

    var showReaderInfoView by mutableStateOf(false)
    val orderedChapters = mutableListOf<Chapter>()

    var readingPosStats by mutableStateOf<Pair<ChapterStats, Int>?>(null)
    val chapterPercentageProgress by derivedStateOf {
        val (info, itemPos) = readingPosStats ?: return@derivedStateOf 0f
        when (info.itemCount) {
            0 -> 100f
            else -> ceil((itemPos.toFloat() / info.itemCount.toFloat()) * 100f)
        }
    }

    val items = ArrayList<ReaderItem>()
    val readRoutine = ChaptersIsReadRoutine(repository)

    val readerSpeaker = ReaderSpeaker(
        textToSpeechManager = textToSpeechManager,
        items = items,
        coroutineScope = viewModelScope
    )

    private val initialLoadDone = AtomicBoolean(false)
    var forceUpdateListViewState: (() -> Unit)? = null
    var maintainLastVisiblePosition: ((() -> Unit) -> Unit)? = null
    var maintainStartPosition: ((() -> Unit) -> Unit)? = null
    var showInvalidChapterDialog: (() -> Unit)? = null

    val chaptersLoader = ChaptersLoader(
        repository = repository,
        liveTranslation = liveTranslation,
        items = items,
        orderedChapters = orderedChapters,
        readerState = ReaderState.INITIAL_LOAD,
        forceUpdateListViewState = { forceUpdateListViewState?.invoke() },
        maintainLastVisiblePosition = { maintainLastVisiblePosition?.invoke(it) },
        maintainStartPosition = { maintainStartPosition?.invoke(it) },
        showInvalidChapterDialog = { showInvalidChapterDialog?.invoke() }
    )

    data class ChapterPosition(val chapterIndex: Int, val chapterItemIndex: Int, val offset: Int)

    val moveToItemPositionFlow = MutableSharedFlow<ChapterPosition>()

    init {
        readerSpeaker.settings.isEnabled.value = true

        viewModelScope.launch {
            val chapter = async(Dispatchers.IO) { repository.bookChapter.get(chapterUrl) }
            val loadTranslator = async(Dispatchers.IO) { liveTranslation.init() }
            val loadChaptersList = async(Dispatchers.IO) {
                orderedChapters.addAll(repository.bookChapter.chapters(bookUrl))
            }
            loadChaptersList.await()
            val chapterIndex = orderedChapters.indexOfFirst { it.url == chapterUrl }
            loadTranslator.await()
            currentChapter = ChapterState(
                chapterUrl = chapterUrl,
                chapterItemIndex = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            // All data prepared! Let's load the current chapter
            chaptersLoader.loadInitialChapter(chapterUrl)
        }

        viewModelScope.launch {
            repository.bookLibrary.updateLastReadEpochTimeMilli(bookUrl, System.currentTimeMillis())
        }

        viewModelScope.launch {
            readerSpeaker.reachedChapterEndFlowChapterIndex.collect { index ->
                withContext(Dispatchers.Main.immediate) {
                    if (!readerSpeaker.settings.isEnabled.value) return@withContext
                    if (chaptersLoader.isLastChapter(index)) return@withContext
                    val nextChapterIndex = index + 1
                    val chapterItem = chaptersLoader.orderedChapters[nextChapterIndex]
                    if (chaptersLoader.loadedChapters.contains(chapterItem.url)) {
                        readerSpeaker.readChapterStartingFromStart(
                            chapterIndex = nextChapterIndex
                        )
                    } else launch {
                        chaptersLoader.loadNextChapter()
                        chaptersLoader.chapterLoadedFlow
                            .filter { it.type == ChaptersLoader.ChapterLoaded.Type.Next }
                            .take(1)
                            .collect {
                                readerSpeaker.readChapterStartingFromStart(
                                    chapterIndex = nextChapterIndex
                                )
                            }
                    }
                }
            }
        }

        viewModelScope.launch {
            chaptersLoader.initialChapterLoadedFlow.collect {
                when (it) {
                    is ChaptersLoader.InitialLoadCompleted.Initial -> {
                        val initialState = getChapterInitialPosition() ?: return@collect
                        val (chapterItemIndex: Int, offset: Int) = initialState
                        moveToItemPositionFlow.emit(
                            ChapterPosition(
                                chapterIndex = it.chapterIndex,
                                chapterItemIndex = chapterItemIndex,
                                offset = offset,
                            )
                        )
                    }
                    is ChaptersLoader.InitialLoadCompleted.Reloaded -> {
                        moveToItemPositionFlow.emit(
                            ChapterPosition(
                                chapterIndex = it.chapterIndex,
                                chapterItemIndex = it.chapterItemIndex,
                                offset = it.chapterItemOffset,
                            )
                        )
                    }
                }
            }
        }
    }

    fun startSpeaker(itemIndex: Int) {
        readerSpeaker.start()
        val startingItem = items[itemIndex]
        viewModelScope.launch {
            readerSpeaker.readChapterStartingFromItemIndex(
                chapterIndex = startingItem.chapterIndex,
                itemIndex = itemIndex
            )
        }
    }

    override fun onCleared() {
        chaptersLoader.coroutineContext.cancelChildren()
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    fun reloadReader() {
        chaptersLoader.reload()
        readerSpeaker.stop()
    }

    fun initialLoad(fn: () -> Unit) {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int) {
        val chapter = chaptersLoader.chaptersStats[chapterUrl] ?: return
        readingPosStats = Pair(chapter, itemPos)
    }

    suspend fun getChapterInitialPosition(): Pair<Int, Int>? {
        val stats = chaptersLoader.chaptersStats[currentChapter.chapterUrl] ?: return null
        return getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapter = stats.chapter,
            items = items
        )
    }
}
