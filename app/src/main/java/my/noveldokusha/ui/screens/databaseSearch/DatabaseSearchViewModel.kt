package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.map
import my.noveldokusha.data.persistentCacheDatabaseSearchGenres
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface DatabaseSearchStateBundle {
    var databaseBaseUrl: String
}

data class GenreItem(val genre: String, val genreId: String, val state: ToggleableState)
enum class SearchMode {
    BookTitle, BookGenres, AuthorName
}

sealed interface SearchInputState {
    data class BookTitle(val text: String) : SearchInputState
    data class BookAuthor(val authorName: String) : SearchInputState
    data class Filters(
        val genresIncludedId: List<String>,
        val genresExcludedId: List<String>,
    ) : SearchInputState
}

@HiltViewModel
class DatabaseSearchViewModel @Inject constructor(
    state: SavedStateHandle,
    scraper: Scraper,
    val appPreferences: AppPreferences,
    app: App
) : BaseViewModel(), DatabaseSearchStateBundle {

    override var databaseBaseUrl by StateExtra_String(state)

    val database = scraper.getCompatibleDatabase(databaseBaseUrl)!!
    val listLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)

    @Volatile
    var inputState: SearchInputState = SearchInputState.BookTitle("")
    val searchMode = mutableStateOf<SearchMode>(SearchMode.BookTitle)
    val inputSearch = mutableStateOf("")
    val filtersSearch = mutableStateListOf<GenreItem>()

    val fetchIterator = PagedListIteratorState(viewModelScope) { index ->
        when (val input = inputState) {
            is SearchInputState.BookAuthor -> database.getSearchAuthorSeries(
                index = index,
                urlAuthorPage = input.authorName
            )
            is SearchInputState.BookTitle -> database.getSearch(
                index = index,
                input = input.text
            )
            is SearchInputState.Filters -> database.getSearchAdvanced(
                index = index,
                genresIncludedId = input.genresIncludedId,
                genresExcludedId = input.genresExcludedId
            )
        }
    }

    private val searchGenresCache = persistentCacheDatabaseSearchGenres(database, app.cacheDir)

    init {
        viewModelScope.launch {
            getGenres().onSuccess { filtersSearch.addAll(it) }
            when (searchMode.value) {
                SearchMode.BookTitle, SearchMode.AuthorName -> onSearchInputSubmit()
                SearchMode.BookGenres -> onSearchGenresSubmit()
            }
        }
    }

    private suspend fun getGenres() = withContext(Dispatchers.Default) {
        searchGenresCache
            .fetch { database.getSearchGenres() }
            .map { list ->
                list.map {
                    GenreItem(genreId = it.id, genre = it.genreName, state = ToggleableState.Off)
                }
            }
    }

    fun onSearchGenresSubmit() {
        if (searchMode.value != SearchMode.BookGenres) return

        inputState = SearchInputState.Filters(
            genresIncludedId = filtersSearch
                .filter { it.state == ToggleableState.On }
                .map { it.genreId },
            genresExcludedId = filtersSearch
                .filter { it.state == ToggleableState.Indeterminate }
                .map { it.genreId },
        )
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }


    fun onSearchInputSubmit() {
        inputState = when (searchMode.value) {
            SearchMode.BookGenres -> return
            SearchMode.BookTitle -> SearchInputState.BookTitle(text = inputSearch.value)
            SearchMode.AuthorName -> SearchInputState.BookAuthor(authorName = inputSearch.value)
        }
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }
}