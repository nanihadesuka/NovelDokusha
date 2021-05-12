package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.*

class ChaptersModel : ViewModel()
{
	private var initialized = false
	fun initialization(bookMetadata: BookMetadata)
	{
		if (initialized) return else initialized = true
		
		this.bookMetadata = bookMetadata
		loadChapters()
	}
	
	inner class ChapterItem(val chapter: bookstore.Chapter)
	{
		val currentlyLastReadChapterLiveData by lazy { bookLastReadChapterFlow.map { it == chapter.url }.asLiveData() }
		val downloadedLiveData by lazy { downloadedChaptersFlow.map { it.contains(chapter.url) }.asLiveData() }
	}
	
	lateinit var bookMetadata: BookMetadata
	val downloadedChaptersFlow by lazy {
		bookstore.bookChapterBody.getExistBodyChapterUrlsFlow(bookMetadata.url)
	}
	val chaptersItemsLiveData by lazy {
		bookstore.bookChapter.chaptersFlow(bookMetadata.url).map {
			it.map(::ChapterItem).asReversed()
		}.asLiveData()
	}
	val bookLastReadChapterFlow by lazy { bookstore.bookLibrary.bookLastReadChapterFlow(bookMetadata.url) }
	val chapters = ArrayList<ChapterItem>()
	val refresh = MutableLiveData<Boolean>()
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