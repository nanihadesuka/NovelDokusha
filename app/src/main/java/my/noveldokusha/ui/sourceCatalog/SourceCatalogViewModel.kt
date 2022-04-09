package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiUtils.toast
import javax.inject.Inject

interface SourceCatalogStateBundle {
    var sourceBaseUrl: String
}

enum class Mode { CATALOG, SEARCH }

@HiltViewModel
class SourceCatalogViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : BaseViewModel(), SourceCatalogStateBundle {

    override var sourceBaseUrl by StateExtra_String(state)

    val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!
    val fetchIterator = FetchIteratorState(viewModelScope) { source.getCatalogList(it).transform() }

    init {
        startCatalogListMode()
    }

    var mode by mutableStateOf(Mode.CATALOG)
    val listLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)

    private fun Response<List<BookMetadata>>.transform() = when (val res = this) {
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