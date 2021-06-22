package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import my.noveldokusha.*
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast

class ChaptersModel : BaseViewModel()
{
	fun initialization(bookMetadata: BookMetadata) = callOneTime {
		this.bookMetadata = bookMetadata
		viewModelScope.launch(Dispatchers.IO) {
			if (!bookstore.bookChapter.hasChapters(bookMetadata.url))
				updateChaptersList()
			
			val book = bookstore.bookLibrary.get(bookMetadata.url)
			if (book == null)
				bookstore.bookLibrary.insert(bookstore.Book(title = bookMetadata.title, url = bookMetadata.url))
		}
	}
	
	lateinit var bookMetadata: BookMetadata
	
	val chaptersWithContextLiveData by lazy {
		bookstore.bookChapter.getChaptersWithContexFlow(bookMetadata.url)
			.map {
				// Try removing repetitive title text from chapters
				if (it.size <= 1) return@map it
				val first = it.first().chapter.title
				val prefix = it.fold(first) { acc, e -> e.chapter.title.commonPrefixWith(acc) }
				val suffix = it.fold(first) { acc, e -> e.chapter.title.commonSuffixWith(acc) }
				return@map it.map { data ->
					val newTitle = data.chapter.title.removeSurrounding(prefix, suffix).ifBlank { data.chapter.title }
					data.copy(chapter = data.chapter.copy(title = newTitle))
				}
			}.combine(preferences.CHAPTERS_SORT_ASCENDING_flow()) { chapters, sorted ->
				when (sorted)
				{
					TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
					TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
					TERNARY_STATE.inactive -> chapters
				}
			}.flowOn(Dispatchers.Default).asLiveData()
	}
	
	val preferences = App.instance.appSharedPreferences()
	
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
			val url = bookMetadata.url
			val res = withContext(Dispatchers.IO) { downloadChaptersList(url) }
			onFetching.value = false
			when (res)
			{
				is Response.Success ->
				{
					if (res.data.isEmpty())
						toast(R.string.no_chapters_found.stringRes())
					
					withContext(Dispatchers.IO) {
						bookstore.bookChapter.merge(res.data, url)
					}
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