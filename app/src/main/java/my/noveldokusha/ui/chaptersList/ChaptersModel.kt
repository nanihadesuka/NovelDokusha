package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
		chaptersLiveData = bookstore.bookChapter.chaptersFlow(bookMetadata.url).distinctUntilChanged().asLiveData()
		bookLastReadChapterFlow = bookstore.bookLibrary.bookLastReadChapterFlow(bookMetadata.url).distinctUntilChanged()
		downloadedChaptersFlow = bookstore.bookChapterBody.getExistBodyChapterUrlsFlow(bookMetadata.url)
		loadChapters()
	}
	
	lateinit var downloadedChaptersFlow: Flow<Set<String>>
	lateinit var chaptersLiveData: LiveData<List<bookstore.Chapter>>
	lateinit var bookLastReadChapterFlow: Flow<String?>
	val chapters = ArrayList<ChaptersActivity.ChapterItem>()
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
	fun setAsRead(selected: ChaptersActivity.ChapterItem?)
	{
		if (setAsReadJob?.isActive == true) return
		setAsReadJob = GlobalScope.launch(Dispatchers.IO) {
			val index = chapters.indexOf(selected).coerceAtLeast(0)
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