package my.noveldokusha.ui.databaseSearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiViews.Checkbox3StatesView

class DatabaseSearchModel(val database: scrubber.database_interface) : BaseViewModel()
{
	init
	{
		viewModelScope.launch(Dispatchers.IO) {
			when (val res = database.getSearchGenres())
			{
				is Response.Success ->
				{
					val list = res.data.asSequence().map { Item(it.key, it.value, Checkbox3StatesView.STATE.NONE) }.toList()
					withContext(Dispatchers.Main)
					{
						genreListLiveData.postValue(list)
					}
				}
				is Response.Error -> Unit
			}
		}
	}
	
	data class Item(val genre: String, val genreId: String, var state: Checkbox3StatesView.STATE)
	
	val genreListLiveData = MutableLiveData<List<Item>>()
}