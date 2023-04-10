package my.noveldokusha.ui.screens.reader.features

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.VoicePredefineState
import my.noveldokusha.tools.TextToSpeechManager
import my.noveldokusha.tools.Utterance
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.screens.reader.ChapterIndex
import my.noveldokusha.ui.screens.reader.ReaderItem
import my.noveldokusha.ui.screens.reader.tools.indexOfReaderItem

data class TextToSpeechSettingData(
    val isPlaying: MutableState<Boolean>,
    val isLoadingChapter: MutableState<Boolean>,
    val activeVoice: State<VoiceData?>,
    val voiceSpeed: State<Float>,
    val voicePitch: State<Float>,
    val availableVoices: SnapshotStateList<VoiceData>,
    val currentActiveItemState: State<TextSynthesis>,
    val isThereActiveItem: State<Boolean>,
    val customSavedVoices: State<List<VoicePredefineState>>,
    val setCustomSavedVoices: (List<VoicePredefineState>) -> Unit,
    val setPlaying: (Boolean) -> Unit,
    val playFirstVisibleItem: () -> Unit,
    val playPreviousItem: () -> Unit,
    val playPreviousChapter: () -> Unit,
    val playNextItem: () -> Unit,
    val playNextChapter: () -> Unit,
    val scrollToActiveItem: () -> Unit,
    val setVoiceId: (voiceId: String) -> Unit,
    val setVoiceSpeed: (Float) -> Unit,
    val setVoicePitch: (Float) -> Unit,
)

data class TextSynthesis(
    val itemPos: ReaderItem.Position,
    override val playState: Utterance.PlayState
) : Utterance<TextSynthesis> {
    override val utteranceId = "${itemPos.chapterItemPosition}-${itemPos.chapterIndex}"
    override fun copyWithState(playState: Utterance.PlayState) = copy(playState = playState)
}

class ReaderTextToSpeech(
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val items: List<ReaderItem>,
    private val chapterLoadedFlow: Flow<ReaderChaptersLoader.ChapterLoaded>,
    private val customSavedVoices: State<List<VoicePredefineState>>,
    private val setCustomSavedVoices: (List<VoicePredefineState>) -> Unit,
    private val isChapterIndexValid: (chapterIndex: Int) -> Boolean,
    private val isChapterIndexTheLast: (chapterIndex: Int) -> Boolean,
    private val isChapterIndexLoaded: (chapterIndex: Int) -> Boolean,
    private val tryLoadPreviousChapter: () -> Unit,
    private val loadNextChapter: () -> Unit,
    private val getPreferredVoiceId: () -> String,
    private val setPreferredVoiceId: (voiceId: String) -> Unit,
    private val getPreferredVoicePitch: () -> Float,
    private val setPreferredVoicePitch: (voiceId: Float) -> Unit,
    private val getPreferredVoiceSpeed: () -> Float,
    private val setPreferredVoiceSpeed: (voiceId: Float) -> Unit,
) {
    private val halfBuffer = 2
    private var updateJob: Job? = null
    private val manager = TextToSpeechManager<TextSynthesis>(
        context = context,
        initialItemState = TextSynthesis(
            itemPos = ReaderItem.Title(
                chapterUrl = "",
                chapterIndex = -1,
                chapterItemPosition = 0,
                text = ""
            ),
            playState = Utterance.PlayState.FINISHED
        )
    )

    val scrolledToTheTop = MutableSharedFlow<Unit>()
    val scrolledToTheBottom = MutableSharedFlow<Unit>()
    val currentReaderItem = manager.currentTextSpeakFlow
    val currentTextPlaying = manager.currentActiveItemState as State<TextSynthesis>
    val reachedChapterEndFlowChapterIndex = MutableSharedFlow<ChapterIndex>() // chapter pos
    val startReadingFromFirstVisibleItem = MutableSharedFlow<Unit>()
    val scrollToReaderItem = MutableSharedFlow<ReaderItem>()
    val scrollToChapterTop = MutableSharedFlow<ChapterIndex>()

    val state = TextToSpeechSettingData(
        isPlaying = mutableStateOf(false),
        isLoadingChapter = mutableStateOf(false),
        activeVoice = manager.activeVoice as State<VoiceData?>,
        availableVoices = manager.availableVoices,
        currentActiveItemState = manager.currentActiveItemState,
        isThereActiveItem = derivedStateOf {
            isChapterIndexValid(manager.currentActiveItemState.value.itemPos.chapterIndex)
        },
        voicePitch = manager.voicePitch,
        voiceSpeed = manager.voiceSpeed,
        customSavedVoices = customSavedVoices,
        setCustomSavedVoices = setCustomSavedVoices,
        setVoiceId = ::setVoice,
        playFirstVisibleItem = ::playFirstVisibleItem,
        playNextChapter = ::playNextChapter,
        playPreviousChapter = ::playPreviousChapter,
        playNextItem = ::playNextItem,
        playPreviousItem = ::playPreviousItem,
        setPlaying = ::setPlaying,
        scrollToActiveItem = ::scrollToActiveItem,
        setVoicePitch = ::setVoicePitch,
        setVoiceSpeed = ::setVoiceSpeed,
    )

    val isAtFirstItem = derivedStateOf {
        currentTextPlaying.value.itemPos.chapterIndex == 0
    }
    val isAtLastItem = derivedStateOf {
        !isAtFirstItem.value && isChapterIndexTheLast(currentTextPlaying.value.itemPos.chapterIndex)
    }

    val isActive = derivedStateOf { state.isThereActiveItem.value || state.isPlaying.value }
    val isSpeaking = derivedStateOf { state.isThereActiveItem.value && state.isPlaying.value }

    init {
        coroutineScope.launch {
            manager
                .serviceLoadedFlow
                .take(1)
                .collect {
                    manager.trySetVoiceById(getPreferredVoiceId())
                    manager.trySetVoicePitch(getPreferredVoicePitch())
                    manager.trySetVoiceSpeed(getPreferredVoiceSpeed())
                }
        }
    }

    @Synchronized
    fun start() {
        state.isPlaying.value = true
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            manager
                .currentTextSpeakFlow
                .filter { it.playState == Utterance.PlayState.FINISHED }
                .collect {
                    withContext(Dispatchers.Main) {
                        when (manager.queueList.size) {
                            halfBuffer -> {
                                val lastUtterance = manager
                                    .queueList
                                    .asSequence()
                                    .last().value
                                readChapterNextChunk(
                                    chapterIndex = lastUtterance.itemPos.chapterIndex,
                                    chapterItemPosition = lastUtterance.itemPos.chapterItemPosition,
                                    quantity = halfBuffer
                                )
                            }
                            0 -> {
                                launch {
                                    reachedChapterEndFlowChapterIndex.emit(it.itemPos.chapterIndex)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
        }
    }

    @Synchronized
    fun stop() {
        state.isPlaying.value = false
        updateJob?.cancel()
        manager.stop()
    }

    fun onClose() {
        stop()
        manager.service.shutdown()
    }

    suspend fun readChapterStartingFromStart(
        chapterIndex: Int
    ) = withContext(Dispatchers.Main.immediate) {
        readChapterStartingFromChapterItemPosition(
            chapterIndex = chapterIndex,
            chapterItemPosition = 0
        )
    }

    private suspend fun readChapterStartingFromChapterItemPosition(
        chapterIndex: Int,
        chapterItemPosition: Int,
    ) = withContext(Dispatchers.Main.immediate) {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) {
            reachedChapterEndFlowChapterIndex.emit(chapterIndex)
            return@withContext
        }
        readChapterStartingFromItemIndex(
            itemIndex = itemIndex,
            chapterIndex = chapterIndex
        )
    }

    suspend fun readChapterStartingFromItemIndex(
        itemIndex: Int,
        chapterIndex: Int,
    ) = withContext(Dispatchers.Main.immediate) {
        val nextItems = getChapterNextItems(
            itemIndex = itemIndex,
            chapterIndex = chapterIndex,
            quantity = halfBuffer * 2
        )

        if (nextItems.isEmpty()) {
            reachedChapterEndFlowChapterIndex.emit(chapterIndex)
            return@withContext
        }

        val firstItem = nextItems.first()
        manager.setCurrentSpeakState(
            TextSynthesis(
                itemPos = firstItem,
                playState = Utterance.PlayState.LOADING
            )
        )

        nextItems.forEach(::speakItem)
    }

    @Synchronized
    private fun scrollToActiveItem() {
        coroutineScope.launch {
            val currentItemPos = state.currentActiveItemState.value.itemPos
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = currentItemPos.chapterIndex,
                chapterItemPosition = currentItemPos.chapterItemPosition,
            )
            val item = items.getOrNull(itemIndex) ?: return@launch
            scrollToReaderItem.emit(item)
        }
    }

    @Synchronized
    private fun playFirstVisibleItem() {
        stop()
        start()
        coroutineScope.launch {
            startReadingFromFirstVisibleItem.emit(Unit)
        }
    }

    @Synchronized
    private fun setPlaying(playing: Boolean) {
        if (!playing) {
            stop()
            return
        }
        start()
        val state = state.currentActiveItemState.value

        if (isChapterIndexValid(state.itemPos.chapterIndex)) {
            coroutineScope.launch {
                readChapterStartingFromChapterItemPosition(
                    chapterIndex = state.itemPos.chapterIndex,
                    chapterItemPosition = state.itemPos.chapterItemPosition
                )
            }
        } else {
            coroutineScope.launch {
                startReadingFromFirstVisibleItem.emit(Unit)
            }
        }
    }

    @Synchronized
    private fun playNextItem() {
        if (!state.isThereActiveItem.value) {
            return
        }

        coroutineScope.launch {
            val currentItemPos = state.currentActiveItemState.value.itemPos
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = currentItemPos.chapterIndex,
                chapterItemPosition = currentItemPos.chapterItemPosition,
            )
            if (itemIndex <= -1 || itemIndex >= items.lastIndex) return@launch
            val nextItemRelativeIndex = items
                .subList(itemIndex + 1, items.size)
                .indexOfFirst { it is ReaderItem.Position }
            if (nextItemRelativeIndex == -1) return@launch
            val nextItemIndex = itemIndex + 1 + nextItemRelativeIndex
            val nextItem = items.getOrNull(nextItemIndex) as? ReaderItem.Position ?: return@launch
            stop()
            start()
            readChapterStartingFromItemIndex(
                itemIndex = nextItemIndex,
                chapterIndex = nextItem.chapterIndex
            )
            scrollToReaderItem.emit(nextItem)
        }
    }

    @Synchronized
    private fun playPreviousItem() {
        if (!state.isThereActiveItem.value) {
            return
        }

        coroutineScope.launch {
            val currentItemPos = state.currentActiveItemState.value.itemPos
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = currentItemPos.chapterIndex,
                chapterItemPosition = currentItemPos.chapterItemPosition,
            )
            if (itemIndex <= 0) return@launch
            val previousItemRelativeIndex = items
                .subList(0, itemIndex)
                .asReversed()
                .indexOfFirst { it is ReaderItem.Position }
            if (previousItemRelativeIndex == -1) return@launch
            val previousItemIndex = itemIndex - 1 - previousItemRelativeIndex
            val previousItem = items.getOrNull(previousItemIndex) ?: return@launch
            stop()
            start()
            readChapterStartingFromItemIndex(
                itemIndex = previousItemIndex,
                chapterIndex = previousItem.chapterIndex
            )
            scrollToReaderItem.emit(previousItem)
        }
    }

    @Synchronized
    private fun playNextChapter() {
        if (!state.isThereActiveItem.value) {
            return
        }

        val currentState = state.currentActiveItemState.value
        val nextChapterIndex = currentState.itemPos.chapterIndex + 1
        stop()
        if (!isChapterIndexValid(nextChapterIndex)) {
            coroutineScope.launch {
                val item = items.findLast {
                    it is ReaderItem.Position && it.chapterIndex == currentState.itemPos.chapterIndex
                } as? ReaderItem.Position ?: return@launch

                manager.currentActiveItemState.value = currentState.copy(
                    playState = Utterance.PlayState.FINISHED,
                    itemPos = item
                )
                scrolledToTheBottom.emit(Unit)
            }
            return
        }
        start()
        coroutineScope.launch {
            if (!isChapterIndexLoaded(nextChapterIndex)) {
                state.isLoadingChapter.value = true
                loadNextChapter()
                chapterLoadedFlow
                    .filter { it.chapterIndex == nextChapterIndex }
                    .take(1)
                    .collect()
                state.isLoadingChapter.value = false
            }
            readChapterStartingFromStart(nextChapterIndex)
            scrollToChapterTop.emit(nextChapterIndex)
        }
    }

    @Synchronized
    private fun playPreviousChapter() {
        if (!state.isThereActiveItem.value) {
            return
        }

        val currentItemState = state.currentActiveItemState.value
        // Scroll to current chapter top if not already otherwise scroll to previous top
        val targetChapterIndex = when (currentItemState.itemPos is ReaderItem.Title) {
            true -> currentItemState.itemPos.chapterIndex - 1
            false -> currentItemState.itemPos.chapterIndex
        }
        stop()
        if (!isChapterIndexValid(targetChapterIndex)) {
            coroutineScope.launch {
                manager.currentActiveItemState.value = currentItemState.copy(
                    playState = Utterance.PlayState.FINISHED
                )
                scrolledToTheTop.emit(Unit)
            }
            return
        }
        start()
        coroutineScope.launch {
            if (!isChapterIndexLoaded(targetChapterIndex)) {
                state.isLoadingChapter.value = true
                tryLoadPreviousChapter()
                chapterLoadedFlow
                    .filter { it.chapterIndex == targetChapterIndex }
                    .take(1)
                    .collect()
                state.isLoadingChapter.value = false
            }
            readChapterStartingFromStart(targetChapterIndex)
            scrollToChapterTop.emit(targetChapterIndex)
        }
    }

    private fun setVoice(voiceId: String) {
        val success = manager.trySetVoiceById(id = voiceId)
        if (success) {
            setPreferredVoiceId(voiceId)
            resumeFromCurrentState()
        }
    }

    private fun setVoicePitch(value: Float) {
        val success = manager.trySetVoicePitch(value)
        if (success) {
            setPreferredVoicePitch(value)
            resumeFromCurrentState()
        }
    }

    private fun setVoiceSpeed(value: Float) {
        val success = manager.trySetVoiceSpeed(value)
        if (success) {
            setPreferredVoiceSpeed(value)
            resumeFromCurrentState()
        }
    }

    private fun resumeFromCurrentState() {
        if (!state.isPlaying.value) {
            return
        }
        stop()
        start()
        val state = manager.currentActiveItemState.value
        if (state.itemPos.chapterIndex > 0) {
            coroutineScope.launch {
                readChapterStartingFromChapterItemPosition(
                    chapterIndex = state.itemPos.chapterIndex,
                    chapterItemPosition = state.itemPos.chapterItemPosition
                )
            }
        }
    }

    private fun readChapterNextChunk(
        chapterIndex: Int,
        chapterItemPosition: Int,
        quantity: Int
    ) {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return
        val nextItems = getChapterNextItems(
            itemIndex = itemIndex + 1,
            chapterIndex = chapterIndex,
            quantity = quantity
        )
        if (nextItems.isEmpty()) return
        nextItems.forEach(::speakItem)
    }

    private fun getChapterNextItems(
        itemIndex: Int,
        chapterIndex: Int,
        quantity: Int
    ): List<ReaderItem.Position> {
        return items
            .subList(itemIndex.coerceAtMost(items.lastIndex), items.size)
            .asSequence()
            .filter { it is ReaderItem.Title || it is ReaderItem.Body }
            .filterIsInstance<ReaderItem.Position>()
            .takeWhile { it.chapterIndex == chapterIndex }
            .take(quantity)
            .toList()
    }

    private fun speakItem(item: ReaderItem) {
        when (item) {
            is ReaderItem.Text -> {
                manager.speak(
                    text = item.textToDisplay,
                    textSynthesis = TextSynthesis(
                        itemPos = item,
                        playState = Utterance.PlayState.PLAYING
                    )
                )
            }
            else -> Unit
        }
    }
}


