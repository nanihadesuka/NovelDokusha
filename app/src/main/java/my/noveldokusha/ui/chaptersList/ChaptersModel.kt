package my.noveldokusha.ui.chaptersList

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.BookMetadata
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.fetchChaptersList
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class ChaptersModel : BaseViewModel()
{
	fun initialization(bookMetadata: BookMetadata) = callOneTime {
		this.bookMetadata = bookMetadata
	}
	
	lateinit var bookMetadata: BookMetadata
	
	val chaptersWithContextLiveData by lazy {
		bookstore.bookChapter.getChaptersWithContexFlow(bookMetadata.url)
			.map { it.asReversed() }
			.asLiveData()
	}
	
	val chapters = ArrayList<bookstore.ChapterDao.ChapterWithContext>()
	val sourceName by lazy { scrubber.getCompatibleSource(this.bookMetadata.url)?.name ?: "" }
	val selectedChaptersUrl = mutableSetOf<String>()
	val fetchIterator = FetchIterator(GlobalScope){
		fetchChaptersList(bookMetadata.url,false)
	}
	
	fun toggleBookmark() = viewModelScope.launch(Dispatchers.IO) {
		bookstore.bookLibrary.toggleBookmark(bookMetadata)
	}
}