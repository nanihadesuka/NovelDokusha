package my.noveldokusha.ui.reader

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.*
import my.noveldokusha.bookstore
import my.noveldokusha.ui.BaseViewModel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

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
			orderedChapters.clear()
			orderedChapters.addAll(bookstore.bookChapter.chapters(bookUrl))
		}
		fn()
	}
	
	var currentChapter: ChapterState by Delegates.observable(ChapterState("", 0, 0)) { _, old, new ->
		savedState.set<String>(savedStateChapterUrlID, new.url)
		if (old.url != new.url) saveLastReadPositionState(bookUrl, new)
	}
	
	lateinit var bookUrl: String
		private set
	
	val orderedChapters = mutableListOf<bookstore.Chapter>()
	val chaptersSize = mutableMapOf<String, Int>()
	val items = ArrayList<ReaderActivity.Item>()
	val readRoutine = ChaptersIsReadRoutine()
	
	var state = State.INITIAL_LOAD
	
	enum class State
	{ IDLE, LOADING, INITIAL_LOAD, INITIAL_LOAD_COMPLETED }
	
	override fun onCleared()
	{
		saveLastReadPositionState(bookUrl, currentChapter)
		super.onCleared()
	}
}

data class ChapterState(val url: String, val position: Int, val offset: Int)

private fun saveLastReadPositionState(bookUrl: String, chapter: ChapterState)
{
	CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
		bookstore.appDB.withTransaction {
			bookstore.bookLibrary.get(bookUrl)?.let {
				bookstore.bookLibrary.update(it.copy(lastReadChapter = chapter.url))
			}
			
			bookstore.bookChapter.get(chapter.url)?.let {
				bookstore.bookChapter.update(it.copy(lastReadPosition = chapter.position, lastReadOffset = chapter.offset))
			}
		}
	}
}

suspend fun getChapterInitialPosition(bookUrl: String, chapterUrl: String, items: ArrayList<ReaderActivity.Item>): Pair<Int, Int>
{
	val titlePos by lazy { items.indexOfFirst { it is ReaderActivity.Item.TITLE } }
	val chapter = bookstore.bookChapter.get(chapterUrl) ?: return Pair(titlePos, 0)
	val book = bookstore.bookLibrary.get(bookUrl)
	val position by lazy {
		items.indexOfFirst {
			it is ReaderActivity.Item.Position && it.pos == chapter.lastReadPosition
		}.let { index ->
			if (index == -1) Pair(titlePos, 0)
			else Pair(index, chapter.lastReadOffset)
		}
	}
	return when
	{
		chapterUrl == book?.lastReadChapter -> position
		chapter.read -> Pair(titlePos, 0)
		else -> position
	}.let { Pair(it.first.coerceAtLeast(titlePos), it.second) }
}

class ChaptersIsReadRoutine
{
	fun setReadStart(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(startSeen = true) }
	fun setReadEnd(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(endSeen = true) }
	
	private data class ChapterReadStatus(val startSeen: Boolean, val endSeen: Boolean)
	
	private val scope = CoroutineScope(Dispatchers.IO)
	private val chapterRead = mutableMapOf<String, ChapterReadStatus>()
	
	private fun checkLoadStatus(chapterUrl: String, fn: (ChapterReadStatus) -> ChapterReadStatus) = scope.launch(Dispatchers.IO) {
		
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