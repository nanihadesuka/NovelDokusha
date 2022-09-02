package my.noveldokusha.ui.screens.reader.tools

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.tools.TextSynthesis
import my.noveldokusha.tools.TextSynthesisState
import my.noveldokusha.tools.TextToSpeechManager
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.screens.reader.ChaptersLoader
import my.noveldokusha.ui.screens.reader.ReaderItem

data class TextToSpeechSettingData(
    val isPlaying: MutableState<Boolean>,
    val isLoadingChapter: MutableState<Boolean>,
    val activeVoice: MutableState<VoiceData?>,
    val availableVoices: SnapshotStateList<VoiceData>,
    val currentActiveItemState: State<TextSynthesis>,
    val setPlaying: (Boolean) -> Unit,
    val playFirstVisibleItem: () -> Unit,
    val playPreviousItem: () -> Unit,
    val playPreviousChapter: () -> Unit,
    val playNextItem: () -> Unit,
    val playNextChapter: () -> Unit,
    val onSelectVoice: (VoiceData) -> Unit,
    val scrollToActiveItem: () -> Unit,
)

class ReaderSpeaker(
    private val textToSpeechManager: TextToSpeechManager,
    private val items: List<ReaderItem>,
    private val coroutineScope: CoroutineScope,
    private val chapterLoadedFlow: Flow<ChaptersLoader.ChapterLoaded>,
    private val scrollToTheTop: MutableSharedFlow<Unit>,
    private val scrollToTheBottom: MutableSharedFlow<Unit>,
    private val isChapterIndexValid: (chapterIndex: Int) -> Boolean,
    private val isChapterIndexLoaded: (chapterIndex: Int) -> Boolean,
    private val loadPreviousChapter: () -> Unit,
    private val loadNextChapter: () -> Unit,
) {
    val currentReaderItem = textToSpeechManager.currentTextSpeakFlow.asLiveData()
    val currentTextPlaying = textToSpeechManager.currentActiveItemState as State<TextSynthesis>
    val reachedChapterEndFlowChapterIndex = MutableSharedFlow<Int>() // chapter pos
    val startReadingFromFirstVisibleItem = MutableSharedFlow<Unit>()
    val scrollToReaderItem = MutableSharedFlow<ReaderItem>()
    val scrollToFirstChapterItemIndex = MutableSharedFlow<Int>()

    val settings = TextToSpeechSettingData(
        isPlaying = mutableStateOf(false),
        isLoadingChapter = mutableStateOf(false),
        activeVoice = textToSpeechManager.activeVoice,
        availableVoices = textToSpeechManager.availableVoices,
        currentActiveItemState = textToSpeechManager.currentActiveItemState,
        onSelectVoice = ::onSelectVoice,
        playFirstVisibleItem = ::playFirstVisibleItem,
        playNextChapter = ::playNextChapter,
        playPreviousChapter = ::playPreviousChapter,
        playNextItem = ::playNextItem,
        playPreviousItem = ::playPreviousItem,
        setPlaying = ::setPlaying,
        scrollToActiveItem = ::scrollToActiveItem
    )

    @Synchronized
    private fun scrollToActiveItem() {
        coroutineScope.launch {
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = settings.currentActiveItemState.value.chapterIndex,
                chapterItemIndex = settings.currentActiveItemState.value.chapterItemIndex,
            )
            if (itemIndex == -1) return@launch
            scrollToReaderItem.emit(items[itemIndex])
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
        val state = textToSpeechManager.currentActiveItemState.value
        if (state.chapterIndex != -1) {
            coroutineScope.launch {
                readChapterStartingFromChapterItemIndex(
                    chapterIndex = state.chapterIndex,
                    chapterItemIndex = state.chapterItemIndex
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
        coroutineScope.launch {
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = settings.currentActiveItemState.value.chapterIndex,
                chapterItemIndex = settings.currentActiveItemState.value.chapterItemIndex,
            )
            if (itemIndex == -1 || itemIndex == items.lastIndex) return@launch
            val nextItemRelativeIndex = items
                .subList(itemIndex + 1, items.size)
                .indexOfFirst { it is ReaderItem.Position }
            if (nextItemRelativeIndex == -1) return@launch
            val nextItemIndex = itemIndex + 1 + nextItemRelativeIndex
            stop()
            start()
            readChapterStartingFromItemIndex(
                itemIndex = nextItemIndex,
                chapterIndex = items[nextItemIndex].chapterIndex
            )
            scrollToReaderItem.emit(items[nextItemIndex])
        }
    }

    @Synchronized
    private fun playPreviousItem() {
        coroutineScope.launch {
            val itemIndex = indexOfReaderItem(
                list = items,
                chapterIndex = settings.currentActiveItemState.value.chapterIndex,
                chapterItemIndex = settings.currentActiveItemState.value.chapterItemIndex,
            )
            if (itemIndex == -1 || itemIndex == 0) return@launch
            val previousItemRelativeIndex = items
                .subList(0, itemIndex)
                .asReversed()
                .indexOfFirst { it is ReaderItem.Position }
            if (previousItemRelativeIndex == -1) return@launch
            val previousItemIndex = itemIndex - 1 - previousItemRelativeIndex
            stop()
            start()
            readChapterStartingFromItemIndex(
                itemIndex = previousItemIndex,
                chapterIndex = items[previousItemIndex].chapterIndex
            )
            scrollToReaderItem.emit(items[previousItemIndex])
        }
    }

    @Synchronized
    private fun playNextChapter() {
        val nextChapterIndex = settings.currentActiveItemState.value.chapterIndex + 1
        stop()
        if (!isChapterIndexValid(nextChapterIndex)) {
            coroutineScope.launch {
                val state = settings.currentActiveItemState.value
                val item = items.findLast {
                    it is ReaderItem.Position && it.chapterIndex == state.chapterIndex
                }
                val chapterItemIndex = (item as? ReaderItem.Position)?.chapterItemIndex ?: 0
                textToSpeechManager.currentActiveItemState.value = state.copy(
                    state = TextSynthesisState.FINISHED,
                    chapterItemIndex = chapterItemIndex
                )
                scrollToTheBottom.emit(Unit)
            }
            return
        }
        start()
        coroutineScope.launch {
            if (!isChapterIndexLoaded(nextChapterIndex)) {
                settings.isLoadingChapter.value = true
                loadNextChapter()
                chapterLoadedFlow
                    .filter { it.chapterIndex == nextChapterIndex }
                    .take(1)
                    .collect()
                settings.isLoadingChapter.value = false
            }
            readChapterStartingFromStart(nextChapterIndex)
            scrollToFirstChapterItemIndex.emit(nextChapterIndex)
        }
    }

    @Synchronized
    private fun playPreviousChapter() {
        val currentChapterIndex = settings.currentActiveItemState.value.chapterIndex
        val currentChapterItemIndex = settings.currentActiveItemState.value.chapterItemIndex
        // Scroll to current chapter top if not already otherwise scroll to previous top
        val targetChapterIndex = when (currentChapterItemIndex == 0) {
            true -> currentChapterIndex - 1
            false -> currentChapterIndex
        }

        stop()
        if (!isChapterIndexValid(targetChapterIndex)) {
            coroutineScope.launch {
                textToSpeechManager.currentActiveItemState.value =
                    settings.currentActiveItemState.value.copy(state = TextSynthesisState.FINISHED)
                scrollToTheTop.emit(Unit)
            }
            return
        }
        start()
        coroutineScope.launch {
            if (!isChapterIndexLoaded(targetChapterIndex)) {
                settings.isLoadingChapter.value = true
                loadPreviousChapter()
                chapterLoadedFlow
                    .filter { it.chapterIndex == targetChapterIndex }
                    .take(1)
                    .collect()
                settings.isLoadingChapter.value = false
            }
            readChapterStartingFromStart(targetChapterIndex)
            scrollToFirstChapterItemIndex.emit(targetChapterIndex)
        }
    }


    private fun onSelectVoice(voiceData: VoiceData) {
        textToSpeechManager.setVoiceById(id = voiceData.id)
        if (!settings.isPlaying.value) {
            return
        }
        stop()
        start()
        val state = textToSpeechManager.currentActiveItemState.value
        if (state.chapterIndex != -1) {
            coroutineScope.launch {
                readChapterStartingFromChapterItemIndex(
                    chapterIndex = state.chapterIndex,
                    chapterItemIndex = state.chapterItemIndex
                )
            }
        }
    }

    private val halfBuffer = 2
    private var updateJob: Job? = null

    @Synchronized
    fun start() {
        settings.isPlaying.value = true
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            textToSpeechManager
                .currentTextSpeakFlow
                .filter { it.state == TextSynthesisState.FINISHED }
                .collect {
                    withContext(Dispatchers.Main) {
                        when (textToSpeechManager.queueList.size) {
                            halfBuffer -> {
                                val lastItem = textToSpeechManager
                                    .queueList
                                    .asSequence()
                                    .last().value
                                readChapterNextChunk(
                                    chapterIndex = lastItem.chapterIndex,
                                    chapterItemIndex = lastItem.chapterItemIndex,
                                    quantity = halfBuffer
                                )
                            }
                            0 -> {
                                launch { reachedChapterEndFlowChapterIndex.emit(it.chapterIndex) }
                            }
                            else -> Unit
                        }
                    }
                }
        }
    }


    @Synchronized
    fun stop() {
        settings.isPlaying.value = false
        updateJob?.cancel()
        textToSpeechManager.stop()
    }

    suspend fun readChapterStartingFromStart(
        chapterIndex: Int
    ) {
        readChapterStartingFromChapterItemIndex(
            chapterIndex = chapterIndex,
            chapterItemIndex = 0
        )
    }

    suspend fun readChapterStartingFromChapterItemIndex(
        chapterIndex: Int,
        chapterItemIndex: Int,
    ) {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterIndex = chapterIndex,
            chapterItemIndex = chapterItemIndex
        )
        if (itemIndex == -1) {
            reachedChapterEndFlowChapterIndex.emit(chapterIndex)
            return
        }
        readChapterStartingFromItemIndex(
            itemIndex = itemIndex,
            chapterIndex = chapterIndex
        )
    }

    suspend fun readChapterStartingFromItemIndex(
        itemIndex: Int,
        chapterIndex: Int,
    ) {
        val nextItems = getChapterNextItems(
            itemIndex = itemIndex,
            chapterIndex = chapterIndex,
            quantity = halfBuffer * 2
        )

        if (nextItems.isEmpty()) {
            reachedChapterEndFlowChapterIndex.emit(chapterIndex)
            return
        }

        val firstItem = nextItems.first()
        if (firstItem is ReaderItem.Position) {
            textToSpeechManager.setCurrentSpeakState(
                TextSynthesis(
                    chapterItemIndex = (firstItem as ReaderItem.Position).chapterItemIndex,
                    chapterIndex = firstItem.chapterIndex,
                    state = TextSynthesisState.LOADING
                )
            )
        }

        nextItems.forEach(::speakItem)
    }

    private fun readChapterNextChunk(
        chapterIndex: Int,
        chapterItemIndex: Int,
        quantity: Int
    ) {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterIndex = chapterIndex,
            chapterItemIndex = chapterItemIndex
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
    ): List<ReaderItem> {
        return items
            .subList(itemIndex.coerceAtMost(items.lastIndex), items.size)
            .asSequence()
            .filter { it is ReaderItem.Title || it is ReaderItem.Body }
            .takeWhile { it.chapterIndex == chapterIndex }
            .take(quantity)
            .toList()
    }

    private fun speakItem(item: ReaderItem) {
        when (item) {
            is ReaderItem.Title -> {
                textToSpeechManager.speak(
                    text = item.textToDisplay,
                    textSynthesis = TextSynthesis(
                        chapterItemIndex = item.chapterItemIndex,
                        chapterIndex = item.chapterIndex,
                        state = TextSynthesisState.PLAYING
                    )
                )
            }
            is ReaderItem.Body -> {
                textToSpeechManager.speak(
                    text = item.textToDisplay,
                    textSynthesis = TextSynthesis(
                        chapterItemIndex = item.chapterItemIndex,
                        chapterIndex = item.chapterIndex,
                        state = TextSynthesisState.PLAYING
                    )
                )
            }
            else -> Unit
        }
    }
}


