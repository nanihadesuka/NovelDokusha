package my.noveldokusha.ui.databaseBookInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import my.noveldokusha.*

class DatabaseBookInfoModel : ViewModel()
{
	private var initialized = false
	fun initialization(database: scrubber.database_interface, bookMetadata: bookstore.BookMetadata)
	{
		if (initialized) return else initialized = true
		this.database = database
		this.bookMetadata = bookMetadata
		downloadBookData()
	}
	
	val relatedBooks = ArrayList<bookstore.BookMetadata>()
	val similarRecommended = ArrayList<bookstore.BookMetadata>()
	
	lateinit var database: scrubber.database_interface
	lateinit var bookMetadata: bookstore.BookMetadata
	lateinit var bookData: scrubber.database_interface.BookData
	val bookDataUpdated = MutableLiveData<Unit>()
	
	private fun downloadBookData(): Unit = GlobalScope.launch(Dispatchers.IO) {
		
		val res = tryConnect {
			Response.Success(database.getBookData(fetchDoc(bookMetadata.url)))
		}
		
		when (res)
		{
			is Response.Success ->
			{
				bookData = res.data
				relatedBooks.clear()
				relatedBooks.addAll(bookData.relatedBooks)
				similarRecommended.clear()
				similarRecommended.addAll(bookData.similarRecommended)
				bookDataUpdated.postValue(Unit)
			}
			is Response.Error -> Unit
		}
	}.let { }
}