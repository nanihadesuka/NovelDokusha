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
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.toast

class ChaptersModel : BaseViewModel()
{
	fun initialization(bookMetadata: BookMetadata) = callOneTime {
		this.bookMetadata = bookMetadata
		viewModelScope.launch(Dispatchers.IO) {
			if (!bookstore.bookChapter.hasChapters(bookMetadata.url))
				updateChaptersList()
		}
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
	fun updateChaptersList()
	{
		if (loadChaptersJob?.isActive == true) return
		loadChaptersJob = GlobalScope.launch(Dispatchers.Main) {
			
			onErrorVisibility.value = View.GONE
			onFetching.value = true
			val res = withContext(Dispatchers.IO) { downloadChaptersList(bookMetadata.url) }
			onFetching.value = false
			when (res)
			{
				is Response.Success ->
				{
					bookstore.bookChapter.insert(res.data)
					if(res.data.isEmpty())
						toast("No chaptes found")
				}
				is Response.Error ->
				{
					onErrorVisibility.value = View.VISIBLE
					onError.value = res.message
				}
			}
		}
	}
	
	fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
		bookstore.bookLibrary.toggleBookmark(bookMetadata)
	}
}