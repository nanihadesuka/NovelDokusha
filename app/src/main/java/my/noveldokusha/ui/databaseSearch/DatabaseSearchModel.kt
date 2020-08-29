package my.noveldokusha.ui.databaseSearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import my.noveldokusha.scrubber

class DatabaseSearchModel : ViewModel()
{
	private var initialized = false
	fun initialization(database: scrubber.database_interface)
	{
		if (initialized) return else initialized = true
		this.database = database
		genreList.addAll(database.searchGenres.asSequence().map { Item(it.key, Checkbox3StatesView.STATE.NONE) })
		genreListUpdated.postValue(Unit)
	}
	
	data class Item(val genre: String, var state: Checkbox3StatesView.STATE)
	
	lateinit var database: scrubber.database_interface
	val genreListUpdated = MutableLiveData<Unit>()
	val genreList = ArrayList<Item>()
}