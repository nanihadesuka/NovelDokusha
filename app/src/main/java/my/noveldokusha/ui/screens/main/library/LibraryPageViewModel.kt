package my.noveldokusha.ui.screens.main.library

import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.accompanist.swiperefresh.SwipeRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.toState
import javax.inject.Inject

@HiltViewModel
class LibraryPageViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    private val preferences: AppPreferences
) : BaseViewModel()
{
    val refreshState = SwipeRefreshState(false)
    val listReading by createPageList(isShowCompleted = false)
    val listCompleted by createPageList(isShowCompleted = true)

    private fun createPageList(isShowCompleted: Boolean) = repository.bookLibrary
        .getBooksInLibraryWithContextFlow
        .map { it.filter { book -> book.book.completed == isShowCompleted } }
        .combine(preferences.LIBRARY_FILTER_READ.flow()) { list, filterRead ->
            when (filterRead)
            {
                AppPreferences.TERNARY_STATE.active -> list.filter { it.chaptersCount == it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inverse -> list.filter { it.chaptersCount != it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }.combine(preferences.LIBRARY_SORT_READ.flow()) { list, sortRead ->
            when (sortRead)
            {
                AppPreferences.TERNARY_STATE.active -> list.sortedBy { it.chaptersCount - it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inverse -> list.sortedByDescending { it.chaptersCount - it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }
        .toState(viewModelScope, listOf())
}
