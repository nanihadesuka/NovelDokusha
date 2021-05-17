package my.noveldokusha.ui.globalSourceSearch

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.BooksFetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class GlobalSourceSearchModel : BaseViewModel()
{
	fun initialization(input: String) = callOneTime {
		this.input = input
		globalResults.addAll(scrubber.sourcesListCatalog.map { SourceResults(it, input, viewModelScope) })
	}
	
	lateinit var input: String
	val globalResults = ArrayList<SourceResults>()
	
	data class SourceResults(val source: scrubber.source_interface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
	{
		var positionOffset: Int? = null
		var position: Int? = null
		
		val list = mutableListOf<BookMetadata>()
		val booksFetchIterator = BooksFetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }
		
		init
		{
			booksFetchIterator.fetchNext()
		}
	}
}