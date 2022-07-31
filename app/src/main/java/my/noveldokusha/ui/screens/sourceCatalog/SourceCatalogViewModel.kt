package my.noveldokusha.ui.screens.sourceCatalog

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.PagedList
import my.noveldokusha.scraper.PagedListIteratorState
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import my.noveldokusha.utils.toast
import javax.inject.Inject

interface SourceCatalogStateBundle {
    var sourceBaseUrl: String
}

@HiltViewModel
class SourceCatalogViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle,
    @ApplicationContext private val context: Context,
    appPreferences: AppPreferences
) : BaseViewModel(), SourceCatalogStateBundle {

    override var sourceBaseUrl by StateExtra_String(state)

    var searchText by state.saveable { mutableStateOf("") }
    val listState = LazyGridState()
    var toolbarMode by state.saveable { mutableStateOf(ToolbarMode.MAIN) }

    val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!
    val fetchIterator =
        PagedListIteratorState(viewModelScope) { source.getCatalogList(it).transform() }

    init {
        startCatalogListMode()
    }

    val listLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)

    private fun Response<PagedList<BookMetadata>>.transform() = when (val res = this) {
        is Response.Error -> Response.Error(res.message)
        is Response.Success -> Response.Success(res.data)
    }

    fun startCatalogListMode() {
        fetchIterator.setFunction { source.getCatalogList(it).transform() }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun startCatalogSearchMode(input: String) {
        fetchIterator.setFunction { source.getCatalogSearch(it, input).transform() }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun addToLibraryToggle(book: BookMetadata) = viewModelScope.launch(Dispatchers.IO)
    {
        repository.bookLibrary.toggleBookmark(book)
        val isInLibrary = repository.bookLibrary.existInLibrary(book.url)
        val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
        toast(context.getString(res))
    }
}