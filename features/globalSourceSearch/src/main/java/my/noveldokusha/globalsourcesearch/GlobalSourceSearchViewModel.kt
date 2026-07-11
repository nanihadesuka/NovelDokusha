package my.noveldokusha.globalsourcesearch

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.BaseViewModel
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldoksuha.data.CatalogItem
import my.noveldoksuha.data.ScraperRepository
import my.noveldokusha.core.utils.StateExtra_String
import my.noveldokusha.core.utils.asMutableStateOf
import javax.inject.Inject

internal interface GlobalSourceSearchStateBundle {
    val initialInput: String
}

@HiltViewModel
internal class GlobalSourceSearchViewModel @Inject constructor(
    state: SavedStateHandle,
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

internal data class SourceResults(
    val source: CatalogItem,
    val searchInput: String,
    val coroutineScope: CoroutineScope
) {
    val fetchIterator = PagedListIteratorState(coroutineScope) {
        source.catalog.getCatalogSearch(it, searchInput)
    }

    init {
        fetchIterator.fetchNext()
    }
}
