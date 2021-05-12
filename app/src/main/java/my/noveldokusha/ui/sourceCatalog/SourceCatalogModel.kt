package my.noveldokusha.ui.sourceCatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.BooksFetchIterator
import my.noveldokusha.bookstore
import my.noveldokusha.scrubber

class SourceCatalogModel : ViewModel()
{
	private var initialized: Boolean = false
	fun initialization(source: scrubber.source_interface.catalog)
	{
		if (initialized) return else initialized = true
		this.source = source
		booksFetchIterator = BooksFetchIterator(viewModelScope) { source.getCatalogList(it) }
		startCatalogListMode()
	}
	
	data class CatalogItem(val bookMetadata: BookMetadata)
	{
		val isInLibraryLiveData = bookstore.bookLibrary.existFlow(bookMetadata.url).asLiveData()
	}
	
	val list = arrayListOf<CatalogItem>()
	lateinit var source: scrubber.source_interface.catalog
	lateinit var booksFetchIterator: BooksFetchIterator
	
	fun startCatalogListMode()
	{
		list.clear()
		booksFetchIterator.setFunction { source.getCatalogList(it) }
		booksFetchIterator.reset()
		booksFetchIterator.fetchNext()
	}
	
	fun startCatalogSearchMode(input: String)
	{
		list.clear()
		booksFetchIterator.setFunction { source.getCatalogSearch(it, input) }
		booksFetchIterator.reset()
		booksFetchIterator.fetchNext()
	}
}