package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.viewModelScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class DatabaseSearchResultsModel : BaseViewModel()
{
	fun initialization(database: scrubber.database_interface, input: DatabaseSearchResultsActivity.SearchMode) = callOneTime {
		this.database = database
		this.fetchIterator = FetchIterator(viewModelScope) { index ->
			when (input)
			{
				is DatabaseSearchResultsActivity.SearchMode.Text -> database.getSearch(index, input.text)
				is DatabaseSearchResultsActivity.SearchMode.Genres -> database.getSearchAdvanced(index, input.genresIncludeId, input.genresExcludeId)
				is DatabaseSearchResultsActivity.SearchMode.AuthorSeries -> database.getSearchAuthorSeries(index, input.urlAuthorPage)
			}
		}
		this.fetchIterator.fetchNext()
	}
	
	lateinit var fetchIterator: FetchIterator<BookMetadata>
	lateinit var database: scrubber.database_interface
}


