package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.map
import my.noveldokusha.data.persistentCacheDatabaseSearchGenres
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_Parcelable
import javax.inject.Inject

interface DatabaseSearchStateBundle {
    val extras: DatabaseSearchExtras
}

data class GenreItem(val genre: String, val genreId: String, val state: ToggleableState)
enum class SearchMode {
    BookTitle, BookGenres, Catalog
}

private sealed interface SearchInputState {
    object Catalog : SearchInputState
    data class BookTitle(val text: String) : SearchInputState
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

    override val extras: DatabaseSearchExtras by StateExtra_Parcelable(state)

    val database = scraper.getCompatibleDatabase(extras.databaseBaseUrl)!!
    val listLayout by appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope)

    @Volatile
    private var booksSearch: SearchInputState = SearchInputState.BookTitle("")

    val searchMode = mutableStateOf(SearchMode.Catalog)
    val inputSearch = mutableStateOf("")
    val filtersSearch = mutableStateListOf<GenreItem>()

    val fetchIterator = PagedListIteratorState(viewModelScope) { index ->
        when (val input = booksSearch) {
            is SearchInputState.Catalog -> database.getCatalog(index = index)
            is SearchInputState.BookTitle -> database.searchByTitle(
                index = index,
                input = input.text
            )
            is SearchInputState.Filters -> database.searchByFilters(
                index = index,
                genresIncludedId = input.genresIncludedId,
                genresExcludedId = input.genresExcludedId
            )
        }
    }

    private val searchGenresCache = persistentCacheDatabaseSearchGenres(database, app.cacheDir)

    init {
        viewModelScope.launch {
            val genresLoading = async { getGenres().onSuccess { filtersSearch.addAll(it) } }

            when (val extras = extras) {
                is DatabaseSearchExtras.Catalog -> {
                    searchMode.value = SearchMode.Catalog
                    onSearchCatalogSubmit()
                }
                is DatabaseSearchExtras.Genres -> {
                    searchMode.value = SearchMode.BookGenres
                    genresLoading.await()
                    val newFiltersSearch = filtersSearch.map {
                        when (it.genreId) {
                            in extras.includedGenresIds -> it.copy(state = ToggleableState.On)
                            in extras.excludedGenresIds -> it.copy(state = ToggleableState.Indeterminate)
                            else -> it.copy(state = ToggleableState.Off)
                        }
                    }
                    filtersSearch.clear()
                    filtersSearch.addAll(newFiltersSearch)
                    onSearchGenresSubmit()
                }
                is DatabaseSearchExtras.Title -> {
                    searchMode.value = SearchMode.BookTitle
                    inputSearch.value = extras.title
                    onSearchInputSubmit()
                }
            }
        }
    }

    private suspend fun getGenres() = withContext(Dispatchers.Default) {
        searchGenresCache
            .fetch { database.getSearchFilters() }
            .map { list ->
                list.map {
                    GenreItem(
                        genreId = it.id,
                        genre = it.genreName,
                        state = ToggleableState.Off
                    )
                }
            }
    }

    fun onSearchGenresSubmit() {
        if (searchMode.value != SearchMode.BookGenres) return

        booksSearch = SearchInputState.Filters(
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

    fun onSearchCatalogSubmit() {
        if (searchMode.value != SearchMode.Catalog) return

        booksSearch = SearchInputState.Catalog
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }

    fun onSearchInputSubmit() {
        if (searchMode.value != SearchMode.BookTitle) return
        if (inputSearch.value.isBlank()) return

        booksSearch = SearchInputState.BookTitle(text = inputSearch.value)
        fetchIterator.reset()
        fetchIterator.fetchNext()
    }
}