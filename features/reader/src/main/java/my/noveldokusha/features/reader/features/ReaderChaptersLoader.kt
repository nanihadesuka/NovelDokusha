package my.noveldokusha.features.reader.features

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.core.Response
import my.noveldokusha.features.reader.ReaderRepository
import my.noveldokusha.features.reader.domain.ChapterLoaded
import my.noveldokusha.features.reader.domain.ChapterState
import my.noveldokusha.features.reader.domain.ChapterStats
import my.noveldokusha.features.reader.domain.ChapterUrl
import my.noveldokusha.features.reader.domain.InitialPositionChapter
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.domain.ReaderState
import my.noveldokusha.features.reader.domain.ReadingChapterPosStats
import my.noveldokusha.features.reader.domain.indexOfReaderItem
import my.noveldokusha.features.reader.tools.textToItemsConverter
import my.noveldokusha.features.reader.ui.ReaderViewHandlersActions
import my.noveldokusha.tooling.local_database.tables.Chapter
import kotlin.coroutines.CoroutineContext

internal class ReaderChaptersLoader(
    private val readerRepository: ReaderRepository,
    private val translatorTranslateOrNull: suspend (text: String) -> String?,
    private val translatorIsActive: () -> Boolean,
    private val translatorSourceLanguageOrNull: () -> String?,
    private val translatorTargetLanguageOrNull: () -> String?,
    private val bookUrl: String,
    val orderedChapters: List<Chapter>,
    @Volatile var readerState: ReaderState,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate

    private sealed interface LoadChapter {
        enum class Type { RestartInitial, Initial, Previous, Next }
        data class RestartInitialChapter(val state: ChapterState) : LoadChapter
        data class Initial(val chapterIndex: Int) : LoadChapter
        data object Previous : LoadChapter
        data object Next : LoadChapter
    }

    val chaptersStats = mutableMapOf<ChapterUrl, ChapterStats>()
    val loadedChapters = mutableSetOf<ChapterUrl>()
    val chapterLoadedFlow = MutableSharedFlow<ChapterLoaded>()
    private val items: SnapshotStateList<ReaderItem> = mutableStateListOf<ReaderItem>()
    private val loaderQueue = mutableSetOf<LoadChapter.Type>()
    private val chapterLoaderFlow = MutableSharedFlow<LoadChapter>()

    init {
        startChapterLoaderWatcher()
    }

    fun getItems(): List<ReaderItem> = items
    fun getItemContext(itemIndex: Int, chapterUrl: String): ReadingChapterPosStats? {
        val item = items.getOrNull(itemIndex) ?: return null
        if (item !is ReaderItem.Position) return null
        val chapterStats = chaptersStats[chapterUrl] ?: return null
        return ReadingChapterPosStats(
            chapterIndex = item.chapterIndex,
            chapterCount = orderedChapters.size,
            chapterItemPosition = item.chapterItemPosition,
            chapterItemsCount = chapterStats.itemsCount,
            chapterTitle = chapterStats.chapter.title,
            chapterUrl = chapterStats.chapter.url,
        )
    }

    fun getItemContext(chapterIndex: Int, chapterItemPosition: Int): ReadingChapterPosStats? {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        val item = items.getOrNull(itemIndex) ?: return null
        if (item !is ReaderItem.Position) return null
        val chapterStats = chaptersStats[item.chapterUrl] ?: return null
        return ReadingChapterPosStats(
            chapterIndex = chapterIndex,
            chapterCount = orderedChapters.size,
            chapterItemPosition = item.chapterItemPosition,
            chapterItemsCount = chapterStats.itemsCount,
            chapterTitle = chapterStats.chapter.title,
            chapterUrl = chapterStats.chapter.url,
        )
    }


    fun isLastChapter(chapterIndex: Int): Boolean = chapterIndex == orderedChapters.lastIndex
    fun isChapterIndexLoaded(chapterIndex: Int): Boolean {
        return orderedChapters.getOrNull(chapterIndex)?.url
            ?.let { loadedChapters.contains(it) }
            ?: false
    }

    fun isChapterIndexValid(chapterIndex: Int) =
        0 <= chapterIndex && chapterIndex < orderedChapters.size

    fun isChapterIndexTheLast(chapterIndex: Int) =
        chapterIndex != -1 && chapterIndex == orderedChapters.lastIndex

    @Synchronized
    fun tryLoadInitial(chapterIndex: Int) {
        if (LoadChapter.Type.Initial in loaderQueue) return
        loaderQueue.add(LoadChapter.Type.Initial)
        launch {
            chapterLoaderFlow.emit(LoadChapter.Initial(chapterIndex = chapterIndex))
        }
    }

    @Synchronized
    fun tryLoadRestartedInitial(chapterLastState: ChapterState) {
        if (LoadChapter.Type.RestartInitial in loaderQueue) return
        loaderQueue.add(LoadChapter.Type.RestartInitial)
        launch {
            chapterLoaderFlow.emit(LoadChapter.RestartInitialChapter(state = chapterLastState))
        }
    }

    @Synchronized
    fun tryLoadPrevious() {
        if (LoadChapter.Type.Previous in loaderQueue) return
        loaderQueue.add(LoadChapter.Type.Previous)
        launch { chapterLoaderFlow.emit(LoadChapter.Previous) }
    }

    @Synchronized
    fun tryLoadNext() {
        if (LoadChapter.Type.Next in loaderQueue) return
        loaderQueue.add(LoadChapter.Type.Next)
        launch { chapterLoaderFlow.emit(LoadChapter.Next) }
    }

    fun reload() {
        coroutineContext.cancelChildren()
        loaderQueue.clear()
        launch(Dispatchers.Main.immediate) {
            items.clear()
            loadedChapters.clear()
            readerState = ReaderState.INITIAL_LOAD
            startChapterLoaderWatcher()
        }
    }

    private fun startChapterLoaderWatcher() {
        launch {
            chapterLoaderFlow
                .collect {
                    when (it) {
                        is LoadChapter.Initial -> {
                            loadInitialChapter(chapterIndex = it.chapterIndex)
                            removeQueueItem(LoadChapter.Type.Initial)
                        }
                        is LoadChapter.Next -> {
                            loadNextChapter()
                            removeQueueItem(LoadChapter.Type.Next)
                        }
                        is LoadChapter.Previous -> {
                            loadPreviousChapter()
                            removeQueueItem(LoadChapter.Type.Previous)
                        }
                        is LoadChapter.RestartInitialChapter -> {
                            loadRestartedInitialChapter(chapterLastState = it.state)
                            removeQueueItem(LoadChapter.Type.RestartInitial)
                        }
                    }
                }
        }
    }

    @Synchronized
    private fun removeQueueItem(type: LoadChapter.Type) {
        loaderQueue.remove(type)
    }

    private suspend fun loadRestartedInitialChapter(
        chapterLastState: ChapterState
    ) = withContext(Dispatchers.Main.immediate) {
        readerState = ReaderState.INITIAL_LOAD
        items.clear()

        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(it)
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
            }
        }
        val index = orderedChapters.indexOfFirst { it.url == chapterLastState.chapterUrl }
        if (index == -1) {
            readerViewHandlersActions.doShowInvalidChapterDialog()
            return@withContext
        }

        addChapter(
            chapterIndex = index,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = readerViewHandlersActions::doMaintainStartPosition,
        )

        readerViewHandlersActions.doSetInitialPosition(
            InitialPositionChapter(
                chapterIndex = index,
                chapterItemPosition = chapterLastState.chapterItemPosition,
                chapterItemOffset = chapterLastState.offset
            )
        )

        readerState = ReaderState.IDLE

        chapterLoadedFlow.emit(
            ChapterLoaded(
                chapterIndex = index,
                type = ChapterLoaded.Type.Initial
            )
        )
    }

    private suspend fun loadInitialChapter(
        chapterIndex: Int
    ) = withContext(Dispatchers.Main.immediate) {
        readerState = ReaderState.INITIAL_LOAD
        items.clear()

        val validIndex = chapterIndex >= 0 && chapterIndex < orderedChapters.size
        if (!validIndex) {
            readerViewHandlersActions.doShowInvalidChapterDialog()
            return@withContext
        }

        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(it)
            }
        }

        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
            }
        }

        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
            }
        }

        addChapter(
            chapterIndex = chapterIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = readerViewHandlersActions::doMaintainStartPosition,
        )


        val chapter = orderedChapters[chapterIndex]
        val initialPosition = readerRepository.getInitialChapterItemPosition(
            bookUrl = bookUrl,
            chapterIndex = chapter.position,
            chapter = chapter,
        )

        readerViewHandlersActions.doSetInitialPosition(initialPosition)
        Log.w("INFOMESS", "initialPosition: $initialPosition")

        chapterLoadedFlow.emit(
            ChapterLoaded(
                chapterIndex = chapterIndex,
                type = ChapterLoaded.Type.Initial
            )
        )

        readerState = ReaderState.IDLE
    }

    private suspend fun loadPreviousChapter() = withContext(Dispatchers.Main.immediate) {
        readerState = ReaderState.LOADING

        val firstItem = items.firstOrNull()!!
        if (firstItem is ReaderItem.BookStart) {
            readerState = ReaderState.IDLE
            return@withContext
        }

        var listIndex = 0
        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(listIndex, it)
                listIndex += 1
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(listIndex, it)
                listIndex += it.size
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                val isRemoved = items.remove(it)
                if (isRemoved) {
                    listIndex -= 1
                }
            }
        }

        val previousIndex = firstItem.chapterIndex - 1
        if (previousIndex < 0) {
            readerViewHandlersActions.doMaintainLastVisiblePosition {
                insert(ReaderItem.BookStart(chapterIndex = previousIndex))
            }
            readerState = ReaderState.IDLE
            return@withContext
        }

        addChapter(
            chapterIndex = previousIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = readerViewHandlersActions::doMaintainLastVisiblePosition,
        )

        chapterLoadedFlow.emit(
            ChapterLoaded(
                chapterIndex = previousIndex,
                type = ChapterLoaded.Type.Previous
            )
        )

        readerState = ReaderState.IDLE
    }

    private suspend fun loadNextChapter() = withContext(Dispatchers.Main.immediate) {
        readerState = ReaderState.LOADING

        val lastItem = items.lastOrNull()!!
        if (lastItem is ReaderItem.BookEnd) {
            readerState = ReaderState.IDLE
            return@withContext
        }

        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(it)
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
            }
        }

        val nextIndex = lastItem.chapterIndex + 1
        if (nextIndex >= orderedChapters.size) {
            insert(ReaderItem.BookEnd(chapterIndex = nextIndex))
            readerState = ReaderState.IDLE
            return@withContext
        }

        addChapter(
            chapterIndex = nextIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
        )

        chapterLoadedFlow.emit(
            ChapterLoaded(
                chapterIndex = nextIndex,
                type = ChapterLoaded.Type.Next
            )
        )

        readerState = ReaderState.IDLE
    }

    private suspend fun addChapter(
        chapterIndex: Int,
        insert: suspend (ReaderItem) -> Unit,
        insertAll: suspend (Collection<ReaderItem>) -> Unit,
        remove: suspend (ReaderItem) -> Unit,
        maintainPosition: suspend (suspend () -> Unit) -> Unit = { it() },
    ) = withContext(Dispatchers.Default) {
        val chapter = orderedChapters[chapterIndex]
        val itemProgressBar = ReaderItem.Progressbar(chapterIndex = chapterIndex)
        var chapterItemPosition = 0
        val itemTitle = ReaderItem.Title(
            chapterUrl = chapter.url,
            chapterIndex = chapterIndex,
            text = chapter.title,
            chapterItemPosition = chapterItemPosition,
        ).copy(
            textTranslated = translatorTranslateOrNull(chapter.title) ?: chapter.title
        )
        chapterItemPosition += 1

        withContext(Dispatchers.Main.immediate) {
            insert(ReaderItem.Divider(chapterIndex = chapterIndex))
            insert(itemTitle)
            insert(itemProgressBar)
        }

        when (val res = readerRepository.downloadChapter(chapter.url)) {
            is Response.Success -> {
                // Split chapter text into items
                val itemsOriginal = textToItemsConverter(
                    chapterUrl = chapter.url,
                    chapterIndex = chapterIndex,
                    chapterItemPositionDisplacement = chapterItemPosition,
                    text = res.data,
                )
                chapterItemPosition += itemsOriginal.size

                val itemTranslationAttribution = if (translatorIsActive()) {
                    ReaderItem.GoogleTranslateAttribution(chapterIndex = chapterIndex)
                } else null

                val itemTranslating = if (translatorIsActive()) {
                    ReaderItem.Translating(
                        chapterIndex = chapterIndex,
                        sourceLang = translatorSourceLanguageOrNull() ?: "",
                        targetLang = translatorTargetLanguageOrNull() ?: "",
                    )
                } else null

                if (itemTranslating != null) {
                    maintainPosition {
                        insert(itemTranslating)
                    }
                }

                // Translate if necessary
                val items = when {
                    translatorIsActive() -> itemsOriginal.map {
                        if (it is ReaderItem.Body) {
                            it.copy(textTranslated = translatorTranslateOrNull(it.text))
                        } else it
                    }
                    else -> itemsOriginal
                }

                withContext(Dispatchers.Main.immediate) {
                    chaptersStats[chapter.url] = ChapterStats(
                        chapter = chapter,
                        itemsCount = items.size,
                        orderedChaptersIndex = chapterIndex
                    )
                }

                maintainPosition {
//                    withContext(Dispatchers.Main.immediate) {
//                        itemProgressBar.visible.value = false
//                        itemTranslating?.visible?.value = false
//                    }
                    remove(itemProgressBar)
                    itemTranslating?.let {
                        remove(it)
                    }
                    itemTranslationAttribution?.let {
                        insert(it)
                    }
                    insertAll(items)
                    insert(ReaderItem.Divider(chapterIndex = chapterIndex))
                }
                withContext(Dispatchers.Main.immediate) {
                    loadedChapters.add(chapter.url)
                }
            }
            is Response.Error -> {
                withContext(Dispatchers.Main.immediate) {
                    chaptersStats[chapter.url] = ChapterStats(
                        chapter = chapter,
                        itemsCount = 1,
                        orderedChaptersIndex = chapterIndex
                    )
                }
                maintainPosition {
//                    withContext(Dispatchers.Main.immediate) {
//                        itemProgressBar.visible.value = false
//                    }
                    remove(itemProgressBar)
                    insert(ReaderItem.Error(chapterIndex = chapterIndex, text = res.message))
                }
                withContext(Dispatchers.Main.immediate) {
                    loadedChapters.add(chapter.url)
                }
            }
        }
    }
}
