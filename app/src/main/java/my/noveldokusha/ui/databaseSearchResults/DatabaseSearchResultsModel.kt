package my.noveldokusha.ui.databaseSearchResults

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import my.noveldokusha.bookstore
import my.noveldokusha.scrubber

class DatabaseSearchResultsModel : ViewModel()
{
	private var initialized = false
	fun initialization(database: scrubber.database_interface, input: SearchMode)
	{
		if (initialized) return else initialized = true
		
		this.database = database
		this.input = input
		this.iterator = when (input)
		{
			is SearchMode.Text -> database.getSearch(input.text)
			is SearchMode.Advanced -> database.getSearchAdvanced(input.genresInclude, input.genresExclude)
		}.iterator()
		
		loadMore()
	}
	
	val searchResults = ArrayList<bookstore.BookMetadata>()
	val searchResultsUpdate = MutableLiveData<Unit>()
	lateinit var database: scrubber.database_interface
	private lateinit var input: SearchMode
	lateinit var iterator: Iterator<scrubber.ReturnSearch>
	var state: STATE = STATE.LOADING
	
	enum class STATE
	{ IDLE, LOADING, END }
	
	sealed class SearchMode
	{
		data class Text(val text: String) : SearchMode()
		data class Advanced(val genresInclude: ArrayList<String>, val genresExclude: ArrayList<String>) : SearchMode()
		companion object
		{
			const val name = "SearchMode"
		}
	}
	
	fun loadMore()
	{
		state = STATE.LOADING
		GlobalScope.launch(Dispatchers.IO) {
			state = when (val res = iterator.next())
			{
				is scrubber.ReturnSearch.Entries ->
				{
					searchResults.addAll(res.books)
					searchResultsUpdate.postValue(Unit)
					STATE.IDLE
				}
				is scrubber.ReturnSearch.NoMoreEntries ->
				{
					searchResultsUpdate.postValue(Unit)
					STATE.END
				}
				is scrubber.ReturnSearch.Error ->
				{
					searchResultsUpdate.postValue(Unit)
					STATE.END
				}
			}
		}
	}
}
