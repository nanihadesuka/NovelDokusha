package my.noveldokusha.ui.screens.main.library

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.repository.Repository
import my.noveldokusha.services.LibraryUpdateService
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.toState
import javax.inject.Inject

@HiltViewModel
class LibraryPageViewModel @Inject constructor(
    private val repository: Repository,
    private val preferences: AppPreferences,
    private val toasty: Toasty,
    @ApplicationContext private val context: Context
) : BaseViewModel() {
    var isPullRefreshing by mutableStateOf(false)
    val listReading by createPageList(isShowCompleted = false)
    val listCompleted by createPageList(isShowCompleted = true)

    private fun createPageList(isShowCompleted: Boolean) = repository.libraryBooks
        .getBooksInLibraryWithContextFlow
        .map { it.filter { book -> book.book.completed == isShowCompleted } }
        .combine(preferences.LIBRARY_FILTER_READ.flow()) { list, filterRead ->
            when (filterRead) {
                AppPreferences.TERNARY_STATE.active -> list.filter { it.chaptersCount == it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inverse -> list.filter { it.chaptersCount != it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }.combine(preferences.LIBRARY_SORT_LAST_READ.flow()) { list, sortRead ->
            when (sortRead) {
                AppPreferences.TERNARY_STATE.active -> list.sortedByDescending { it.book.lastReadEpochTimeMilli }
                AppPreferences.TERNARY_STATE.inverse -> list.sortedBy { it.book.lastReadEpochTimeMilli }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }
        .toState(viewModelScope, listOf())


    private fun showLoadingSpinner() {
        viewModelScope.launch {
            // Keep for 3 seconds so the user can notice the refresh has been triggered.
            isPullRefreshing = true
            delay(3000L)
            isPullRefreshing = false
        }
    }

    fun onLibraryCategoryRefresh(isCompletedCategory: Boolean) {
        showLoadingSpinner()
        toasty.show(R.string.updaing_library)
        LibraryUpdateService.start(
            ctx = context,
            completedCategory = isCompletedCategory
        )
    }
}
