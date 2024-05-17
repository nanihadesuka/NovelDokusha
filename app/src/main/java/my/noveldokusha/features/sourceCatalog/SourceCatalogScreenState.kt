package my.noveldokusha.features.sourceCatalog

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.noveldokusha.tooling.local_database.BookMetadata
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldoksuha.coreui.components.ToolbarMode
import my.noveldokusha.core.appPreferences.ListLayoutMode

data class SourceCatalogScreenState(
    val sourceCatalogNameStrId: State<Int>,
    val searchTextInput: MutableState<String>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
    val toolbarMode: MutableState<ToolbarMode>,
    val listLayoutMode: MutableState<ListLayoutMode>,
)