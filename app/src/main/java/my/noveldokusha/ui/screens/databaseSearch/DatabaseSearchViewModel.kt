package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import javax.inject.Inject

interface DatabaseSearchStateBundle {
    var databaseBaseUrl: String
}

data class GenreItem(val genre: String, val genreId: String, var state: ToggleableState)

@HiltViewModel
class DatabaseSearchViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle
) : BaseViewModel(), DatabaseSearchStateBundle {

    override var databaseBaseUrl by StateExtra_String(state)
    val searchText = mutableStateOf("")
    val genresList = mutableStateListOf<GenreItem>()
    val database = scraper.getCompatibleDatabase(databaseBaseUrl)!!

    init {
        load()
    }

    private fun load() = viewModelScope.launch(Dispatchers.IO) {
        when (val res = database.getSearchGenres()) {
            is Response.Success -> {
                val list =
                    res.data.asSequence().map { GenreItem(it.key, it.value, ToggleableState.Off) }
                        .toList()
                withContext(Dispatchers.Main) {
                    genresList.addAll(list)
                }
            }
            is Response.Error -> Unit
        }
    }
}