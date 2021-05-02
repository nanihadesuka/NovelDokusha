package my.noveldokusha.ui.globalSourceSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.BooksFetchIterator
import my.noveldokusha.bookstore
import my.noveldokusha.scrubber

class GlobalSourceSearchModel : ViewModel()
{
	private var initialized = false
	fun initialization(input: String)
	{
		if (initialized) return else initialized = true
		this.input = input
		globalResults.addAll(scrubber.sourcesListCatalog.map { SourceResults(it, input, viewModelScope) })
	}
	
	lateinit var input: String
	val globalResults = ArrayList<SourceResults>()
	
	data class SourceResults(val source: scrubber.source_interface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
	{
		val list = mutableListOf<bookstore.BookMetadata>()
		val booksFetchIterator = BooksFetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }
		
		init
		{
			booksFetchIterator.fetchNext()
		}
	}
}