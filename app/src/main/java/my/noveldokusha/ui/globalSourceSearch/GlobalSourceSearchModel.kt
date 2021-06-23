package my.noveldokusha.ui.globalSourceSearch

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class GlobalSourceSearchModel : BaseViewModel()
{
	fun initialization(input: String) = callOneTime {
		globalResults.addAll(scrubber.sourcesListCatalog.map { SourceResults(it, input, viewModelScope) })
	}
	
	val globalResults = ArrayList<SourceResults>()
}

data class SourceResults(val source: scrubber.source_interface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
	var savedState: Parcelable? = null
	val list = ArrayList<BookMetadata>()
	val booksFetchIterator = FetchIterator(coroutineScope, list) { source.getCatalogSearch(it, searchInput) }
	
	init
	{
		booksFetchIterator.fetchNext()
	}
}
