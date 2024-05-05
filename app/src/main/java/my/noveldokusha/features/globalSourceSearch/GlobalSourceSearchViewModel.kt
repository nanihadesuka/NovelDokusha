package my.noveldokusha.features.globalSourceSearch

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import my.noveldokusha.core.AppPreferences
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.repository.CatalogItem
import my.noveldokusha.repository.ScraperRepository
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import my.noveldokusha.utils.asMutableStateOf
import javax.inject.Inject

interface GlobalSourceSearchStateBundle {
    val initialInput: String
}

@HiltViewModel
class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val scraperRepository: ScraperRepository,
) : BaseViewModel(), GlobalSourceSearchStateBundle {
    override val initialInput by StateExtra_String(state)

    @Volatile
    private var searchJob: Job? = null

    val searchInput = state.asMutableStateOf("searchInput") { initialInput }
    val sourcesResults = mutableStateListOf<SourceResults>()

    init {
        search(text = searchInput.value)
    }

    fun search(text: String) {
        if (text.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            sourcesResults.clear()
            scraperRepository.sourcesCatalogListFlow()
                .take(1)
                .collect { sources ->
                    sources.map { source ->
                        SourceResults(
                            source = source,
                            searchInput = text,
                            coroutineScope = this@launch
                        )
                    }.let(sourcesResults::addAll)
                }
        }
    }

}

data class SourceResults(
    val source: CatalogItem,
    val searchInput: String,
    val coroutineScope: CoroutineScope
) {
    val fetchIterator =
        PagedListIteratorState(coroutineScope) {
            source.catalog.getCatalogSearch(it, searchInput)
        }

    init {
        fetchIterator.fetchNext()
    }
}
