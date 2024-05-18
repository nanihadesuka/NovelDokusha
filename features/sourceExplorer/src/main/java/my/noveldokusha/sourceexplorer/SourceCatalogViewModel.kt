package my.noveldokusha.sourceexplorer

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.BaseViewModel
import my.noveldoksuha.coreui.components.ToolbarMode
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.mappers.mapToBookMetadata
import my.noveldokusha.core.Toasty
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.utils.StateExtra_String
import my.noveldokusha.core.utils.asMutableStateOf
import my.noveldokusha.scraper.Scraper
import javax.inject.Inject

interface SourceCatalogStateBundle {
    var sourceBaseUrl: String
}


@HiltViewModel
internal class SourceCatalogViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val toasty: Toasty,
    stateHandle: SavedStateHandle,
    appPreferences: AppPreferences,
    scraper: Scraper,
) : BaseViewModel(), SourceCatalogStateBundle {

    override var sourceBaseUrl by StateExtra_String(stateHandle)
    private val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!

    val state = SourceCatalogScreenState(
        sourceCatalogNameStrId = mutableStateOf(source.nameStrId),
        searchTextInput = stateHandle.asMutableStateOf("searchTextInput") { "" },
        toolbarMode = stateHandle.asMutableStateOf("toolbarMode") { ToolbarMode.MAIN },
        fetchIterator = PagedListIteratorState(viewModelScope) {
            source.getCatalogList(it).mapToBookMetadata()
        },
        listLayoutMode = appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope),
    )

    init {
        onSearchCatalog()
    }

    fun onSearchCatalog() {
        state.fetchIterator.setFunction { source.getCatalogList(it).mapToBookMetadata() }
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun onSearchText(input: String) {
        state.fetchIterator.setFunction { source.getCatalogSearch(it, input).mapToBookMetadata() }
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun addToLibraryToggle(book: my.noveldokusha.tooling.local_database.BookMetadata) =
        viewModelScope.launch(Dispatchers.IO)
        {
            val isInLibrary =
                appRepository.toggleBookmark(bookUrl = book.url, bookTitle = book.title)
            val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
            toasty.show(res)
        }
}