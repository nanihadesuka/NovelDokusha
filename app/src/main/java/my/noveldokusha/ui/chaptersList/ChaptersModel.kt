package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.Response
import my.noveldokusha.bookstore
import my.noveldokusha.fetchChaptersList
import my.noveldokusha.scrubber

class ChaptersModel : ViewModel()
{
	private var initialized = false
	fun initialization(bookMetadata: bookstore.BookMetadata)
	{
		if (initialized) return else initialized = true
		
		this.bookMetadata.title = bookMetadata.title
		this.bookMetadata.url = bookMetadata.url
		chaptersItemsLiveData = bookstore.bookChapter.chaptersFlow(bookMetadata.url).map {
			it.map(::ChapterItem).asReversed()
		}.asLiveData()
		bookLastReadChapterFlow = bookstore.bookLibrary.bookLastReadChapterFlow(bookMetadata.url)
		downloadedChaptersFlow = bookstore.bookChapterBody.getExistBodyChapterUrlsFlow(bookMetadata.url)
		loadChapters()
	}
	
	inner class ChapterItem(val chapter: bookstore.Chapter)
	{
		val currentlyLastReadChapterLiveData by lazy { bookLastReadChapterFlow.map { it == chapter.url }.asLiveData() }
		val downloadedLiveData by lazy { downloadedChaptersFlow.map { it.contains(chapter.url) }.asLiveData() }
	}
	
	lateinit var downloadedChaptersFlow: Flow<Set<String>>
	lateinit var chaptersItemsLiveData: LiveData<List<ChapterItem>>
	lateinit var bookLastReadChapterFlow: Flow<String?>
	val chapters = ArrayList<ChapterItem>()
	val refresh = MutableLiveData<Boolean>()
	val bookMetadata = bookstore.BookMetadata("", "")
	val bookTitle by lazy { this.bookMetadata.title }
	val sourceName by lazy { scrubber.getCompatibleSource(this.bookMetadata.url)?.name ?: "" }
	val errorMessage = MutableLiveData<String>()
	val errorMessageVisibility = MutableLiveData<Int>()
	val errorMessageMaxLines = MutableLiveData<Int>(10)
	val numberOfChapters = MutableLiveData<Int>()
	
	private var loadChaptersJob: Job? = null
	fun loadChapters(tryCache: Boolean = true)
	{
		if (loadChaptersJob?.isActive == true) return
		loadChaptersJob = GlobalScope.launch(Dispatchers.IO) {
			
			errorMessageVisibility.postValue(View.GONE)
			when (val res = fetchChaptersList(bookMetadata.url, tryCache))
			{
				is Response.Error ->
				{
					errorMessageVisibility.postValue(View.VISIBLE)
					errorMessage.postValue(res.message)
				}
				else -> Unit
			}
			refresh.postValue(false)
		}
	}
	
	private var setAsReadJob: Job? = null
	
	/**
	 * Set as read all the chapters bellow the checked chapter (selected chapter included).
	 * If no chapter checked then set all as read.
	 */
	fun setAsRead(position: Int)
	{
		if (setAsReadJob?.isActive == true) return
		setAsReadJob = GlobalScope.launch(Dispatchers.IO) {
			val index = position.coerceAtLeast(0)
			chapters.drop(index).map { it.chapter.url }.let {
				bookstore.bookChapter.setAsRead(it)
			}
		}
	}
	
	fun downloadAllChapters() = GlobalScope.launch(Dispatchers.IO) {
		chapters.reversed().forEach { bookstore.bookChapterBody.fetchBody(it.chapter.url) }
	}.let {}
	
	fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
		val book = bookstore.Book(title = bookMetadata.title, url = bookMetadata.url)
		when (bookstore.bookLibrary.exist(url = book.url))
		{
			true -> bookstore.bookLibrary.remove(book)
			false -> bookstore.bookLibrary.insert(book)
		}
	}
}