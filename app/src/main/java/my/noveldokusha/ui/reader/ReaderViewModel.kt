package my.noveldokusha.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    val appPreferences: AppPreferences
) : BaseViewModel(), ReaderStateBundle {

    enum class ReaderState { IDLE, LOADING, INITIAL_LOAD }
    data class ChapterStats(val itemCount: Int, val chapter: Chapter, val index: Int)

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    val localBookBaseFolder =
        File(repository.settings.folderBooks, bookUrl.removePrefix("local://"))

    var currentChapter: ChapterState by Delegates.observable(
        ChapterState(
            chapterUrl,
            0,
            0
        )
    ) { _, old, new ->
        chapterUrl = new.url
        if (old.url != new.url) saveLastReadPositionState(repository, bookUrl, new, old)
    }

    val orderedChapters: List<Chapter>

    var readerFontSize by mutableStateOf(appPreferences.READER_FONT_SIZE)
    var readerFontFamily by mutableStateOf(appPreferences.READER_FONT_FAMILY)
    var readerState by mutableStateOf(ReaderState.INITIAL_LOAD)
    var readingPosStats by mutableStateOf<Pair<ChapterStats, Int>?>(null)

    val list = mutableStateListOf<ReaderItem>()
    val listState = LazyListState(0, 0)

    val readRoutine = ChaptersIsReadRoutine(repository)

    private val chaptersStats = mutableMapOf<String, ChapterStats>()
    private val initialLoadDone = AtomicBoolean(false)

    init {
        val chapter =
            viewModelScope.async(Dispatchers.IO) { repository.bookChapter.get(chapterUrl) }
        val bookChapter =
            viewModelScope.async(Dispatchers.IO) { repository.bookChapter.chapters(bookUrl) }

        // Need to fix this somehow
        runBlocking {
            currentChapter = ChapterState(
                url = chapterUrl,
                position = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            this@ReaderViewModel.orderedChapters = bookChapter.await()
        }

        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.READER_FONT_SIZE_flow().collect { readerFontSize = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.READER_FONT_FAMILY_flow().collect { readerFontFamily = it }
        }
    }

    override fun onCleared() {
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    fun initialLoad(fn: () -> Unit) {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    fun getNextChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index + 1

    fun getPreviousChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index - 1

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int) {
        val chapter = chaptersStats[chapterUrl] ?: return
        readingPosStats = Pair(chapter, itemPos)
    }

    fun addChapterStats(chapter: Chapter, itemCount: Int, index: Int) {
        chaptersStats[chapter.url] = ChapterStats(itemCount, chapter, index)
    }

    suspend fun fetchChapterBody(chapterUrl: String) =
        repository.bookChapterBody.fetchBody(chapterUrl)

    suspend fun getChapterInitialPosition(): Pair<Int, Int>? {
        val stats = chaptersStats[currentChapter.url] ?: return null
        return getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapter = stats.chapter,
            items = list
        )
    }
}

data class ChapterState(val url: String, val position: Int, val offset: Int)

private fun saveLastReadPositionState(
    repository: Repository,
    bookUrl: String,
    chapter: ChapterState,
    oldChapter: ChapterState? = null
) = CoroutineScope(Dispatchers.IO).launch {
    repository.withTransaction {
        repository.bookLibrary.get(bookUrl)?.let {
            repository.bookLibrary.update(it.copy(lastReadChapter = chapter.url))
        }

        if (oldChapter?.url != null) repository.bookChapter.get(oldChapter.url)?.let {
            repository.bookChapter.update(
                it.copy(
                    lastReadPosition = oldChapter.position,
                    lastReadOffset = oldChapter.offset
                )
            )
        }

        repository.bookChapter.get(chapter.url)?.let {
            repository.bookChapter.update(
                it.copy(
                    lastReadPosition = chapter.position,
                    lastReadOffset = chapter.offset
                )
            )
        }
    }
}

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapter: Chapter,
    items: List<ReaderItem>
): Pair<Int, Int> = coroutineScope {

    val book = async(Dispatchers.IO) { repository.bookLibrary.get(bookUrl) }
    val titlePos = async(Dispatchers.Default) {
        items.indexOfFirst { it is ReaderItem.TITLE }
    }
    val position = async(Dispatchers.Default) {
        items.indexOfFirst {
            it is ReaderItem.Position && it.pos == chapter.lastReadPosition
        }.let { index ->
            if (index == -1) Pair(titlePos.await(), 0)
            else Pair(index, chapter.lastReadOffset)
        }
    }

    when {
        chapter.url == book.await()?.lastReadChapter -> position.await()
        chapter.read -> Pair(titlePos.await(), 0)
        else -> position.await()
    }.let { Pair(it.first.coerceAtLeast(titlePos.await()), it.second) }
}

