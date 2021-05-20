package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.BookMetadata
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.fetchChaptersList
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class ChaptersModel : BaseViewModel()
{
	fun initialization(bookMetadata: BookMetadata) = callOneTime {
		this.bookMetadata = bookMetadata
		loadChapters()
	}
	
	lateinit var bookMetadata: BookMetadata
	
	val chaptersWithContextLiveData by lazy {
		bookstore.bookChapter.getChaptersWithContexFlow(bookMetadata.url)
			.map { it.asReversed() }
			.asLiveData()
	}
	
	val chapters = ArrayList<bookstore.ChapterDao.ChapterWithContext>()
	val refresh = MutableLiveData<Boolean>()
	val sourceName by lazy { scrubber.getCompatibleSource(this.bookMetadata.url)?.name ?: "" }
	val errorMessage = MutableLiveData<String>()
	val errorMessageVisibility = MutableLiveData<Int>()
	val errorMessageMaxLines = MutableLiveData<Int>(10)
	
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