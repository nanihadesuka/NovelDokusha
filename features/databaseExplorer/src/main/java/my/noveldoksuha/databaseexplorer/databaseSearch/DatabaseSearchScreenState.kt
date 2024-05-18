package my.noveldoksuha.databaseexplorer.databaseSearch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldokusha.core.appPreferences.ListLayoutMode
import my.noveldokusha.tooling.local_database.BookMetadata

internal data class DatabaseSearchScreenState(
    val databaseNameStrId: State<Int>,
    val searchMode: MutableState<SearchMode>,
    val searchTextInput: MutableState<String>,
    val genresList: SnapshotStateList<GenreItem>,
    val listLayoutMode: MutableState<ListLayoutMode>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
)