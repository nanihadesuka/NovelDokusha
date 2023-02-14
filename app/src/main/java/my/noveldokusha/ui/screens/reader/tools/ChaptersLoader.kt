package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import my.noveldokusha.data.Response
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.repository.Repository
import my.noveldokusha.ui.screens.reader.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ChaptersLoader(
    private val repository: Repository,
    private val translateOrNull: suspend (text: String) -> String?,
    private val translationIsActive: () -> Boolean,
    private val translationSourceLanguageOrNull: () -> String?,
    private val translationTargetLanguageOrNull: () -> String?,
    private val bookUrl: String,
    val orderedChapters: List<Chapter>,
    @Volatile var readerState: ReaderState,
    val forceUpdateListViewState: suspend () -> Unit,
    val maintainLastVisiblePosition: suspend (suspend () -> Unit) -> Unit,
    val maintainStartPosition: suspend (suspend () -> Unit) -> Unit,
    val setInitialPosition: suspend (ItemPosition) -> Unit,
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

    val chaptersStats = mutableMapOf<String, ChapterStats>()
    val loadedChapters = mutableSetOf<String>()
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
            chapterPosition = item.chapterPosition,
            chapterCount = orderedChapters.size,
            chapterItemPosition = item.chapterItemPosition,
            chapterItemsCount = chapterStats.itemsCount,
            chapterTitle = chapterStats.chapter.title
        )
    }
    fun getItemContext(chapterPosition: Int, chapterItemPosition: Int): ReadingChapterPosStats? {
        val itemIndex = indexOfReaderItem(
            list = items,
            chapterPosition = chapterPosition,
            chapterItemPosition = chapterItemPosition
        )
        val item = items.getOrNull(itemIndex) ?: return null
        if (item !is ReaderItem.Position) return null
        val chapterStats = chaptersStats[item.chapterUrl] ?: return null
        return ReadingChapterPosStats(
            chapterPosition = item.chapterPosition,
            chapterCount = orderedChapters.size,
            chapterItemPosition = item.chapterItemPosition,
            chapterItemsCount = chapterStats.itemsCount,
            chapterTitle = chapterStats.chapter.title
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
            index = index,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = maintainStartPosition,
        )

        forceUpdateListViewState()
        setInitialPosition(
            ItemPosition(
                chapterIndex = index,
                chapterItemIndex = chapterLastState.chapterItemIndex,
                itemOffset = chapterLastState.offset
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
            index = chapterIndex,
            insert = insert,
            insertAll = insertAll,
            remove = remove,
            maintainPosition = maintainStartPosition,
        )

        val initialPosition = getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapterIndex = chapterIndex,
            chapter = orderedChapters[chapterIndex],
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

        val previousIndex = firstItem.chapterPosition - 1
        if (previousIndex < 0) {
            maintainLastVisiblePosition {
                insert(
                    ReaderItem.BookStart(
                        chapterUrl = firstItem.chapterUrl,
                        chapterPosition = previousIndex
                    )
                )
                forceUpdateListViewState()
            }
            readerState = ReaderState.IDLE
            return@withContext
        }

        addChapter(
            index = previousIndex,
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
        val nextIndex = lastItem.chapterPosition + 1

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

        if (nextIndex >= orderedChapters.size) {
            insert(
                ReaderItem.BookEnd(
                    chapterUrl = lastItem.chapterUrl,
                    chapterPosition = nextIndex,
                )
            )
            forceUpdateListViewState()
            readerState = ReaderState.IDLE
            return@withContext
        }

        addChapter(
            index = nextIndex,
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
        index: Int,
        insert: suspend (ReaderItem) -> Unit,
        insertAll: suspend (Collection<ReaderItem>) -> Unit,
        remove: suspend (ReaderItem) -> Unit,
        maintainPosition: suspend (suspend () -> Unit) -> Unit = { it() },
    ) = withContext(Dispatchers.Default) {
        val chapter = orderedChapters[index]
        val itemProgressBar = ReaderItem.Progressbar(
            chapterUrl = chapter.url,
            chapterPosition = index,
        )
        var chapterItemIndex = 0
        val itemTitle = ReaderItem.Title(
            chapterUrl = chapter.url,
            chapterPosition = index,
            text = chapter.title,
            chapterItemPosition = chapterItemIndex,
        ).copy(
            textTranslated = translateOrNull(chapter.title) ?: chapter.title
        )
        chapterItemIndex += 1

        maintainPosition {
            insert(
                ReaderItem.Divider(
                    chapterUrl = chapter.url,
                    chapterPosition = index,
                )
            )
            insert(itemTitle)
            insert(itemProgressBar)
            forceUpdateListViewState()
        }

        when (val res = repository.chapterBody.fetchBody(chapter.url)) {
            is Response.Success -> {
                // Split chapter text into items
                val itemsOriginal = textToItemsConverter(
                    chapterUrl = chapter.url,
                    chapterPos = index,
                    initialChapterItemIndex = chapterItemIndex,
                    text = res.data,
                )
                chapterItemIndex += itemsOriginal.size

                val itemTranslationAttribution = if (translationIsActive()) {
                    ReaderItem.GoogleTranslateAttribution(
                        chapterUrl = chapter.url,
                        chapterPosition = index,
                    )
                } else null

                val itemTranslating = if (translationIsActive()) {
                    ReaderItem.Translating(
                        chapterUrl = chapter.url,
                        chapterPosition = index,
                        sourceLang = translationSourceLanguageOrNull() ?: "",
                        targetLang = translationTargetLanguageOrNull() ?: "",
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
                    translationIsActive() -> itemsOriginal.map {
                        if (it is ReaderItem.Body) {
                            it.copy(textTranslated = translateOrNull(it.text))
                        } else it
                    }
                    else -> itemsOriginal
                }

                withContext(Dispatchers.Main.immediate) {
                    chaptersStats[chapter.url] = ChapterStats(
                        chapter = chapter,
                        itemsCount = items.size,
                        orderedChaptersIndex = index
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
                    insert(
                        ReaderItem.Divider(
                            chapterUrl = chapter.url,
                            chapterPosition = index,
                        )
                    )
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
                        orderedChaptersIndex = index
                    )
                }
                maintainPosition {
                    remove(itemProgressBar)
                    insert(
                        ReaderItem.Error(
                            chapterUrl = chapter.url,
                            chapterPosition = index,
                            text = res.message,
                        )
                    )
                    forceUpdateListViewState()
                }
                withContext(Dispatchers.Main.immediate) {
                    loadedChapters.add(chapter.url)
                }
            }
        }
    }
}