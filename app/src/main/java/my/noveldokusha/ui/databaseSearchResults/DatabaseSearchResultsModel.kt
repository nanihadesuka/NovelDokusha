package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.viewModelScope
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.ui.BaseViewModel

class DatabaseSearchResultsModel(
	val database: DatabaseInterface,
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


