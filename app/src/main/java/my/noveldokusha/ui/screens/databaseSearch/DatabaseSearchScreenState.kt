package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.PagedListIteratorState

data class DatabaseSearchScreenState(
    val databaseName: State<String>,
    val searchMode: MutableState<SearchMode>,
    val searchTextInput: MutableState<String>,
    val genresList: SnapshotStateList<GenreItem>,
    val listLayoutMode: MutableState<AppPreferences.LIST_LAYOUT_MODE>,
    val fetchIterator: PagedListIteratorState<BookMetadata>,
)