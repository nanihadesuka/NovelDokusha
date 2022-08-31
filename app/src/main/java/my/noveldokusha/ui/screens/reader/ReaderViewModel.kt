package my.noveldokusha.ui.screens.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
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
    textToSpeechManager: TextToSpeechManager,
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

    val readRoutine = ChaptersIsReadRoutine(repository)

    @Volatile
    var forceUpdateListViewState: (suspend () -> Unit)? = null

    @Volatile
    var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    var setInitialPosition: (suspend (ItemPosition) -> Unit)? = null

    @Volatile
    var showInvalidChapterDialog: (suspend () -> Unit)? = null

    val listIsEnabled = MutableLiveData<Boolean>(false)

    val scrollToTheTop = MutableSharedFlow<Unit>()
    val scrollToTheBottom = MutableSharedFlow<Unit>()

    private suspend fun <T> withMainNow(fn: suspend CoroutineScope.() -> T) =
        withContext(Dispatchers.Main.immediate, fn)

    val chaptersLoader = ChaptersLoader(
        repository = repository,
        translateOrNull = { liveTranslation.translator?.translate?.invoke(it) },
        translationIsActive = { liveTranslation.translator != null },
        translationSourceLanguageOrNull = { liveTranslation.translator?.sourceLocale?.displayLanguage },
        translationTargetLanguageOrNull = { liveTranslation.translator?.targetLocale?.displayLanguage },
        bookUrl = bookUrl,
        orderedChapters = orderedChapters,
        readerState = ReaderState.INITIAL_LOAD,
        forceUpdateListViewState = { withMainNow { forceUpdateListViewState?.invoke() } },
        maintainLastVisiblePosition = { withMainNow { maintainLastVisiblePosition?.invoke(it) } },
        maintainStartPosition = { withMainNow { maintainStartPosition?.invoke(it) } },
        setInitialPosition = { withMainNow { setInitialPosition?.invoke(it) } },
        showInvalidChapterDialog = { withMainNow { showInvalidChapterDialog?.invoke() } },
    )

    val items get() = chaptersLoader.getItems()

    val readerSpeaker = ReaderSpeaker(
        textToSpeechManager = textToSpeechManager,
        items = items,
        coroutineScope = viewModelScope,
        chapterLoadedFlow = chaptersLoader.chapterLoadedFlow,
        isChapterIndexLoaded = chaptersLoader::isChapterIndexLoaded,
        isChapterIndexValid = chaptersLoader::isChapterIndexValid,
        loadPreviousChapter = chaptersLoader::loadPrevious,
        loadNextChapter = chaptersLoader::loadNext,
        scrollToTheTop = scrollToTheTop,
        scrollToTheBottom = scrollToTheBottom,
    )

    init {
        viewModelScope.launch {
            val chapter = async(Dispatchers.IO) { repository.bookChapter.get(chapterUrl) }
            val loadTranslator = async(Dispatchers.IO) { liveTranslation.init() }
            val chaptersList = async(Dispatchers.Default) {
                orderedChapters.also { it.addAll(repository.bookChapter.chapters(bookUrl)) }
            }
            val chapterIndex = async(Dispatchers.Default) {
                chaptersList.await().indexOfFirst { it.url == chapterUrl }
            }
            chaptersList.await()
            loadTranslator.await()
            currentChapter = ChapterState(
                chapterUrl = chapterUrl,
                chapterItemIndex = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0,
            )
            // All data prepared! Let's load the current chapter
            chaptersLoader.loadInitial(chapterIndex = chapterIndex.await())
        }

        viewModelScope.launch {
            repository.bookLibrary.updateLastReadEpochTimeMilli(bookUrl, System.currentTimeMillis())
        }

        viewModelScope.launch {
            readerSpeaker.reachedChapterEndFlowChapterIndex.collect { chapterIndex ->
                withContext(Dispatchers.Main.immediate) {
                    if (chaptersLoader.isLastChapter(chapterIndex)) return@withContext
                    val nextChapterIndex = chapterIndex + 1
                    val chapterItem = chaptersLoader.orderedChapters[nextChapterIndex]
                    if (chaptersLoader.loadedChapters.contains(chapterItem.url)) {
                        readerSpeaker.readChapterStartingFromStart(
                            chapterIndex = nextChapterIndex
                        )
                    } else launch {
                        chaptersLoader.loadNext()
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

    fun onClose() {
        readerSpeaker.stop()
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

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int) {
        val chapter = chaptersLoader.chaptersStats[chapterUrl] ?: return
        readingPosStats = Pair(chapter, itemPos)
    }
}
