package my.noveldokusha.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.scraper.Response
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.fetchChaptersList
import my.noveldokusha.ui.BaseViewModel

class LibraryPageModel : BaseViewModel()
{
	fun initialization(showCompleted: Boolean) = callOneTime {
		this.showCompleted = showCompleted
		
		booksLiveData = when (this.showCompleted)
		{
			true -> bookstore.bookLibrary.booksCompletedFlow
			false -> bookstore.bookLibrary.booksReadingFlow
		}.map { it.map(::BookItem) }.asLiveData()
	}
	
	lateinit var booksLiveData: LiveData<List<BookItem>>
	
	data class BookItem(val data: bookstore.Book)
	{
		val numberOfUnreadChapters = bookstore.bookChapter.numberOfUnreadChaptersFlow(data.url).asLiveData()
	}
	
	private var showCompleted: Boolean = false
	val books: MutableList<BookItem> = mutableListOf()
	val refreshing = MutableLiveData(false)
	
	data class UpdateNotice(val newChapters: MutableList<String>, val failed: MutableList<String>)
	
	val updateNotice = MutableLiveData<UpdateNotice>()
	
	fun update()
	{
		refreshing.postValue(true)
		val booksCopy = books.map { it.copy() }
		
		GlobalScope.launch(Dispatchers.IO) {
			val newChapters = mutableListOf<String>()
			val failed = mutableListOf<String>()
			for (book in booksCopy)
			{
				val oldChaptersList = bookstore.bookChapter.chapters(book.data.url)
				when (val res = fetchChaptersList(book.data.url, false))
				{
					is Response.Success -> if (res.data.size > oldChaptersList.size) newChapters.add(book.data.title)
					is Response.Error -> failed.add(book.data.title)
				}
			}
			updateNotice.postValue(UpdateNotice(newChapters, failed))
			refreshing.postValue(false)
		}
	}
}