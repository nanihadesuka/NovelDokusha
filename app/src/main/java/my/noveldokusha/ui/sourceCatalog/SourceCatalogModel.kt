package my.noveldokusha.ui.sourceCatalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast
import javax.inject.Inject

interface SourceCatalogStateBundle
{
    var sourceBaseUrl: String
}

@HiltViewModel
class SourceCatalogModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : BaseViewModel(), SourceCatalogStateBundle
{
    override var sourceBaseUrl by StateExtra_String(state)
    val source = scraper.getCompatibleSourceCatalog(sourceBaseUrl)!!

    val fetchIterator = FetchIterator(viewModelScope) { source.getCatalogList(it).transform() }

    init
    {
        startCatalogListMode()
    }

    private fun Response<List<BookMetadata>>.transform() = when (val res = this)
    {
        is Response.Error -> Response.Error(res.message)
        is Response.Success -> Response.Success(res.data.map { CatalogItem(repository, it) })
    }

    data class CatalogItem(private val repository: Repository, val bookMetadata: BookMetadata)
    {
        val isInLibraryLiveData = repository.bookLibrary.existInLibraryFlow(bookMetadata.url).asLiveData()
    }

    fun startCatalogListMode()
    {
        fetchIterator.setFunction { source.getCatalogList(it).transform() }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun startCatalogSearchMode(input: String)
    {
        fetchIterator.setFunction { source.getCatalogSearch(it, input).transform() }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun addToLibraryToggle(item: CatalogItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.bookLibrary.toggleBookmark(item.bookMetadata)
        val isInLibrary = repository.bookLibrary.existInLibrary(item.bookMetadata.url)
        val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
        toast(res.stringRes())
    }
}