package my.noveldokusha.ui.main.library

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    val appPreferences: AppPreferences,
    private val repository: Repository,
) : BaseViewModel()
{
    @OptIn(ExperimentalMaterialApi::class)
    val bottomSheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden)

    fun setBookAsCompleted(book: Book, completed: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        repository.bookLibrary.update(book.copy(completed = completed))
    }

    var optionsBookDialogState by mutableStateOf<OptionsBookDialogState>(OptionsBookDialogState.Hide)

    val readFilter by appPreferences.LIBRARY_FILTER_READ.state(viewModelScope)
    val readSort by appPreferences.LIBRARY_SORT_READ.state(viewModelScope)

    fun readFilterToggle()
    {
        appPreferences.LIBRARY_FILTER_READ.value = appPreferences.LIBRARY_FILTER_READ.value.next()
    }

    fun readSortToggle()
    {
        appPreferences.LIBRARY_SORT_READ.value = appPreferences.LIBRARY_SORT_READ.value.next()
    }

    fun bookCompletedToggle(bookUrl: String)
    {
        viewModelScope.launch {
            val book = repository.BookLibrary().get(bookUrl) ?: return@launch
            repository.BookLibrary().update(book.copy(completed = !book.completed))
        }
    }

    fun getBook(bookUrl: String) = repository.BookLibrary().getFlow(bookUrl).filterNotNull()
}

sealed interface OptionsBookDialogState
{
    object Hide : OptionsBookDialogState
    data class Show(val book: Book) : OptionsBookDialogState
}
