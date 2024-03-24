package my.noveldokusha.features.reader.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Response
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.features.reader.ChapterState
import my.noveldokusha.features.reader.ChapterStats
import my.noveldokusha.features.reader.ChapterUrl
import my.noveldokusha.features.reader.ReaderItem
import my.noveldokusha.features.reader.ReaderState
import my.noveldokusha.features.reader.ReadingChapterPosStats
import my.noveldokusha.features.reader.tools.InitialPositionChapter
import my.noveldokusha.features.reader.tools.getInitialChapterItemPosition
import my.noveldokusha.features.reader.tools.indexOfReaderItem
import my.noveldokusha.features.reader.tools.textToItemsConverter
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ReaderChaptersLoader(
    private val appRepository: AppRepository,
    private val translatorTranslateOrNull: suspend (text: String) -> String?,
    private val translatorIsActive: () -> Boolean,
    private val translatorSourceLanguageOrNull: () -> String?,
    private val translatorTargetLanguageOrNull: () -> String?,
    private val bookUrl: String,
    val orderedChapters: List<Chapter>,
    @Volatile var readerState: ReaderState,
    val forceUpdateListViewState: suspend () -> Unit,
    val maintainLastVisiblePosition: suspend (suspend () -> Unit) -> Unit,
    val maintainStartPosition: suspend (suspend () -> Unit) -> Unit,
    val setInitialPosition: suspend (InitialPositionChapter) -> Unit,
    val showInvalidChapterDialog: suspend () -> Unit,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate

    data class ChapterLoaded(val chapterIndex: Int, val type: Type) {
        enum class Type { Previous, Next, Initial }
    }

    private sealed interface LoadChapter {
        enum class Type { RestartInitial, Initial, Previous, Next }
        data class RestartInitialChapter(val state: ChapterState) : LoadChapter
        data class Initial(val chapterIndex: Int) : LoadChapter
        object Previous : LoadChapter
        object Next : LoadChapter
    }

    val chaptersStats = mutableMapOf<ChapterUrl, ChapterStats>()
    val loadedChapters = mutableSetOf<ChapterUrl>()
    val chapterLoadedFlow = MutableSharedFlow<ChapterLoaded>()
    private val items: MutableList<ReaderItem> = ArrayList()
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
            forceUpdateListViewState()
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
        forceUpdateListViewState()

        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(it)
                forceUpdateListViewState()
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
                forceUpdateListViewState()
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
                forceUpdateListViewState()
            }
        }
        val index = orderedChapters.indexOfFirst { it.url == chapterLastState.chapterUrl }
        if (index == -1) {
            showInvalidChapterDialog()
            return@withContext
        }

        addChapter(
            chapterIndex = index,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = maintainStartPosition,
        )

        forceUpdateListViewState()
        setInitialPosition(
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
        forceUpdateListViewState()

        val validIndex = chapterIndex >= 0 && chapterIndex < orderedChapters.size
        if (!validIndex) {
            showInvalidChapterDialog()
            return@withContext
        }

        val insert: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.add(it)
                forceUpdateListViewState()
            }
        }

        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
                forceUpdateListViewState()
            }
        }

        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
                forceUpdateListViewState()
            }
        }

        addChapter(
            chapterIndex = chapterIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = maintainStartPosition,
        )


        val chapter = orderedChapters[chapterIndex]
        val initialPosition = getInitialChapterItemPosition(
            appRepository = appRepository,
            bookUrl = bookUrl,
            chapterIndex = chapter.position,
            chapter = chapter,
        )

        forceUpdateListViewState()
        setInitialPosition(initialPosition)

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
                forceUpdateListViewState()
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(listIndex, it)
                listIndex += it.size
                forceUpdateListViewState()
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                val isRemoved = items.remove(it)
                if (isRemoved) {
                    listIndex -= 1
                }
                forceUpdateListViewState()
            }
        }

        val previousIndex = firstItem.chapterIndex - 1
        if (previousIndex < 0) {
            maintainLastVisiblePosition {
                insert(ReaderItem.BookStart(chapterIndex = previousIndex))
                forceUpdateListViewState()
            }
            readerState = ReaderState.IDLE
            return@withContext
        }

        addChapter(
            chapterIndex = previousIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = maintainLastVisiblePosition,
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
                forceUpdateListViewState()
            }
        }
        val insertAll: suspend (Collection<ReaderItem>) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.addAll(it)
                forceUpdateListViewState()
            }
        }
        val remove: suspend (ReaderItem) -> Unit = {
            withContext(Dispatchers.Main.immediate) {
                items.remove(it)
                forceUpdateListViewState()
            }
        }

        val nextIndex = lastItem.chapterIndex + 1
        if (nextIndex >= orderedChapters.size) {
            insert(ReaderItem.BookEnd(chapterIndex = nextIndex))
            forceUpdateListViewState()
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

        maintainPosition {
            insert(ReaderItem.Divider(chapterIndex = chapterIndex))
            insert(itemTitle)
            insert(itemProgressBar)
            forceUpdateListViewState()
        }

        when (val res = appRepository.chapterBody.fetchBody(chapter.url)) {
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
                        forceUpdateListViewState()
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
                    remove(itemProgressBar)
                    itemTranslating?.let {
                        remove(it)
                    }
                    itemTranslationAttribution?.let {
                        insert(it)
                    }
                    insertAll(items)
                    insert(ReaderItem.Divider(chapterIndex = chapterIndex))
                    forceUpdateListViewState()
                }
                withContext(Dispatchers.Main.immediate) {
                    loadedChapters.add(chapter.url)
                }
            }
            is Response.Error -> {
                Timber.d(res.exception)
                withContext(Dispatchers.Main.immediate) {
                    chaptersStats[chapter.url] = ChapterStats(
                        chapter = chapter,
                        itemsCount = 1,
                        orderedChaptersIndex = chapterIndex
                    )
                }
                maintainPosition {
                    remove(itemProgressBar)
                    insert(ReaderItem.Error(chapterIndex = chapterIndex, text = res.message))
                    forceUpdateListViewState()
                }
                withContext(Dispatchers.Main.immediate) {
                    loadedChapters.add(chapter.url)
                }
            }
        }
    }
}