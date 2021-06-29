package my.noveldokusha.ui.databaseSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityDatabaseSearchBinding
import my.noveldokusha.databinding.ActivityDatabaseSearchGenreItemBinding
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.inflater
import my.noveldokusha.uiViews.Checkbox3StatesView
import java.util.*
import kotlin.collections.ArrayList

class DatabaseSearchActivity : BaseActivity()
{
	
	class IntentData : Intent
	{
		var databaseBaseUrl by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseBaseUrl: String) : super(ctx, DatabaseSearchActivity::class.java)
		{
			this.databaseBaseUrl = databaseBaseUrl
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<DatabaseSearchModel>()
	private val viewHolder by lazy { ActivityDatabaseSearchBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { GenresAdapter() }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(scrubber.getCompatibleDatabase(extras.databaseBaseUrl)!!)
		
		supportActionBar!!.let {
			it.title = "Database"
			it.subtitle = viewModel.database.name.capitalize(Locale.ROOT)
			it.setDisplayHomeAsUpEnabled(true)
		}
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.listView.itemAnimator = DefaultItemAnimator()
		viewHolder.searchByGenreButton.setOnClickListener { _ ->
			val list = viewModel.genreListLiveData.value ?: return@setOnClickListener
			val input = DatabaseSearchResultsActivity.SearchMode.Genres(
				genresIncludeId = ArrayList(list.filter { it.state == Checkbox3StatesView.STATE.POSITIVE }.map { it.genreId }),
				genresExcludeId = ArrayList(list.filter { it.state == Checkbox3StatesView.STATE.NEGATIVE }.map { it.genreId })
			)
			
			DatabaseSearchResultsActivity
				.IntentData(this@DatabaseSearchActivity, databaseUrlBase = viewModel.database.baseUrl, input = input)
				.let(this@DatabaseSearchActivity::startActivity)
		}
		viewModel.genreListLiveData.observe(this) { list ->
			viewAdapter.listView.list = list
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.search_menu__appbar, menu)
		
		val searchViewItem = menu!!.findItem(R.id.action_search)
		val searchView = searchViewItem.actionView as SearchView
		
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				query?.let {
					DatabaseSearchResultsActivity
						.IntentData(
							this@DatabaseSearchActivity,
							databaseUrlBase = viewModel.database.baseUrl,
							input = DatabaseSearchResultsActivity.SearchMode.Text(text = it)
						).let(this@DatabaseSearchActivity::startActivity)
				}
				return true
			}
			
			override fun onQueryTextChange(newText: String?): Boolean = true
		})
		
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		android.R.id.home ->
		{
			this.onBackPressed()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}
	
}

private class GenresAdapter : MyListAdapter<DatabaseSearchModel.Item, GenresAdapter.ViewBinder>()
{
	override fun areItemsTheSame(old: DatabaseSearchModel.Item, new: DatabaseSearchModel.Item) = old.genreId == new.genreId
	override fun areContentsTheSame(old: DatabaseSearchModel.Item, new: DatabaseSearchModel.Item) = old.state == new.state
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
		ViewBinder(ActivityDatabaseSearchGenreItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val itemData = list[position]
		val itemView = binder.viewHolder
		itemView.item.text = itemData.genre
		itemView.item.onStateChangeListener = { itemData.state = it }
		itemView.item.state = itemData.state
	}
	
	class ViewBinder(val viewHolder: ActivityDatabaseSearchGenreItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
