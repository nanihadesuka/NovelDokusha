package my.noveldokusha.ui.globalSourceSearch

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.scraper
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import javax.inject.Inject

interface GlobalSourceSearchStateBundle
{
    val input: String
}

@HiltViewModel
class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    val appPreferences: AppPreferences
) : BaseViewModel(), GlobalSourceSearchStateBundle
{
    override val input by StateExtra_String(state)

    val globalResults = appPreferences.SOURCES_LANGUAGES.let { activeLangs ->
        scraper.sourcesListCatalog
            .filter { it.language in activeLangs }
            .map { SourceResults(it, input, viewModelScope) }
    }
}

data class SourceResults(val source: SourceInterface.catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
    var savedState: Parcelable? = null
    val booksFetchIterator = FetchIterator(coroutineScope) { source.getCatalogSearch(it, searchInput) }

    init
    {
        booksFetchIterator.fetchNext()
    }
}
