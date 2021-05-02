package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import my.noveldokusha.BooksFetchIterator
import my.noveldokusha.bookstore
import my.noveldokusha.scrubber

class DatabaseSearchResultsModel : ViewModel()
{
	private var initialized = false
	fun initialization(database: scrubber.database_interface, input: SearchMode)
	{
		if (initialized) return else initialized = true
		
		this.database = database
		this.booksFetchIterator = BooksFetchIterator(viewModelScope) { index ->
			when (input)
			{
				is SearchMode.Text -> database.getSearch(index, input.text)
				is SearchMode.Advanced -> database.getSearchAdvanced(index, input.genresInclude, input.genresExclude)
			}
		}
		this.booksFetchIterator.fetchNext()
	}
	
	lateinit var booksFetchIterator: BooksFetchIterator
	lateinit var database: scrubber.database_interface
	val searchResults = ArrayList<bookstore.BookMetadata>()
	
	sealed class SearchMode
	{
		data class Text(val text: String) : SearchMode()
		data class Advanced(val genresInclude: ArrayList<String>, val genresExclude: ArrayList<String>) : SearchMode()
		companion object
		{
			const val name = "SearchMode"
		}
	}
}


