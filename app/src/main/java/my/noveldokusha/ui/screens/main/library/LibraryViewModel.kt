package my.noveldokusha.ui.screens.main.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.repository.Repository
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.screens.main.BookSettingsDialogState
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    val appPreferences: AppPreferences,
    private val repository: Repository,
) : BaseViewModel() {

    var bookSettingsDialogState by mutableStateOf<BookSettingsDialogState>(BookSettingsDialogState.Hide)
    var showBottomSheet by mutableStateOf(false)

    val readFilter by appPreferences.LIBRARY_FILTER_READ.state(viewModelScope)
    val readSort by appPreferences.LIBRARY_SORT_LAST_READ.state(viewModelScope)

    fun readFilterToggle() {
        appPreferences.LIBRARY_FILTER_READ.value = readFilter.next()
    }

    fun readSortToggle() {
        appPreferences.LIBRARY_SORT_LAST_READ.value = readSort.next()
    }

    fun bookCompletedToggle(bookUrl: String) {
        viewModelScope.launch {
            val book = repository.libraryBooks.get(bookUrl) ?: return@launch
            repository.libraryBooks.update(book.copy(completed = !book.completed))
        }
    }

    fun getBook(bookUrl: String) = repository.libraryBooks.getFlow(bookUrl).filterNotNull()
}

