package my.noveldokusha.ui.screens.sourceCatalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface SourceCatalogStateBundle {
    var sourceBaseUrl: String
}

@HiltViewModel
class SourceCatalogViewModel @Inject constructor(
    private val repository: Repository,
    private val toasty: Toasty,
    state: SavedStateHandle,
    appPreferences: AppPreferences,
    scraper: Scraper,
) : BaseViewModel(), SourceCatalogStateBundle {

    override var sourceBaseUrl by StateExtra_String(state)

    var searchTextInput by state.saveable { mutableStateOf("") }
    var toolbarMode by state.saveable { mutableStateOf(ToolbarMode.MAIN) }

    val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!
    val fetchIterator = PagedListIteratorState(viewModelScope) { source.getCatalogList(it) }

    init {
        onSearchCatalog()
    }

    var booksListLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)

    fun onSearchCatalog() {
        fetchIterator.setFunction { source.getCatalogList(it) }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun onSearchText(input: String) {
        fetchIterator.setFunction { source.getCatalogSearch(it, input) }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun addToLibraryToggle(book: BookMetadata) = viewModelScope.launch(Dispatchers.IO)
    {
        repository.libraryBooks.toggleBookmark(book)
        val isInLibrary = repository.libraryBooks.existInLibrary(book.url)
        val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
        toasty.show(res)
    }
}