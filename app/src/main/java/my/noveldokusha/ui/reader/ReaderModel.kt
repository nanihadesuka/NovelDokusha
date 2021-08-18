package my.noveldokusha.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import my.noveldokusha.Chapter
import my.noveldokusha.bookstore
import my.noveldokusha.ui.BaseViewModel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class ReaderModel(private val savedState: SavedStateHandle, val bookUrl: String, selectedChapter: String) : BaseViewModel()
{
	private val savedStateChapterUrlID = "chapterUrl"
	val url = savedState.get<String>(savedStateChapterUrlID) ?: selectedChapter
	var currentChapter: ChapterState by Delegates.observable(ChapterState(url, 0, 0)) { _, old, new ->
		savedState.set<String>(savedStateChapterUrlID, new.url)
		if (old.url != new.url) saveLastReadPositionState(bookUrl, new, old)
	}
	val orderedChapters: List<Chapter>
	
	init
	{
		val chapter = viewModelScope.async(Dispatchers.IO) { bookstore.bookChapter.get(url) }
		val bookChapter = viewModelScope.async(Dispatchers.IO) { bookstore.bookChapter.chapters(bookUrl) }
		
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
	
	val chaptersStats = mutableMapOf<String, ChapterStats>()
	val items = ArrayList<Item>()
	val readRoutine = ChaptersIsReadRoutine()
	
	var state = State.INITIAL_LOAD
	
	enum class State
	{ IDLE, LOADING, INITIAL_LOAD }
	
	override fun onCleared()
	{
		saveLastReadPositionState(bookUrl, currentChapter)
		super.onCleared()
	}
}

data class ChapterState(val url: String, val position: Int, val offset: Int)

private fun saveLastReadPositionState(bookUrl: String, chapter: ChapterState, oldChapter: ChapterState? = null)
{
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		bookstore.appDB.withTransaction {
			bookstore.bookLibrary.get(bookUrl)?.let {
				bookstore.bookLibrary.update(it.copy(lastReadChapter = chapter.url))
			}
			
			if (oldChapter?.url != null) bookstore.bookChapter.get(oldChapter.url)?.let {
				bookstore.bookChapter.update(it.copy(lastReadPosition = oldChapter.position, lastReadOffset = oldChapter.offset))
			}
			
			bookstore.bookChapter.get(chapter.url)?.let {
				bookstore.bookChapter.update(it.copy(lastReadPosition = chapter.position, lastReadOffset = chapter.offset))
			}
		}
	}
}

suspend fun getChapterInitialPosition(bookUrl: String, chapter: Chapter, items: ArrayList<Item>): Pair<Int, Int>
{
	val book = CoroutineScope(Dispatchers.IO).async() { bookstore.bookLibrary.get(bookUrl) }
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

class ChaptersIsReadRoutine
{
	fun setReadStart(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(startSeen = true) }
	fun setReadEnd(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(endSeen = true) }
	
	private data class ChapterReadStatus(val startSeen: Boolean, val endSeen: Boolean)
	
	private val scope = CoroutineScope(Dispatchers.IO)
	private val chapterRead = mutableMapOf<String, ChapterReadStatus>()
	
	private fun checkLoadStatus(chapterUrl: String, fn: (ChapterReadStatus) -> ChapterReadStatus) = scope.launch {
		
		val chapter = bookstore.bookChapter.get(chapterUrl) ?: return@launch
		val oldStatus = chapterRead.getOrPut(chapterUrl) {
			if (chapter.read) ChapterReadStatus(true, true) else ChapterReadStatus(false, false)
		}
		
		if (oldStatus.startSeen && oldStatus.endSeen) return@launch
		
		val newStatus = fn(oldStatus)
		if (newStatus.startSeen && newStatus.endSeen)
			bookstore.bookChapter.update(chapter.copy(read = true))
		
		chapterRead[chapterUrl] = newStatus
	}
}