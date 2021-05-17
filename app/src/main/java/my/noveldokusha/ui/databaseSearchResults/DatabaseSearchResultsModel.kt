package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.viewModelScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.BooksFetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class DatabaseSearchResultsModel : BaseViewModel()
{
	fun initialization(database: scrubber.database_interface, input: DatabaseSearchResultsActivity.SearchMode) = callOneTime {
		this.database = database
		this.booksFetchIterator = BooksFetchIterator(viewModelScope) { index ->
			when (input)
			{
				is DatabaseSearchResultsActivity.SearchMode.Text -> database.getSearch(index, input.text)
				is DatabaseSearchResultsActivity.SearchMode.Advanced -> database.getSearchAdvanced(index, input.genresIncludeId, input.genresExcludeId)
			}
		}
		this.booksFetchIterator.fetchNext()
	}
	
	lateinit var booksFetchIterator: BooksFetchIterator
	lateinit var database: scrubber.database_interface
	val searchResults = ArrayList<BookMetadata>()
	
}


