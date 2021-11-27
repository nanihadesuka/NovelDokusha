package my.noveldokusha.ui.databaseSearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.data.Repository
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_String
import my.noveldokusha.uiViews.Checkbox3StatesView
import javax.inject.Inject

interface DatabaseSearchStateBundle
{
    var databaseBaseUrl: String
}

@HiltViewModel
class DatabaseSearchModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : BaseViewModel(), DatabaseSearchStateBundle
{
    data class Item(val genre: String, val genreId: String, var state: Checkbox3StatesView.STATE)

    override var databaseBaseUrl by StateExtra_String(state)
    val genreListLiveData = MutableLiveData<List<Item>>()
    val database = scraper.getCompatibleDatabase(databaseBaseUrl)!!

    init
    {
        load()
    }

    private fun load() = viewModelScope.launch(Dispatchers.IO) {
        when (val res = database.getSearchGenres())
        {
            is Response.Success ->
            {
                val list = res.data.asSequence().map { Item(it.key, it.value, Checkbox3StatesView.STATE.NONE) }.toList()
                genreListLiveData.postValue(list)
            }
            is Response.Error -> Unit
        }
    }
}