package my.noveldokusha.ui.databaseBookInfo

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.fetchDoc
import my.noveldokusha.ui.BaseViewModel

class DatabaseBookInfoModel(val database: DatabaseInterface, val bookMetadata: BookMetadata) : BaseViewModel()
{
	val bookDataLiveData by lazy {
		flow {
			val doc = fetchDoc(bookMetadata.url)
			emit(Response.Success(database.getBookData(doc)))
		}.flowOn(Dispatchers.IO).asLiveData()
	}
}