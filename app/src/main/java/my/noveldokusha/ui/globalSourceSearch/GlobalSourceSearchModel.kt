package my.noveldokusha.ui.globalSourceSearch

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class GlobalSourceSearchModel(val input: String) : BaseViewModel()
{
	val globalResults = scrubber.sourcesListCatalog.map { SourceResults(it, input, viewModelScope) }
}

data class SourceResults(val source: scrubber.source_interface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
	var savedState: Parcelable? = null
	val booksFetchIterator = FetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }
	
	init
	{
		booksFetchIterator.fetchNext()
	}
}
