package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.viewModelScope
import my.noveldokusha.BookMetadata
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class DatabaseSearchResultsModel(
	val database: scrubber.database_interface,
	val input: DatabaseSearchResultsActivity.SearchMode
) : BaseViewModel()
{
	val fetchIterator: FetchIterator<BookMetadata> = FetchIterator(viewModelScope) { index ->
		when (input)
		{
			is DatabaseSearchResultsActivity.SearchMode.Text -> database.getSearch(index, input.text)
			is DatabaseSearchResultsActivity.SearchMode.Genres -> database.getSearchAdvanced(index, input.genresIncludeId, input.genresExcludeId)
			is DatabaseSearchResultsActivity.SearchMode.AuthorSeries -> database.getSearchAuthorSeries(index, input.urlAuthorPage)
		}
	}
	
	init
	{
		fetchIterator.fetchNext()
	}
}


