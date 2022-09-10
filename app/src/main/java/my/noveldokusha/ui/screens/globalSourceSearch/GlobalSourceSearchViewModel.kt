package my.noveldokusha.ui.screens.globalSourceSearch

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.repository.ScraperRepository
import my.noveldokusha.repository.SourceCatalogItem
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface GlobalSourceSearchStateBundle {
    val input: String
}

@HiltViewModel
class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val scraperRepository: ScraperRepository,
) : BaseViewModel(), GlobalSourceSearchStateBundle {
    override val input by StateExtra_String(state)

    init {
        viewModelScope.launch {
            scraperRepository.sourcesCatalogListFlow()
                .take(1)
                .collectLatest {
                    it.map { source ->
                        SourceResults(
                            source = source,
                            searchInput = input,
                            coroutineScope = viewModelScope
                        )
                    }.let(list::addAll)
                }
        }
    }

    val list = mutableStateListOf<SourceResults>()
}

data class SourceResults(
    val source: SourceCatalogItem,
    val searchInput: String,
    val coroutineScope: CoroutineScope
) {
    val fetchIterator =
        PagedListIteratorState(coroutineScope) { source.catalog.getCatalogSearch(it, searchInput) }

    init {
        fetchIterator.fetchNext()
    }
}
