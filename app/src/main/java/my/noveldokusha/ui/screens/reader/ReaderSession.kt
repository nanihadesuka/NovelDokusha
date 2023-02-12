package my.noveldokusha.ui.screens.reader

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.repository.Repository
import my.noveldokusha.services.NarratorMediaPlayerService
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.screens.reader.tools.*
import kotlin.math.ceil
import kotlin.properties.Delegates

class ReaderSession(
    val bookUrl: String,
    private val initialChapterUrl: String,
    private val scope: CoroutineScope,
    private val repository: Repository,
    private val translationManager: TranslationManager,
    private val appPreferences: AppPreferences,
    private val context: Context,
    val forceUpdateListViewState: suspend () -> Unit,
    val maintainLastVisiblePosition: suspend (suspend () -> Unit) -> Unit,
    val maintainStartPosition: suspend (suspend () -> Unit) -> Unit,
    val setInitialPosition: suspend (ItemPosition) -> Unit,
    val showInvalidChapterDialog: suspend () -> Unit,
) {

    var chapterUrl: String = initialChapterUrl

    val orderedChapters = mutableListOf<Chapter>()

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

    val screenScrollReadingPosStats = mutableStateOf<ReadingChapterPosStats?>(null)
    val screenScrollChapterPercentageProgress = derivedStateOf {
        val stats = screenScrollReadingPosStats.value ?: return@derivedStateOf 0f
        when (stats.chapterItemsCount) {
            0 -> 100f
            else -> ceil((stats.chapterItemIndex.toFloat() / stats.chapterItemsCount.toFloat()) * 100f)
        }
    }

    val speakerChapterPercentageProgress = derivedStateOf {
        val item = readerSpeaker.currentTextPlaying.value.item
        val stats = getItemPosStats(itemIndex = item.chapterItemIndex, chapterUrl = item.chapterUrl)
            ?: return@derivedStateOf 0f
        when (stats.chapterItemsCount) {
            0 -> 100f
            else -> ceil((stats.chapterItemIndex.toFloat() / stats.chapterItemsCount.toFloat()) * 100f)
        }
    }

    val isReaderInSpeakModeStarted =  derivedStateOf {
        readerSpeaker.settings.isThereActiveItem.value ||
                readerSpeaker.settings.isPlaying.value
    }

    val isReaderInSpeakMode = derivedStateOf {
        readerSpeaker.settings.isThereActiveItem.value &&
                readerSpeaker.settings.isPlaying.value
    }

    val liveTranslation = LiveTranslation(
        translationManager = translationManager,
        appPreferences = appPreferences
    )

    val chaptersLoader = ChaptersLoader(
        repository = repository,
        translateOrNull = { liveTranslation.translatorState?.translate?.invoke(it) },
        translationIsActive = { liveTranslation.translatorState != null },
        translationSourceLanguageOrNull = { liveTranslation.translatorState?.sourceLocale?.displayLanguage },
        translationTargetLanguageOrNull = { liveTranslation.translatorState?.targetLocale?.displayLanguage },
        bookUrl = bookUrl,
        orderedChapters = orderedChapters,
        readerState = ReaderState.INITIAL_LOAD,
        forceUpdateListViewState = forceUpdateListViewState,
        maintainLastVisiblePosition = maintainLastVisiblePosition,
        maintainStartPosition = maintainStartPosition,
        setInitialPosition = setInitialPosition,
        showInvalidChapterDialog = showInvalidChapterDialog,
    )

    val items = chaptersLoader.getItems()

    val readerSpeaker = ReaderSpeaker(
        coroutineScope = scope,
        context = context,
        items = items,
        chapterLoadedFlow = chaptersLoader.chapterLoadedFlow,
        isChapterIndexLoaded = chaptersLoader::isChapterIndexLoaded,
        isChapterIndexValid = chaptersLoader::isChapterIndexValid,
        tryLoadPreviousChapter = chaptersLoader::tryLoadPrevious,
        loadNextChapter = chaptersLoader::tryLoadNext,
        customSavedVoices = appPreferences.READER_TEXT_TO_SPEECH_SAVED_PREDEFINED_LIST.state(scope),
        setCustomSavedVoices = {
            appPreferences.READER_TEXT_TO_SPEECH_SAVED_PREDEFINED_LIST.value = it
        },
        getPreferredVoiceId = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_ID.value },
        setPreferredVoiceId = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_ID.value = it },
        getPreferredVoiceSpeed = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_SPEED.value },
        setPreferredVoiceSpeed = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_SPEED.value = it },
        getPreferredVoicePitch = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_PITCH.value },
        setPreferredVoicePitch = { appPreferences.READER_TEXT_TO_SPEECH_VOICE_PITCH.value = it },
    )

    fun init() {
        initLoadData()
        scope.launch {
            repository.libraryBooks.updateLastReadEpochTimeMilli(
                bookUrl,
                System.currentTimeMillis()
            )
        }
        initReaderTTSObservers()
    }

    private fun initLoadData() {
        scope.launch {
            val chapter = async(Dispatchers.IO) { repository.bookChapters.get(chapterUrl) }
            val loadTranslator = async(Dispatchers.IO) { liveTranslation.init() }
            val chaptersList = async(Dispatchers.Default) {
                orderedChapters.also { it.addAll(repository.bookChapters.chapters(bookUrl)) }
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
            chaptersLoader.tryLoadInitial(chapterIndex = chapterIndex.await())
        }
    }

    private fun initReaderTTSObservers() {
        scope.launch {
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
                        chaptersLoader.tryLoadNext()
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

        scope.launch(Dispatchers.Main.immediate) {
            snapshotFlow { isReaderInSpeakModeStarted.value }
                .filter { it }
                .collectLatest {
                    NarratorMediaPlayerService.start(context)
                }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerSpeaker
                .currentReaderItemFlow
                .debounce(timeoutMillis = 5_000)
                .filter { isReaderInSpeakMode.value }
                .collect {
                    saveLastReadPositionFromCurrentSpeakItem()
                }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerSpeaker
                .currentReaderItemFlow
                .filter { isReaderInSpeakMode.value }
                .collect {
                    val item = it.item
                    if (item !is ReaderItem.ParagraphLocation) return@collect
                    when (item.location) {
                        ReaderItem.Location.FIRST -> markChapterStartAsSeen(chapterUrl = item.chapterUrl)
                        ReaderItem.Location.LAST -> markChapterEndAsSeen(chapterUrl = item.chapterUrl)
                        ReaderItem.Location.MIDDLE -> Unit
                    }
                }
        }
    }

    fun startSpeaker(itemIndex: Int) {
        val startingItem = items.getOrNull(itemIndex) ?: return
        readerSpeaker.start()
        scope.launch {
            readerSpeaker.readChapterStartingFromItemIndex(
                chapterIndex = startingItem.chapterIndex,
                itemIndex = itemIndex
            )
        }
    }

    fun close() {
        chaptersLoader.coroutineContext.cancelChildren()
        if (isReaderInSpeakMode.value) {
            saveLastReadPositionFromCurrentSpeakItem()
        } else {
            saveLastReadPositionState(repository, bookUrl, currentChapter)
        }
        readerSpeaker.onClose()
        scope.coroutineContext.cancelChildren()
    }

    fun reloadReader() {
        chaptersLoader.reload()
        readerSpeaker.stop()
    }

    fun updateInfoViewTo(itemIndex: Int) {
        val stats = getItemPosStats(itemIndex = itemIndex, chapterUrl = chapterUrl) ?: return
        screenScrollReadingPosStats.value = stats
    }

    fun getItemPosStats(itemIndex: Int, chapterUrl: String) : ReadingChapterPosStats? {
        val item = items.getOrNull(itemIndex) ?: return null
        if (item !is ReaderItem.Position) return null
        val stats = chaptersLoader.chaptersStats[chapterUrl] ?: return null
        return ReadingChapterPosStats(
            chapterIndex = item.chapterIndex,
            chapterItemIndex = item.chapterItemIndex,
            chapterItemsCount = stats.itemsCount,
            chapterTitle = stats.chapter.title
        )
    }

    private val readRoutine = ChaptersIsReadRoutine(repository)

    fun markChapterStartAsSeen(chapterUrl: String) {
        readRoutine.setReadStart(chapterUrl = chapterUrl)
    }

    fun markChapterEndAsSeen(chapterUrl: String) {
        readRoutine.setReadEnd(chapterUrl = chapterUrl)
    }

    private fun saveLastReadPositionFromCurrentSpeakItem() {
        val item = readerSpeaker.settings.currentActiveItemState.value.item
        saveLastReadPositionState(
            repository = repository,
            bookUrl = bookUrl,
            chapter = ChapterState(
                chapterUrl = item.chapterUrl,
                chapterItemIndex = item.chapterItemIndex,
                offset = 0
            )
        )
    }
}