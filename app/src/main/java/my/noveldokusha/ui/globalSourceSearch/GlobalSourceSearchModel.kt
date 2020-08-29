package my.noveldokusha.ui.globalSourceSearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import my.noveldokusha.Response
import my.noveldokusha.bookstore
import my.noveldokusha.scrubber

class GlobalSourceSearchModel : ViewModel()
{
	private var initialized = false
	fun initialization(input: String)
	{
		if (initialized) return else initialized = true
		this.input = input
		globalResults.addAll(scrubber.sourcesListCatalog.map { SourceResults(it, input) })
		globalResultsUpdated.postValue(Unit)
	}
	
	lateinit var input: String
	val globalResults = ArrayList<SourceResults>()
	val globalResultsUpdated = MutableLiveData<Unit>()
	
	data class SourceResults(val source: scrubber.source_interface.catalog, val searchInput: String)
	{
		val results = mutableListOf<bookstore.BookMetadata>()
		val resultsUpdated = MutableLiveData<Unit>()
		
		init
		{
			GlobalScope.launch(Dispatchers.IO) {
				when (val res = source.getSearchResult(searchInput))
				{
					is Response.Success ->
					{
						results.clear()
						results.addAll(res.data)
						resultsUpdated.postValue(Unit)
					}
					is Response.Error -> resultsUpdated.postValue(Unit)
				}
			}
		}
	}
}