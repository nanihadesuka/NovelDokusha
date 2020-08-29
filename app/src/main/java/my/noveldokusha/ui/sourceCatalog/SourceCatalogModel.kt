package my.noveldokusha.ui.sourceCatalog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import my.noveldokusha.Response
import my.noveldokusha.bookstore
import my.noveldokusha.downloadSourceCatalog
import my.noveldokusha.scrubber

class SourceCatalogModel : ViewModel()
{
	private var initialized: Boolean = false
	fun initialization(source: scrubber.source_interface.catalog)
	{
		if (initialized) return else initialized = true
		this.source = source
		loadCatalog()
	}
	
	enum class Mode
	{ MAIN, BAR_SEARCH }
	
	var mode = Mode.MAIN
	
	lateinit var source: scrubber.source_interface.catalog
	val catalogList = ArrayList<bookstore.BookMetadata>()
	val catalogListUpdates = MutableLiveData<Unit>(Unit)
	val refreshing = MutableLiveData<Boolean>(false)
	val loading = MutableLiveData<Boolean>()
	
	fun toggleBookmark(bookMetadata: bookstore.BookMetadata) = viewModelScope.launch(Dispatchers.IO) {
		val book = bookstore.Book(title = bookMetadata.title, url = bookMetadata.url)
		when (bookstore.bookLibrary.exist(url = book.url))
		{
			true -> bookstore.bookLibrary.remove(book)
			false -> bookstore.bookLibrary.insert(book)
		}
	}
	
	private val catalogListTemp = arrayListOf<bookstore.BookMetadata>()
	private var loadCatalogJob: Job? = null
	fun loadCatalog()
	{
		if (loadCatalogJob?.isActive == true) return
		loadCatalogJob = GlobalScope.launch {
			refreshing.postValue(true)
			val res = downloadSourceCatalog(source)
			if (isActive) when (res)
			{
				is Response.Error -> refreshing.postValue(false)
				is Response.Success ->
				{
					catalogList.clear()
					catalogList.addAll(res.data)
					catalogListUpdates.postValue(Unit)
					refreshing.postValue(false)
				}
			}
		}
	}
	
	private var searchCatalogJob: Job? = null
	fun searchCatalog(input: String)
	{
		searchCatalogJob?.cancel()
		searchCatalogJob = GlobalScope.launch {
			catalogListTemp.clear()
			catalogListTemp.addAll(catalogList)
			catalogList.clear()
			catalogListUpdates.postValue(Unit)
			loading.postValue(true)
			val res = source.getSearchResult(input)
			if (isActive) when (res)
			{
				is Response.Success ->
				{
					catalogList.addAll(res.data)
					catalogListUpdates.postValue(Unit)
					loading.postValue(false)
				}
				is Response.Error -> loading.postValue(false)
			}
		}
	}
	
	fun exitSearchCatalogMode()
	{
		mode = Mode.MAIN
		searchCatalogJob?.cancel()
		catalogList.clear()
		catalogList.addAll(catalogListTemp)
		catalogListUpdates.postValue(Unit)
	}
	
	fun exitLoadCatalogMode()
	{
		mode = Mode.BAR_SEARCH
		loadCatalogJob?.cancel()
	}
}