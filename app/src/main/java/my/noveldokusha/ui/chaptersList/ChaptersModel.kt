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
	
	val selectedChaptersUrl = mutableSetOf<String>()
	
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
	
	
	fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
		bookstore.bookLibrary.toggleBookmark(bookMetadata)
	}
}