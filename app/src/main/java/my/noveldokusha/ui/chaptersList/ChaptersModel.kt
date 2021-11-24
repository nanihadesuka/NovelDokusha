package my.noveldokusha.ui.chaptersList

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.*
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.database.bookstore
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast

class ChaptersModel(val bookMetadata: BookMetadata) : BaseViewModel()
{
	init
	{
		viewModelScope.launch(Dispatchers.IO) {
			if (!bookstore.bookChapter.hasChapters(bookMetadata.url))
				updateChaptersList()
			
			val book = bookstore.bookLibrary.get(bookMetadata.url)
			if (book == null)
				bookstore.bookLibrary.insert(Book(title = bookMetadata.title, url = bookMetadata.url))
		}
	}

	val isInLibrary = bookstore.bookLibrary.existInLibraryFlow(bookMetadata.url).asLiveData()
	val preferences = App.instance.appSharedPreferences()
	val onFetching = MutableLiveData<Boolean>()
	val onError = MutableLiveData<String>()
	val onErrorVisibility = MutableLiveData<Int>()
	val selectedChaptersUrl = mutableSetOf<String>()
	val chaptersFilterFlow = MutableStateFlow("")
	val chaptersWithContextLiveData = bookstore.bookChapter.getChaptersWithContexFlow(bookMetadata.url)
			.map {
				// Try removing repetitive title text from chapters
				if (it.size <= 1) return@map it
				val first = it.first().chapter.title
				val prefix = it.fold(first) { acc, e -> e.chapter.title.commonPrefixWith(acc, ignoreCase = true) }
				val suffix = it.fold(first) { acc, e -> e.chapter.title.commonSuffixWith(acc, ignoreCase = true) }
				
				// Kotlin Std Lib doesn't have optional ignoreCase parameter
				fun String.removeSurrounding(prefix: CharSequence, suffix: CharSequence, ignoreCase: Boolean = false): String
				{
					if ((length >= prefix.length + suffix.length) && startsWith(prefix, ignoreCase) && endsWith(suffix, ignoreCase))
					{
						return substring(prefix.length, length - suffix.length)
					}
					return this
				}
				
				return@map it.map { data ->
					val newTitle = data.chapter.title.removeSurrounding(prefix, suffix, ignoreCase = true)
						.ifBlank { data.chapter.title }
					data.copy(chapter = data.chapter.copy(title = newTitle))
				}
			}.combine(preferences.CHAPTERS_SORT_ASCENDING_flow()) { chapters, sorted ->
				when (sorted)
				{
					TERNARY_STATE.active -> chapters.sortedBy { it.chapter.position }
					TERNARY_STATE.inverse -> chapters.sortedByDescending { it.chapter.position }
					TERNARY_STATE.inactive -> chapters
				}
			}
			.combine(chaptersFilterFlow.debounce(50)) { chapters, searchText ->
				if (searchText.isBlank()) chapters
				else chapters.filter { it.chapter.title.contains(searchText, ignoreCase = true) }
			}
			.flowOn(Dispatchers.Default).asLiveData()

	private var loadChaptersJob: Job? = null
	fun updateChaptersList()
	{
		if (bookMetadata.url.startsWith("local://"))
		{
			toast(R.string.local_book_nothing_to_update.stringRes())
			onFetching.postValue(false)
			return
		}
		
		if (loadChaptersJob?.isActive == true) return
		loadChaptersJob = CoroutineScope(Dispatchers.Main).launch {
			
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