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
import my.noveldokusha.services.narratorMediaControls.NarratorMediaControlsService
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.screens.reader.tools.*
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
    private var bookCoverUrl: String? = null
    private var chapterUrl: String = initialChapterUrl

    private val readRoutine = ChaptersIsReadRoutine(repository)
    private val orderedChapters = mutableListOf<Chapter>()

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

    val readingStats = mutableStateOf<ReadingChapterPosStats?>(null)
    val readingChapterProgressPercentage = derivedStateOf {
        readingStats.value?.chapterReadPercentage() ?: 0f
    }

    val speakerStats = derivedStateOf {
        val item = readerSpeaker.currentTextPlaying.value.itemPos
        chaptersLoader.getItemContext(
            chapterPosition = item.chapterPosition,
            chapterItemPosition = item.chapterItemPosition
        )
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
            val book = async(Dispatchers.IO) { repository.libraryBooks.get(bookUrl) }
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
            bookCoverUrl = book.await()?.coverImageUrl
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
            snapshotFlow { readerSpeaker.isActive.value }
                .filter { it }
                .collectLatest {
                    NarratorMediaControlsService.start(context)
                }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerSpeaker
                .currentReaderItem
                .debounce(timeoutMillis = 5_000)
                .filter { readerSpeaker.isSpeaking.value }
                .collect {
                    saveLastReadPositionFromCurrentSpeakItem()
                }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerSpeaker
                .currentReaderItem
                .filter { readerSpeaker.isSpeaking.value }
                .collect {
                    val item = it.itemPos
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
                chapterIndex = startingItem.chapterPosition,
                itemIndex = itemIndex
            )
        }
    }

    fun close() {
        chaptersLoader.coroutineContext.cancelChildren()
        if (readerSpeaker.isSpeaking.value) {
            saveLastReadPositionFromCurrentSpeakItem()
        } else {
            saveLastReadPositionState(repository, bookUrl, currentChapter)
        }
        readerSpeaker.onClose()
        scope.coroutineContext.cancelChildren()
        NarratorMediaControlsService.stop(context)
    }

    fun reloadReader() {
        chaptersLoader.reload()
        readerSpeaker.stop()
    }

    fun updateInfoViewTo(itemIndex: Int) {
        val stats = chaptersLoader.getItemContext(
            itemIndex = itemIndex,
            chapterUrl = chapterUrl
        ) ?: return
        readingStats.value = stats
    }


    fun markChapterStartAsSeen(chapterUrl: String) {
        readRoutine.setReadStart(chapterUrl = chapterUrl)
    }

    fun markChapterEndAsSeen(chapterUrl: String) {
        readRoutine.setReadEnd(chapterUrl = chapterUrl)
    }

    private fun saveLastReadPositionFromCurrentSpeakItem() {
        val item = readerSpeaker.settings.currentActiveItemState.value.itemPos
        saveLastReadPositionState(
            repository = repository,
            bookUrl = bookUrl,
            chapter = ChapterState(
                chapterUrl = item.chapterUrl,
                chapterItemIndex = item.chapterItemPosition,
                offset = 0
            )
        )
    }
}