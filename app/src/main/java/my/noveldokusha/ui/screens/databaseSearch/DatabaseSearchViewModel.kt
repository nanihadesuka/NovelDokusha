package my.noveldokusha.ui.screens.databaseSearch

import android.os.Parcelable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.map
import my.noveldokusha.data.persistentCacheDatabaseSearchGenres
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_Parcelable
import my.noveldokusha.utils.asMutableListStateOf
import my.noveldokusha.utils.asMutableStateOf
import javax.inject.Inject

interface DatabaseSearchStateBundle {
    val extras: DatabaseSearchExtras
}

@Parcelize
data class GenreItem(
    val genre: String,
    val genreId: String,
    val state: ToggleableState
) : Parcelable

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
    stateHandle: SavedStateHandle,
    scraper: Scraper,
    val appPreferences: AppPreferences,
    app: App
) : BaseViewModel(), DatabaseSearchStateBundle {

    override val extras: DatabaseSearchExtras by StateExtra_Parcelable(stateHandle)
    private var firstLoad by stateHandle.asMutableStateOf("firstLoad") { true }

    private val database = scraper.getCompatibleDatabase(extras.databaseBaseUrl)!!
    val state = DatabaseSearchScreenState(
        databaseNameStrId = derivedStateOf { database.nameStrId },
        searchMode = stateHandle.asMutableStateOf("searchMode") { SearchMode.Catalog },
        searchTextInput = stateHandle.asMutableStateOf("searchTextInput") { "" },
        genresList = stateHandle.asMutableListStateOf("genresList") { mutableStateListOf() },
        listLayoutMode = appPreferences.BOOKS_LIST_LAYOUT_MODE.state(viewModelScope),
        fetchIterator = createPageLoaderState()
    )

    @Volatile
    private var booksSearch: SearchInputState = SearchInputState.BookTitle("")
    private val searchGenresCache = persistentCacheDatabaseSearchGenres(database, app.cacheDir)
    private fun createPageLoaderState() = PagedListIteratorState(viewModelScope) { index ->
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


    init {
        if (firstLoad) viewModelScope.launch {
            init()
            firstLoad = false
        }
    }

    private suspend fun init() = coroutineScope {
        val genresLoading = async { getGenres().onSuccess { state.genresList.addAll(it) } }

        when (val extras = extras) {
            is DatabaseSearchExtras.Catalog -> {
                state.searchMode.value = SearchMode.Catalog
                onSearchCatalogSubmit()
            }
            is DatabaseSearchExtras.Genres -> {
                state.searchMode.value = SearchMode.BookGenres
                genresLoading.await()
                val newFiltersSearch = state.genresList.map {
                    when (it.genreId) {
                        in extras.includedGenresIds -> it.copy(state = ToggleableState.On)
                        in extras.excludedGenresIds -> it.copy(state = ToggleableState.Indeterminate)
                        else -> it.copy(state = ToggleableState.Off)
                    }
                }
                state.genresList.clear()
                state.genresList.addAll(newFiltersSearch)
                onSearchGenresSubmit()
            }
            is DatabaseSearchExtras.Title -> {
                state.searchMode.value = SearchMode.BookTitle
                state.searchTextInput.value = extras.title
                onSearchInputSubmit()
            }
        }
    }

    fun onSearchGenresSubmit() {
        if (state.searchMode.value != SearchMode.BookGenres) return

        booksSearch = SearchInputState.Filters(
            genresIncludedId = state.genresList
                .filter { it.state == ToggleableState.On }
                .map { it.genreId },
            genresExcludedId = state.genresList
                .filter { it.state == ToggleableState.Indeterminate }
                .map { it.genreId },
        )
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun onSearchCatalogSubmit() {
        if (state.searchMode.value != SearchMode.Catalog) return

        booksSearch = SearchInputState.Catalog
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
    }

    fun onSearchInputSubmit() {
        if (state.searchMode.value != SearchMode.BookTitle) return
        if (state.searchTextInput.value.isBlank()) return

        booksSearch = SearchInputState.BookTitle(text = state.searchTextInput.value)
        state.fetchIterator.reset()
        state.fetchIterator.fetchNext()
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
}