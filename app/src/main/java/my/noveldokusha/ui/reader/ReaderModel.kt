package my.noveldokusha.ui.reader

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.*
import my.noveldokusha.Chapter
import my.noveldokusha.bookstore
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.ObservableNoInitValue
import java.util.*
import kotlin.collections.ArrayList

class ReaderModel(private val savedState: SavedStateHandle) : BaseViewModel()
{
	private val savedStateChapterUrlID = "chapterUrl"
	
	fun initialization(bookUrl: String, selectedChapter: String, fn: () -> Unit) = callOneTime {
		this.bookUrl = bookUrl
		val url = savedState.get<String>(savedStateChapterUrlID) ?: selectedChapter
		
		runBlocking {
			val chapter = bookstore.bookChapter.get(url)
			currentChapter = ChapterState(
				url = url,
				position = chapter?.lastReadPosition ?: 0,
				offset = chapter?.lastReadOffset ?: 0
			)
			orderedChapters.addAll(bookstore.bookChapter.chapters(bookUrl))
		}
		fn()
	}
	
	var currentChapter: ChapterState by ObservableNoInitValue { _, old, new ->
		savedState.set<String>(savedStateChapterUrlID, new.url)
		if (old.url != new.url) saveLastReadPositionState(bookUrl, new, old)
	}
	
	lateinit var bookUrl: String
		private set
	
	val orderedChapters = mutableListOf<Chapter>()
	
	data class ChapterStats(val size: Int, val chapter: Chapter, val index: Int)
	
	val chaptersStats = mutableMapOf<String, ChapterStats>()
	val items = ArrayList<ReaderActivity.Item>()
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

suspend fun getChapterInitialPosition(bookUrl: String, chapter: Chapter, items: ArrayList<ReaderActivity.Item>): Pair<Int, Int>
{
	val book = CoroutineScope(Dispatchers.IO).async { bookstore.bookLibrary.get(bookUrl) }
	val titlePos = CoroutineScope(Dispatchers.Default).async {
		items.indexOfFirst { it is ReaderActivity.Item.TITLE }
	}
	val position = CoroutineScope(Dispatchers.Default).async {
		items.indexOfFirst {
			it is ReaderActivity.Item.Position && it.pos == chapter.lastReadPosition
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