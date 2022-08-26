package my.noveldokusha.ui.screens.reader.tools

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
    val currentTextPlaying get() = textToSpeechManager.currentActiveItemState.value
    val reachedChapterEndFlowChapterIndex = MutableSharedFlow<Int>() // chapter pos
    val startReadingFromFirstVisibleItemFlow = MutableSharedFlow<Unit>()

    val settings = TextToSpeechSettingData(
        isEnabled = mutableStateOf(false),
        isPlaying = mutableStateOf(false),
        availableVoices = textToSpeechManager.availableVoices,
        currentActiveItemState = textToSpeechManager.currentActiveItemState,
        onSelectVoice = ::onSelectVoice,
        playNextChapter = {},
        playNextItem = {},
        playPreviousChapter = {},
        playPreviousItem = {},
        setPlaying = here@{ playing ->
            if (!playing) {
                stop()
                return@here
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
    )

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
            chapterIndex
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


