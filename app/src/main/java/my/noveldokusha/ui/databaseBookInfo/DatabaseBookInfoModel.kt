package my.noveldokusha.ui.databaseBookInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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
	val bookDataLiveData = MutableLiveData<scrubber.database_interface.BookData>()
	
	private fun downloadBookData()
	{
		viewModelScope.launch(Dispatchers.IO) {
			
			val res: Response<scrubber.database_interface.BookData> = tryConnect {
				Response.Success(database.getBookData(fetchDoc(bookMetadata.url)))
			}
			
			if (res is Response.Success)
			{
				val data = res.data
				bookDataLiveData.postValue(data)
			}
		}
	}
}