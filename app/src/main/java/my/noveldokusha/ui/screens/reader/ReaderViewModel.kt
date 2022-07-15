package my.noveldokusha.ui.screens.reader

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslatorState
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import java.io.File
import java.util.Locale
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
class ReaderViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val translationManager: TranslationManager
) : BaseViewModel(), ReaderStateBundle
{
    enum class ReaderState
    { IDLE, LOADING, INITIAL_LOAD }

    data class ChapterStats(val itemCount: Int, val chapter: Chapter, val index: Int)

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    var translator: TranslatorState? = translationManager.getTranslator(
        source = TranslateLanguage.ENGLISH,
        target = TranslateLanguage.CATALAN,
    )

    val localBookBaseFolder = File(repository.settings.folderBooks, bookUrl.removePrefix("local://"))

    var currentChapter: ChapterState by Delegates.observable(ChapterState(chapterUrl, 0, 0)) { _, old, new ->
        chapterUrl = new.url
        if (old.url != new.url) saveLastReadPositionState(repository, bookUrl, new, old)
    }

    var showReaderInfoView by mutableStateOf(false)
    val orderedChapters: List<Chapter>

    val readingPosStats = MutableLiveData<Pair<ChapterStats, Int>>()

    init
    {
        val chapter = viewModelScope.async(Dispatchers.IO) { repository.bookChapter.get(chapterUrl) }
        val bookChapter = viewModelScope.async(Dispatchers.IO) { repository.bookChapter.chapters(bookUrl) }

        // Need to fix this somehow
        runBlocking {
            currentChapter = ChapterState(
                url = chapterUrl,
                position = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            this@ReaderViewModel.orderedChapters = bookChapter.await()
        }
    }

    val items = ArrayList<ReaderItem>()
    val readRoutine = ChaptersIsReadRoutine(repository)
    var readerState = ReaderState.INITIAL_LOAD

    private val chaptersStats = mutableMapOf<String, ChapterStats>()
    private val initialLoadDone = AtomicBoolean(false)

    override fun onCleared()
    {
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    fun reloadReader() {
        readerState = ReaderState.INITIAL_LOAD
        items.clear()
    }

    fun initialLoad(fn: () -> Unit)
    {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    fun getNextChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index + 1

    fun getPreviousChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index - 1

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int)
    {
        val chapter = chaptersStats[chapterUrl] ?: return
        readingPosStats.postValue(Pair(chapter, itemPos))
    }

    fun addChapterStats(chapter: Chapter, itemCount: Int, index: Int)
    {
        chaptersStats[chapter.url] = ChapterStats(itemCount, chapter, index)
    }

    suspend fun fetchChapterBody(chapterUrl: String) = repository.bookChapterBody.fetchBody(chapterUrl)

    suspend fun getChapterInitialPosition(): Pair<Int, Int>?
    {
        val stats = chaptersStats[currentChapter.url] ?: return null
        return getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapter = stats.chapter,
            items = items
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
            repository.bookChapter.update(it.copy(lastReadPosition = oldChapter.position, lastReadOffset = oldChapter.offset))
        }

        repository.bookChapter.get(chapter.url)?.let {
            repository.bookChapter.update(it.copy(lastReadPosition = chapter.position, lastReadOffset = chapter.offset))
        }
    }
}

suspend fun getChapterInitialPosition(
    repository: Repository,
    bookUrl: String,
    chapter: Chapter,
    items: ArrayList<ReaderItem>
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

    when
    {
        chapter.url == book.await()?.lastReadChapter -> position.await()
        chapter.read -> Pair(titlePos.await(), 0)
        else -> position.await()
    }.let { Pair(it.first.coerceAtLeast(titlePos.await()), it.second) }
}

