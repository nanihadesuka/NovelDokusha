package my.noveldokusha.features.reader.manager

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppPreferences
import my.noveldokusha.features.reader.ChapterState
import my.noveldokusha.features.reader.ReaderItem
import my.noveldokusha.features.reader.ReaderState
import my.noveldokusha.features.reader.ReadingChapterPosStats
import my.noveldokusha.features.reader.chapterReadPercentage
import my.noveldokusha.features.reader.features.ReaderChaptersLoader
import my.noveldokusha.features.reader.features.ReaderLiveTranslation
import my.noveldokusha.features.reader.features.ReaderTextToSpeech
import my.noveldokusha.features.reader.features.ReaderViewHandlersActions
import my.noveldokusha.features.reader.tools.ChaptersIsReadRoutine
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.repository.ReaderRepository
import my.noveldokusha.services.narratorMediaControls.NarratorMediaControlsService
import my.noveldokusha.text_translator.domain.TranslationManager
import my.noveldokusha.tools.Utterance
import kotlin.properties.Delegates


class ReaderSession(
    val bookUrl: String,
    initialChapterUrl: String,
    private val appRepository: AppRepository,
    private val appPreferences: AppPreferences,
    private val readerRepository: ReaderRepository,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
    @ApplicationContext private val context: Context,
    translationManager: my.noveldokusha.text_translator.domain.TranslationManager,
) {
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("ReaderSession")
    )

    private var chapterUrl: String = initialChapterUrl

    private val readRoutine = ChaptersIsReadRoutine(appRepository)
    private val orderedChapters =
        mutableListOf<my.noveldokusha.feature.local_database.tables.Chapter>()

    var bookTitle: String? = null
    var bookCoverUrl: String? = null

    var currentChapter: ChapterState by Delegates.observable(
        ChapterState(
            chapterUrl = chapterUrl,
            chapterItemPosition = 0,
            offset = 0
        )
    ) { _, old, new ->
        chapterUrl = new.chapterUrl
        if (
            old.chapterUrl != new.chapterUrl &&
            savePositionMode.value == SavePositionMode.Reading
        ) {
            readerRepository.saveBookLastReadPositionState(bookUrl, new, old)
        }
    }

    private enum class SavePositionMode { Reading, Speaking }

    private val savePositionMode = derivedStateOf<SavePositionMode> {
        if (readerTextToSpeech.isSpeaking.value) SavePositionMode.Speaking else SavePositionMode.Reading
    }

    val readingStats = mutableStateOf<ReadingChapterPosStats?>(null)
    val readingChapterProgressPercentage = derivedStateOf {
        readingStats.value?.chapterReadPercentage() ?: 0f
    }

    val speakerStats = derivedStateOf {
        val item = readerTextToSpeech.currentTextPlaying.value.itemPos
        readerChaptersLoader.getItemContext(
            chapterIndex = item.chapterIndex,
            chapterItemPosition = item.chapterItemPosition
        )
    }

    val readerLiveTranslation = ReaderLiveTranslation(
        translationManager = translationManager,
        appPreferences = appPreferences
    )

    val readerChaptersLoader = ReaderChaptersLoader(
        appRepository = appRepository,
        translatorTranslateOrNull = { readerLiveTranslation.translatorState?.translate?.invoke(it) },
        translatorIsActive = { readerLiveTranslation.translatorState != null },
        translatorSourceLanguageOrNull = { readerLiveTranslation.translatorState?.sourceLocale?.displayLanguage },
        translatorTargetLanguageOrNull = { readerLiveTranslation.translatorState?.targetLocale?.displayLanguage },
        bookUrl = bookUrl,
        orderedChapters = orderedChapters,
        readerState = ReaderState.INITIAL_LOAD,
        readerViewHandlersActions = readerViewHandlersActions,
    )

    val items = readerChaptersLoader.getItems()

    val readerTextToSpeech = ReaderTextToSpeech(
        coroutineScope = scope,
        context = context,
        items = items,
        chapterLoadedFlow = readerChaptersLoader.chapterLoadedFlow,
        isChapterIndexLoaded = readerChaptersLoader::isChapterIndexLoaded,
        isChapterIndexTheLast = readerChaptersLoader::isChapterIndexTheLast,
        isChapterIndexValid = readerChaptersLoader::isChapterIndexValid,
        tryLoadPreviousChapter = readerChaptersLoader::tryLoadPrevious,
        loadNextChapter = readerChaptersLoader::tryLoadNext,
        customSavedVoices = appPreferences.READER_TEXT_TO_SPEECH_SAVED_PREDEFINED_LIST.state(
            scope
        ),
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
            appRepository.libraryBooks.updateLastReadEpochTimeMilli(
                bookUrl,
                System.currentTimeMillis()
            )
        }
        initReaderTTSObservers()
    }

    private fun initLoadData() {
        scope.launch {
            val book = async(Dispatchers.IO) { appRepository.libraryBooks.get(bookUrl) }
            val chapter = async(Dispatchers.IO) { appRepository.bookChapters.get(chapterUrl) }
            val loadTranslator = async(Dispatchers.IO) { readerLiveTranslation.init() }
            val chaptersList = async(Dispatchers.Default) {
                orderedChapters.also { it.addAll(appRepository.bookChapters.chapters(bookUrl)) }
            }
            val chapterIndex = async(Dispatchers.Default) {
                chaptersList.await().indexOfFirst { it.url == chapterUrl }
            }
            chaptersList.await()
            loadTranslator.await()
            bookCoverUrl = book.await()?.coverImageUrl
            bookTitle = book.await()?.title
            currentChapter = ChapterState(
                chapterUrl = chapterUrl,
                chapterItemPosition = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0,
            )
            // All data prepared! Let's load the current chapter
            readerChaptersLoader.tryLoadInitial(chapterIndex = chapterIndex.await())
        }
    }

    private fun initReaderTTSObservers() {
        scope.launch {
            readerTextToSpeech.reachedChapterEndFlowChapterIndex.collect { chapterIndex ->
                withContext(Dispatchers.Main.immediate) {
                    if (readerChaptersLoader.isLastChapter(chapterIndex)) return@withContext
                    val nextChapterIndex = chapterIndex + 1
                    val chapterItem = readerChaptersLoader.orderedChapters[nextChapterIndex]
                    if (readerChaptersLoader.loadedChapters.contains(chapterItem.url)) {
                        readerTextToSpeech.readChapterStartingFromStart(
                            chapterIndex = nextChapterIndex
                        )
                    } else launch {
                        readerChaptersLoader.tryLoadNext()
                        readerChaptersLoader.chapterLoadedFlow
                            .filter { it.type == ReaderChaptersLoader.ChapterLoaded.Type.Next }
                            .take(1)
                            .collect {
                                readerTextToSpeech.readChapterStartingFromStart(
                                    chapterIndex = nextChapterIndex
                                )
                            }
                    }
                }
            }
        }

        scope.launch(Dispatchers.Main.immediate) {
            snapshotFlow { readerTextToSpeech.isActive.value }
                .filter { it }
                .collectLatest {
                    NarratorMediaControlsService.start(context)
                }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerTextToSpeech
                .currentReaderItem
                .filter { it.playState == Utterance.PlayState.PLAYING }
                .filter { savePositionMode.value == SavePositionMode.Speaking }
                .collect { saveLastReadPositionStateSpeaker(it.itemPos) }
        }

        scope.launch(Dispatchers.Main.immediate) {
            readerTextToSpeech
                .currentReaderItem
                .filter { it.playState == Utterance.PlayState.PLAYING }
                .filter { savePositionMode.value == SavePositionMode.Speaking }
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
        readerTextToSpeech.start()
        scope.launch {
            readerTextToSpeech.readChapterStartingFromItemIndex(
                itemIndex = itemIndex,
                chapterIndex = startingItem.chapterIndex
            )
        }
    }

    fun close() {
        readerChaptersLoader.coroutineContext.cancelChildren()
        when (savePositionMode.value) {
            SavePositionMode.Reading -> readerRepository.saveBookLastReadPositionState(
                bookUrl,
                currentChapter
            )
            SavePositionMode.Speaking -> saveLastReadPositionStateSpeaker(
                item = readerTextToSpeech.currentTextPlaying.value.itemPos
            )
        }
        readerTextToSpeech.onClose()
        scope.coroutineContext.cancelChildren()
        NarratorMediaControlsService.stop(context)
    }

    fun reloadReader() {
        readerChaptersLoader.reload()
        readerTextToSpeech.stop()
    }

    fun updateInfoViewTo(itemIndex: Int) {
        val stats = readerChaptersLoader.getItemContext(
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

    private fun saveLastReadPositionStateSpeaker(item: ReaderItem.Position) {
        readerRepository.saveBookLastReadPositionState(
            bookUrl = bookUrl,
            newChapter = ChapterState(
                chapterUrl = item.chapterUrl,
                chapterItemPosition = item.chapterItemPosition,
                offset = 0
            )
        )
    }
}
