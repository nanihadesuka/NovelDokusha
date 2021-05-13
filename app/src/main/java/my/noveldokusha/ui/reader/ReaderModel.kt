package my.noveldokusha.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import my.noveldokusha.bookstore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class ReaderModel(private val savedState: SavedStateHandle) : ViewModel()
{
	private var initialized: Boolean = false
	fun initialization(bookUrl: String, bookSelectedChapterUrl: String)
	{
		if (initialized) return else initialized = true
		
		this.bookUrl = bookUrl
		this.bookSelectedChapterUrl = savedState.get<String>(this::bookSelectedChapterUrl.name) ?: bookSelectedChapterUrl
		
		readRoutine = ChaptersIsReadRoutine()
		
		runBlocking {
			currentChapter.url = bookstore.bookLibrary.get(bookUrl)?.lastReadChapter ?: bookSelectedChapterUrl
			bookstore.bookChapter.get(currentChapter.url)?.let {
				currentChapter.position = it.lastReadPosition
				currentChapter.offset = it.lastReadOffset
			}
			_orderedChapters.clear()
			_orderedChapters.addAll(bookstore.bookChapter.chapters(bookUrl))
		}
	}
	
	var readRoutine: ChaptersIsReadRoutine? = null
	
	lateinit var bookUrl: String
	var bookSelectedChapterUrl: String = ""
		set(value)
		{
			field = value
			savedState.set<String>(::bookSelectedChapterUrl.name, value)
		}
	
	private val _orderedChapters = mutableListOf<bookstore.Chapter>()
	val orderedChapters: List<bookstore.Chapter> = _orderedChapters
	val chaptersSize = mutableMapOf<String, Int>()
	
	data class LastReadChapter(var url: String, var title: String, var position: Int, var offset: Int)
	
	val items = ArrayList<ReaderActivity.Item>()
	val currentChapter = LastReadChapter("", "", 0, 0)
	var state = State.INITIAL_LOAD
	var isTop_firstCall = true
	
	private var currentChapterOld = currentChapter.copy()
	private val timer = Timer(false).schedule(delay = 1000 * 10, period = 1000 * 10) {
		if (currentChapterOld == currentChapter)
			return@schedule
		
		saveLastReadPositionState()
		currentChapterOld = currentChapter.copy()
	}
	
	fun saveLastReadPositionState()
	{
		val bookUrl = bookUrl
		val currentChapter = currentChapter.copy()
		viewModelScope.launch {
			
			bookstore.bookLibrary.get(bookUrl)?.let {
				bookstore.bookLibrary.update(it.copy(lastReadChapter = currentChapter.url))
			}
			
			bookstore.bookChapter.get(currentChapter.url)?.let {
				bookstore.bookChapter.update(it.copy(lastReadPosition = currentChapter.position, lastReadOffset = currentChapter.offset))
			}
		}
	}
	
	enum class State
	{ IDLE, LOADING, INITIAL_LOAD, INITIAL_LOAD_COMPLETED }
	
	override fun onCleared()
	{
		timer.cancel()
		super.onCleared()
	}
}

suspend fun getChapterInitialPosition(bookUrl: String, chapterUrl: String, items: ArrayList<ReaderActivity.Item>): Pair<Int, Int>
{
	
	val titlePos by lazy { items.indexOfFirst { it is ReaderActivity.Item.TITLE } }
	val book = bookstore.bookLibrary.get(bookUrl)
	val chapter = bookstore.bookChapter.get(chapterUrl) ?: return Pair(titlePos, 0)
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
	
	private val chapterRead = mutableMapOf<String, ChapterReadStatus>()
	private fun checkLoadStatus(chapterUrl: String, fn: (ChapterReadStatus) -> ChapterReadStatus) = GlobalScope.launch(Dispatchers.IO) {
		
		val chapter = bookstore.bookChapter.get(chapterUrl) ?: return@launch
		val oldStatus = withContext(Dispatchers.Main) {
			chapterRead.getOrPut(chapterUrl) {
				if (chapter.read) ChapterReadStatus(true, true) else ChapterReadStatus(false, false)
			}
		}
		
		if (oldStatus.startSeen && oldStatus.endSeen) return@launch
		
		val newStatus = fn(oldStatus)
		if (newStatus.startSeen && newStatus.endSeen)
			bookstore.bookChapter.update(chapter.copy(read = true))
		
		withContext(Dispatchers.Main) {
			chapterRead[chapterUrl] = newStatus
		}
	}
}