package my.noveldokusha.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.drop
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiUtils.asLiveEvent
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

interface ReaderStateBundle
{
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderModel @Inject constructor(
    val repository: Repository,
    private val state: SavedStateHandle,
    val appPreferences: AppPreferences
) : BaseViewModel(), ReaderStateBundle
{
    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    private val savedStateChapterUrlID = "chapterUrl"
    val url = state.get<String>(savedStateChapterUrlID) ?: chapterUrl
    var currentChapter: ChapterState by Delegates.observable(ChapterState(url, 0, 0)) { _, old, new ->
        state.set<String>(savedStateChapterUrlID, new.url)
        if (old.url != new.url) saveLastReadPositionState(repository, bookUrl, new, old)
    }

    val orderedChapters: List<Chapter>

    init
    {
        val chapter = viewModelScope.async(Dispatchers.IO) { repository.bookChapter.get(url) }
        val bookChapter = viewModelScope.async(Dispatchers.IO) { repository.bookChapter.chapters(bookUrl) }

        runBlocking {
            currentChapter = ChapterState(
                url = url,
                position = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            this@ReaderModel.orderedChapters = bookChapter.await()
        }
    }

    private val initialLoadDone = AtomicBoolean(false)
    fun initialLoad(fn: () -> Unit)
    {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    data class ChapterStats(val size: Int, val chapter: Chapter, val index: Int)

    val readerFontSize = appPreferences.READER_FONT_SIZE_flow().asLiveData()
    val readerFontFamily = appPreferences.READER_FONT_FAMILY_flow().asLiveData()

    val chaptersStats = mutableMapOf<String, ChapterStats>()
    val items = ArrayList<Item>()
    val readRoutine = ChaptersIsReadRoutine(repository)

    var readerState = ReaderState.INITIAL_LOAD

    enum class ReaderState
    { IDLE, LOADING, INITIAL_LOAD }

    override fun onCleared()
    {
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    suspend fun fetchChapterBody(chapterUrl: String) = repository.bookChapterBody.fetchBody(chapterUrl)

    suspend fun getChapterInitialPosition() = getChapterInitialPosition(
        repository = repository,
        bookUrl = bookUrl,
        chapter = chaptersStats[currentChapter.url]!!.chapter,
        items = items
    )
}

data class ChapterState(val url: String, val position: Int, val offset: Int)

private fun saveLastReadPositionState(
    repository: Repository,
    bookUrl: String,
    chapter: ChapterState,
    oldChapter: ChapterState? = null
)
{
    CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
        repository.withTransaction {
            repository.bookLibrary.get(bookUrl)?.let {
                repository.bookLibrary.update(it.copy(lastReadChapter = chapter.url))
            }

            if (oldChapter?.url != null) repository.bookChapter.get(oldChapter.url)?.let {
                repository.bookChapter.update(it.copy(lastReadPosition = oldChapter.position, lastReadOffset = oldChapter.offset))
            }

            repository.bookChapter.get(chapter.url)?.let {
                repository.bookChapter.update(it.copy(lastReadPosition = chapter.position, lastReadOffset = chapter.offset))
            }
        }
    }
}

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapter: Chapter,
    items: ArrayList<Item>
): Pair<Int, Int>
{
    val book = CoroutineScope(Dispatchers.IO).async { repository.bookLibrary.get(bookUrl) }
    val titlePos = CoroutineScope(Dispatchers.Default).async {
        items.indexOfFirst { it is Item.TITLE }
    }
    val position = CoroutineScope(Dispatchers.Default).async {
        items.indexOfFirst {
            it is Item.Position && it.pos == chapter.lastReadPosition
        }.let { index ->
            if (index == -1) Pair(titlePos.await(), 0)
            else Pair(index, chapter.lastReadOffset)
        }
    }

    return when
    {
        chapter.url == book.await()?.lastReadChapter -> position.await()
        chapter.read -> Pair(titlePos.await(), 0)
        else -> position.await()
    }.let { Pair(it.first.coerceAtLeast(titlePos.await()), it.second) }
}

class ChaptersIsReadRoutine(val repository: Repository)
{
    fun setReadStart(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(startSeen = true) }
    fun setReadEnd(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(endSeen = true) }

    private data class ChapterReadStatus(val startSeen: Boolean, val endSeen: Boolean)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val chapterRead = mutableMapOf<String, ChapterReadStatus>()

    private fun checkLoadStatus(chapterUrl: String, fn: (ChapterReadStatus) -> ChapterReadStatus) = scope.launch {

        val chapter = repository.bookChapter.get(chapterUrl) ?: return@launch
        val oldStatus = chapterRead.getOrPut(chapterUrl) {
            if (chapter.read) ChapterReadStatus(true, true) else ChapterReadStatus(false, false)
        }

        if (oldStatus.startSeen && oldStatus.endSeen) return@launch

        val newStatus = fn(oldStatus)
        if (newStatus.startSeen && newStatus.endSeen)
            repository.bookChapter.update(chapter.copy(read = true))

        chapterRead[chapterUrl] = newStatus
    }
}