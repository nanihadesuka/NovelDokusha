package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.repository.Repository
import my.noveldokusha.data.persistentCacheDatabaseSearchGenres
import my.noveldokusha.data.Response
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface DatabaseSearchStateBundle {
    var databaseBaseUrl: String
}

data class GenreItem(val genre: String, val genreId: String, var state: ToggleableState)

@HiltViewModel
class DatabaseSearchViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle,
    private val scraper: Scraper,
    app: App
) : BaseViewModel(), DatabaseSearchStateBundle {

    override var databaseBaseUrl by StateExtra_String(state)
    var searchText by mutableStateOf("")
    var genresList = mutableStateListOf<GenreItem>()
    val database get() = scraper.getCompatibleDatabase(databaseBaseUrl)!!

    private val searchGenresCache = persistentCacheDatabaseSearchGenres(database, app.cacheDir)

    init {
        load()
    }

    private suspend fun getGenres() = withContext(Dispatchers.IO) {
        searchGenresCache.fetch { database.getSearchGenres() }
    }

    private fun load() = viewModelScope.launch(Dispatchers.IO) {

        when (val res = getGenres()) {
            is Response.Success -> {
                val list = res.data
                    .asSequence()
                    .map { GenreItem(it.key, it.value, ToggleableState.Off) }
                    .toList()
                withContext(Dispatchers.Main) {
                    genresList.addAll(list)
                }
            }
            is Response.Error -> Unit
        }
    }
}