package my.noveldokusha.ui.databaseBookInfo

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.database_interface
import my.noveldokusha.scraper.fetchDoc
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class DatabaseBookInfoModel(val database: database_interface, val bookMetadata: BookMetadata) : BaseViewModel()
{
	val bookDataLiveData by lazy {
		flow {
			val doc = fetchDoc(bookMetadata.url)
			emit(Response.Success(database.getBookData(doc)))
		}.flowOn(Dispatchers.IO).asLiveData()
	}
}