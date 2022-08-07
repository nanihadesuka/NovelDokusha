package my.noveldokusha.ui.screens.globalSourceSearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.AppPreferences
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface GlobalSourceSearchStateBundle
{
    val input: String
}

@HiltViewModel
class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val scraper: Scraper,
) : BaseViewModel(), GlobalSourceSearchStateBundle
{
    override val input by StateExtra_String(state)

    val list = appPreferences.SOURCES_LANGUAGES.value.let { activeLangs ->
        scraper.sourcesListCatalog
            .filter { it.language in activeLangs }
            .map { SourceResults(it, input, viewModelScope) }
    }
}

data class SourceResults(val source: SourceInterface.Catalog, val searchInput: String, val coroutineScope: CoroutineScope)
{
    val fetchIterator = PagedListIteratorState(coroutineScope) { source.getCatalogSearch(it, searchInput) }

    init
    {
        fetchIterator.fetchNext()
    }
}
