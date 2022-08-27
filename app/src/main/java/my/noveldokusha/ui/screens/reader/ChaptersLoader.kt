package my.noveldokusha.ui.screens.reader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.network.Response
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation
import my.noveldokusha.ui.screens.reader.tools.textToItemsConverter
import kotlin.coroutines.CoroutineContext

class ChaptersLoader(
    private val repository: Repository,
    private val liveTranslation: LiveTranslation,
    val items: MutableList<ReaderItem>,
    val orderedChapters: List<Chapter>,
    var readerState: ReaderViewModel.ReaderState,
    val forceUpdateListViewState: () -> Unit,
    val maintainLastVisiblePosition: (() -> Unit) -> Unit,
    val maintainStartPosition: (() -> Unit) -> Unit,
    val showInvalidChapterDialog: () -> Unit,
) : CoroutineScope {


    sealed interface InitialLoadCompleted {
        data class Initial(val chapterIndex: Int) : InitialLoadCompleted
        data class Reloaded(
            val chapterIndex: Int,
            val chapterItemIndex: Int,
            val chapterItemOffset: Int
        ) : InitialLoadCompleted
    }

    data class ChapterLoaded(val chapterIndex: Int, val type: Type) {
        enum class Type { Previous, Next, Initial }
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate

    fun isLastChapter(index: Int) = index == orderedChapters.lastIndex

    val chaptersStats = mutableMapOf<String, ReaderViewModel.ChapterStats>()
    val loadedChapters = mutableSetOf<String>()

    val initialChapterLoadedFlow = MutableSharedFlow<InitialLoadCompleted>()
    val chapterLoadedFlow = MutableSharedFlow<ChapterLoaded>()

    fun reload() {
        coroutineContext.cancelChildren()
        items.clear()
        loadedChapters.clear()
        readerState = ReaderViewModel.ReaderState.INITIAL_LOAD
    }

    fun loadRestartedInitialChapter(chapterLastState: ReaderViewModel.ChapterState): Boolean {
        readerState = ReaderViewModel.ReaderState.INITIAL_LOAD
        items.clear()
        forceUpdateListViewState()

        val insert: (ReaderItem) -> Unit = { items.add(it) }
        val insertAll: (Collection<ReaderItem>) -> Unit = { items.addAll(it) }
        val remove: (ReaderItem) -> Unit = { items.remove(it) }

        val index = orderedChapters.indexOfFirst { it.url == chapterLastState.chapterUrl }
        if (index == -1) {
            showInvalidChapterDialog()
            return false
        }

        launch {
            addChapter(
                index = index,
                insert = insert,
                insertAll = insertAll,
                remove = remove,
                maintainPosition = maintainStartPosition,
                onCompletion = {
                    initialChapterLoadedFlow.emit(InitialLoadCompleted.Reloaded(
                        chapterIndex = index,
                        chapterItemIndex = chapterLastState.chapterItemIndex,
                        chapterItemOffset = chapterLastState.offset
                    ))
                    chapterLoadedFlow.emit(
                        ChapterLoaded(
                            chapterIndex = index,
                            type = ChapterLoaded.Type.Initial
                        )
                    )
                }
            )
        }
        return true
    }

    fun loadInitialChapter(chapterUrl: String): Boolean {
        readerState = ReaderViewModel.ReaderState.INITIAL_LOAD
        items.clear()
        forceUpdateListViewState()

        val insert: (ReaderItem) -> Unit = { items.add(it) }
        val insertAll: (Collection<ReaderItem>) -> Unit = { items.addAll(it) }
        val remove: (ReaderItem) -> Unit = { items.remove(it) }

        val index = orderedChapters.indexOfFirst { it.url == chapterUrl }
        if (index == -1) {
            showInvalidChapterDialog()
            return false
        }

        launch {
            addChapter(
                index = index,
                insert = insert,
                insertAll = insertAll,
                remove = remove,
                maintainPosition = maintainStartPosition,
                onCompletion = {
                    initialChapterLoadedFlow.emit(InitialLoadCompleted.Initial(chapterIndex = index))
                    chapterLoadedFlow.emit(
                        ChapterLoaded(
                            chapterIndex = index,
                            type = ChapterLoaded.Type.Initial
                        )
                    )
                }
            )
        }
        return true
    }

    fun loadPreviousChapter(): Boolean {
        readerState = ReaderViewModel.ReaderState.LOADING

        val firstItem = items.firstOrNull()!!
        if (firstItem is ReaderItem.BookStart) {
            readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        var listIndex = 0
        val insert: (ReaderItem) -> Unit = {
            items.add(listIndex, it)
            listIndex += 1
        }
        val insertAll: (Collection<ReaderItem>) -> Unit = {
            items.addAll(listIndex, it)
            listIndex += it.size
        }
        val remove = { item: ReaderItem ->
            val isRemoved = items.remove(item)
            if (isRemoved) {
                listIndex -= 1
            }
        }

        val previousIndex = chaptersStats[firstItem.chapterUrl]!!.chapterIndex - 1

        if (previousIndex < 0) {
            maintainLastVisiblePosition {
                insert(
                    ReaderItem.BookStart(
                        chapterUrl = firstItem.chapterUrl,
                        chapterIndex = previousIndex
                    )
                )
            }
            readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        launch {
            addChapter(
                index = previousIndex,
                insert = insert,
                insertAll = insertAll,
                remove = remove,
                maintainPosition = maintainLastVisiblePosition,
                onCompletion = {
                    readerState = ReaderViewModel.ReaderState.IDLE
                    chapterLoadedFlow.emit(
                        ChapterLoaded(
                            chapterIndex = previousIndex,
                            type = ChapterLoaded.Type.Previous
                        )
                    )
                }
            )
        }
        return true
    }

    fun loadNextChapter(): Boolean {
        readerState = ReaderViewModel.ReaderState.LOADING

        val lastItem = items.lastOrNull()!!
        if (lastItem is ReaderItem.BookEnd) {
            readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }
        val nextIndex = chaptersStats[lastItem.chapterUrl]!!.chapterIndex + 1

        val insert: (ReaderItem) -> Unit = { items.add(it) }
        val insertAll: (Collection<ReaderItem>) -> Unit = { items.addAll(it) }
        val remove: (ReaderItem) -> Unit = { items.remove(it) }

        if (nextIndex >= orderedChapters.size) {
            insert(
                ReaderItem.BookEnd(
                    chapterUrl = lastItem.chapterUrl,
                    chapterIndex = nextIndex,
                )
            )
            readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        launch {
            addChapter(
                index = nextIndex,
                insert = insert,
                insertAll = insertAll,
                remove = remove,
                onCompletion = {
                    readerState = ReaderViewModel.ReaderState.IDLE
                    chapterLoadedFlow.emit(
                        ChapterLoaded(
                            chapterIndex = nextIndex,
                            type = ChapterLoaded.Type.Next
                        )
                    )
                }
            )
        }
        return true
    }

    private suspend fun addChapter(
        index: Int,
        insert: ((ReaderItem) -> Unit),
        insertAll: ((Collection<ReaderItem>) -> Unit),
        remove: ((ReaderItem) -> Unit),
        maintainPosition: (() -> Unit) -> Unit = { it() },
        onCompletion: (suspend () -> Unit)
    ) = withContext(Dispatchers.Default) {
        val chapter = orderedChapters[index]
        val itemProgressBar = ReaderItem.Progressbar(
            chapterUrl = chapter.url,
            chapterIndex = index,
        )
        val itemTitle = ReaderItem.Title(
            chapterUrl = chapter.url,
            chapterIndex = index,
            text = chapter.title,
            chapterItemIndex = 0,
        ).copy(
            textTranslated = liveTranslation.translator?.translate?.invoke(chapter.title)
                ?: chapter.title
        )

        withContext(Dispatchers.Main) {
            maintainPosition {
                insert(
                    ReaderItem.Divider(
                        chapterUrl = chapter.url,
                        chapterIndex = index,
                    )
                )
                insert(itemTitle)
                insert(itemProgressBar)
                forceUpdateListViewState()
            }
        }

        when (val res = repository.bookChapterBody.fetchBody(chapter.url)) {
            is Response.Success -> {
                // Split chapter text into items
                val itemsOriginal = textToItemsConverter(
                    chapterUrl = chapter.url,
                    chapterPos = index,
                    text = res.data,
                )

                val itemTranslationAttribution = liveTranslation.translator?.let {
                    ReaderItem.GoogleTranslateAttribution(
                        chapterUrl = chapter.url,
                        chapterIndex = index,
                    )
                }

                val itemTranslation = liveTranslation.translator?.let {
                    ReaderItem.Translating(
                        chapterUrl = chapter.url,
                        chapterIndex = index,
                        sourceLang = it.sourceLocale.displayLanguage,
                        targetLang = it.targetLocale.displayLanguage,
                    )
                }

                if (itemTranslation != null) {
                    withContext(Dispatchers.Main) {
                        maintainPosition {
                            insert(itemTranslation)
                            forceUpdateListViewState()
                        }
                    }
                }

                // Translate if necessary
                val items = liveTranslation.translator?.let { translator ->
                    itemsOriginal.map {
                        if (it is ReaderItem.Body) {
                            it.copy(textTranslated = translator.translate(it.text))
                        } else it
                    }
                } ?: itemsOriginal

                withContext(Dispatchers.Main) {
                    chaptersStats[chapter.url] = ReaderViewModel.ChapterStats(
                        chapter = chapter,
                        itemCount = items.size,
                        chapterIndex = index
                    )
                    maintainPosition {
                        remove(itemProgressBar)
                        itemTranslation?.let {
                            remove(it)
                        }
                        itemTranslationAttribution?.let {
                            insert(it)
                        }
                        insertAll(items)
                        insert(
                            ReaderItem.Divider(
                                chapterUrl = chapter.url,
                                chapterIndex = index,
                            )
                        )
                        forceUpdateListViewState()
                    }
                    loadedChapters.add(chapter.url)
                    onCompletion()
                }
            }
            is Response.Error -> {
                withContext(Dispatchers.Main) {
                    chaptersStats[chapter.url] = ReaderViewModel.ChapterStats(
                        chapter = chapter,
                        itemCount = 1,
                        chapterIndex = index
                    )
                    maintainPosition {
                        remove(itemProgressBar)
                        insert(
                            ReaderItem.Error(
                                chapterUrl = chapter.url,
                                chapterIndex = index,
                                text = res.message,
                            )
                        )
                        forceUpdateListViewState()
                    }
                    loadedChapters.add(chapter.url)
                    onCompletion()
                }
            }
        }
    }
}