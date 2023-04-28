package my.noveldokusha.ui.screens.main.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.repository.Repository
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.composeViews.BookSettingsDialogState
import my.noveldokusha.utils.asMutableStateOf
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    val appPreferences: AppPreferences,
    private val repository: Repository,
    private val stateHandle: SavedStateHandle,
) : BaseViewModel() {
    var bookSettingsDialogState by stateHandle.asMutableStateOf<BookSettingsDialogState>(
        key = "bookSettingsDialogState",
        default = { BookSettingsDialogState.Hide },
    )
    var showBottomSheet by stateHandle.asMutableStateOf("showBottomSheet") { false }

    var readFilter by appPreferences.LIBRARY_FILTER_READ.state(viewModelScope)
    var readSort by appPreferences.LIBRARY_SORT_LAST_READ.state(viewModelScope)

    fun readFilterToggle() {
        readFilter = readFilter.next()
    }

    fun readSortToggle() {
        readSort = readSort.next()
    }

    fun bookCompletedToggle(bookUrl: String) {
        viewModelScope.launch {
            val book = repository.libraryBooks.get(bookUrl) ?: return@launch
            repository.libraryBooks.update(book.copy(completed = !book.completed))
        }
    }

    fun getBook(bookUrl: String) = repository.libraryBooks.getFlow(bookUrl).filterNotNull()
}

