package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
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
	val sourceName by lazy { scrubber.getCompatibleSource(this.bookMetadata.url)?.name ?: "" }
	val onFetching = MutableLiveData<Boolean>()
	val onError = MutableLiveData<String>()
	val onErrorVisibility = MutableLiveData<Int>()
	val selectedChaptersUrl = mutableSetOf<String>()

	private var loadChaptersJob: Job? = null
	fun loadChapters(tryCache: Boolean = true)
	{
		if (loadChaptersJob?.isActive == true) return
		loadChaptersJob = GlobalScope.launch(Dispatchers.Main) {
			
			onErrorVisibility.value = View.GONE
			onFetching.value = true
			val res = withContext(Dispatchers.IO) { fetchChaptersList(bookMetadata.url, tryCache) }
			onFetching.value = false
			
			when (res)
			{
				is Response.Error ->
				{
					onErrorVisibility.value = View.VISIBLE
					onError.value = res.message
				}
				else -> Unit
			}
		}
	}
	
	fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
		bookstore.bookLibrary.toggleBookmark(bookMetadata)
	}
}