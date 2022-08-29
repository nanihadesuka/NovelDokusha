package my.noveldokusha.ui.screens.reader.tools

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import my.noveldokusha.tools.TextSynthesis
import my.noveldokusha.tools.TextSynthesisState
import my.noveldokusha.tools.TextToSpeechManager
import my.noveldokusha.tools.VoiceData
import my.noveldokusha.ui.screens.reader.ReaderItem
import my.noveldokusha.ui.screens.reader.TextToSpeechSettingData

class ReaderSpeaker(
    private val textToSpeechManager: TextToSpeechManager,
    private val items: List<ReaderItem>,
    private val coroutineScope: CoroutineScope,
) {
    val currentTextLiveData = textToSpeechManager.currentTextSpeakFlow.asLiveData()
    val currentTextPlaying = textToSpeechManager.currentActiveItemState as State<TextSynthesis>
    val reachedChapterEndFlowChapterIndex = MutableSharedFlow<Int>() // chapter pos
    val startReadingFromFirstVisibleItemFlow = MutableSharedFlow<Unit>()
    val scrollToItemFlowReaderItem = MutableSharedFlow<ReaderItem>()
    val scrollToChapterTopFlowChapterIndex = MutableSharedFlow<Int>()

    val settings = TextToSpeechSettingData(
        isEnabled = mutableStateOf(false),
        isPlaying = mutableStateOf(false),
        availableVoices = textToSpeechManager.availableVoices,
        currentActiveItemState = textToSpeechManager.currentActiveItemState,
        onSelectVoice = ::onSelectVoice,
        playNextChapter = ::playNextChapter,
        playPreviousChapter = ::playPreviousChapter,
        playNextItem = ::playNextItem,
        playPreviousItem = ::playPreviousItem,
        setPlaying = ::setPlaying
    )

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
                startReadingFromFirstVisibleItemFlow.emit(Unit)
            }
        }
    }

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
            scrollToItemFlowReaderItem.emit(items[nextItemIndex])
        }
    }

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
            scrollToItemFlowReaderItem.emit(items[previousItemIndex])
        }
    }

    private fun playNextChapter() {
        val nextChapterIndex = settings.currentActiveItemState.value.chapterIndex + 1
        stop()
        start()
        coroutineScope.launch {
            readChapterStartingFromStart(nextChapterIndex)
            scrollToChapterTopFlowChapterIndex.emit(nextChapterIndex)
        }
    }

    private fun playPreviousChapter() {
        val previousChapterIndex = settings.currentActiveItemState.value.chapterIndex - 1
        stop()
        start()
        coroutineScope.launch {
            readChapterStartingFromStart(previousChapterIndex)
            scrollToChapterTopFlowChapterIndex.emit(previousChapterIndex)
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


