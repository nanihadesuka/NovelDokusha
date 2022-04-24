package my.noveldokusha.ui.main.library

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiViews.Checkbox3StatesView
import java.io.InputStream
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
}