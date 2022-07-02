package my.noveldokusha.ui.databaseSearchResults

import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.scraper.PagedListIteratorState
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiUtils.StateExtra_StringArrayList
import java.io.InvalidObjectException
import javax.inject.Inject

interface DatabaseSearchResultsStateBundle
{
    var databaseUrlBase: String
    var text: String
    var genresIncludeId: ArrayList<String>
    var genresExcludeId: ArrayList<String>
    var searchMode: String
    var authorName: String
    var urlAuthorPage: String
    val input
        get() = when (this.searchMode)
        {
            DatabaseSearchResultsActivity.SearchMode.Text::class.simpleName -> DatabaseSearchResultsActivity.SearchMode.Text(text = text)
            DatabaseSearchResultsActivity.SearchMode.Genres::class.simpleName -> DatabaseSearchResultsActivity.SearchMode.Genres(
                genresIncludeId = genresIncludeId,
                genresExcludeId = genresExcludeId
            )
            DatabaseSearchResultsActivity.SearchMode.AuthorSeries::class.simpleName -> DatabaseSearchResultsActivity.SearchMode.AuthorSeries(
                authorName = authorName,
                urlAuthorPage = urlAuthorPage
            )
            else -> throw InvalidObjectException("Invalid SearchMode subclass: $searchMode")
        }
    val database get() = scraper.getCompatibleDatabase(databaseUrlBase)!!

    fun set(databaseUrlBase: String, input: DatabaseSearchResultsActivity.SearchMode)
    {
        this.databaseUrlBase = databaseUrlBase
        this.searchMode = input::class.simpleName!!
        when (input)
        {
            is DatabaseSearchResultsActivity.SearchMode.Text ->
            {
                this.text = input.text
            }
            is DatabaseSearchResultsActivity.SearchMode.Genres ->
            {
                this.genresIncludeId = input.genresIncludeId
                this.genresExcludeId = input.genresExcludeId
            }
            is DatabaseSearchResultsActivity.SearchMode.AuthorSeries ->
            {
                this.authorName = input.authorName
                this.urlAuthorPage = input.urlAuthorPage
            }
        }
    }
}

@HiltViewModel
class DatabaseSearchResultsViewModel @Inject constructor(
    val state: SavedStateHandle,
    val appPreferences: AppPreferences
) : BaseViewModel(), DatabaseSearchResultsStateBundle
{
    override var databaseUrlBase by StateExtra_String(state)
    override var text by StateExtra_String(state)
    override var genresIncludeId by StateExtra_StringArrayList(state)
    override var genresExcludeId by StateExtra_StringArrayList(state)
    override var searchMode by StateExtra_String(state)
    override var authorName by StateExtra_String(state)
    override var urlAuthorPage by StateExtra_String(state)

    val listLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)
    val fetchIterator = PagedListIteratorState(viewModelScope) { index ->
        when (val input = this.input)
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


