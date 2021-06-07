package my.noveldokusha.ui.main

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import kotlin.properties.Delegates

class LibraryPageModel : BaseViewModel()
{
	fun initialization(showCompleted: Boolean) = callOneTime {
		this.showCompleted = showCompleted
	}
	
	val booksWithContextFlow by lazy {
		bookstore.bookLibrary.getBooksInLibraryWithContextFlow.map { it.filter { book -> book.book.completed == showCompleted } }
	}
	
	val booksWithContext = mutableListOf<bookstore.LibraryDao.BookWithContext>()
	
	private var showCompleted by Delegates.notNull<Boolean>()
	val refreshing = MutableLiveData(false)
	
	data class UpdateNotice(val newChapters: MutableList<String>, val failed: MutableList<String>)
	
	val updateNotice = MutableLiveData<UpdateNotice>()
	
	fun update()
	{
		refreshing.postValue(true)
		val completed = showCompleted
		GlobalScope.launch(Dispatchers.IO) {
			val books = bookstore.bookLibrary.getAll().filter { it.completed == completed }
			val newChapters = mutableListOf<String>()
			val failed = mutableListOf<String>()
			for (book in books)
			{
				val oldChaptersList = bookstore.bookChapter.chapters(book.url)
				when (val res = downloadChaptersList(book.url))
				{
					is Response.Success ->
					{
						bookstore.bookChapter.merge(res.data, book.url)
						if (res.data.size > oldChaptersList.size)
							newChapters.add(book.title)
					}
					is Response.Error -> failed.add(book.title)
				}
			}
			updateNotice.postValue(UpdateNotice(newChapters, failed))
			refreshing.postValue(false)
		}
	}
}