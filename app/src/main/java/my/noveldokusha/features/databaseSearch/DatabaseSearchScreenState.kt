package my.noveldokusha.features.databaseSearch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.core.AppPreferences
import my.noveldokusha.feature.local_database.BookMetadata

data class DatabaseSearchScreenState(
    val databaseNameStrId: State<Int>,
    val searchMode: MutableState<SearchMode>,
    val searchTextInput: MutableState<String>,
    val genresList: SnapshotStateList<GenreItem>,
    val listLayoutMode: MutableState<AppPreferences.LIST_LAYOUT_MODE>,
    val fetchIterator: my.noveldokusha.network.PagedListIteratorState<BookMetadata>,
)