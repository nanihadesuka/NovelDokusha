package my.noveldokusha.sourceexplorer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.noveldoksuha.coreui.components.ToolbarMode
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.tooling.local_database.BookMetadata

internal data class SourceCatalogScreenState(
    val sourceCatalogNameStrId: State<Int>,
    val searchTextInput: MutableState<String>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
    val toolbarMode: MutableState<ToolbarMode>,
    val listLayoutMode: MutableState<ListLayoutMode>,
)