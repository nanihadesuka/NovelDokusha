package my.noveldokusha.ui.databaseSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsModel
import java.util.*
import kotlin.collections.ArrayList

class DatabaseSearchActivity : BaseActivity()
{
	class Extras(val databaseBaseUrl: String)
	{
		fun intent(ctx: Context) = Intent(ctx, DatabaseSearchActivity::class.java).also {
			it.putExtra(::databaseBaseUrl.name, databaseBaseUrl)
		}
	}
	
	private val extras = object
	{
		fun databaseBaseUrl() = intent.extras!!.get(Extras::databaseBaseUrl.name)!! as String
	}
	
	private val viewModel by viewModels<DatabaseSearchModel>()
	private val viewHolder by lazy { ActivityDatabaseSearchBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { GenresAdapter(viewModel.genreList) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(scrubber.getCompatibleDatabase(extras.databaseBaseUrl())!!)
		
		supportActionBar!!.let {
			it.title = "Database"
			it.subtitle = viewModel.database.name.capitalize(Locale.ROOT)
			it.setDisplayHomeAsUpEnabled(true)
		}
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.listView.itemAnimator = DefaultItemAnimator()
		viewHolder.searchByGenreButton.setOnClickListener { _ ->
			val input = DatabaseSearchResultsModel.SearchMode.Advanced(
				genresInclude = ArrayList(viewModel.genreList.filter { it.state == Checkbox3StatesView.STATE.POSITIVE }.map { it.genre }),
				genresExclude = ArrayList(viewModel.genreList.filter { it.state == Checkbox3StatesView.STATE.NEGATIVE }.map { it.genre })
			)
			val intent = DatabaseSearchResultsActivity.Extras(databaseUrlBase = viewModel.database.baseUrl, input = input).intent(this)
			startActivity(intent)
		}
		viewModel.genreListUpdated.observe(this) {
			viewAdapter.listView.notifyDataSetChanged()
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.source_catalog_menu__appbar, menu)
		
		val searchViewItem = menu!!.findItem(R.id.action_search)
		val searchView = searchViewItem.actionView as SearchView
		
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				query?.let {
					val input = DatabaseSearchResultsModel.SearchMode.Text(text = query)
					val intent = DatabaseSearchResultsActivity
						.Extras(databaseUrlBase = viewModel.database.baseUrl, input = input)
						.intent(this@DatabaseSearchActivity)
					startActivity(intent)
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
	
	inner class GenresAdapter(private val list: ArrayList<DatabaseSearchModel.Item>) : RecyclerView.Adapter<GenresAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
			ViewBinder(ActivityDatabaseSearchGenreItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = list[position]
			val itemView = binder.viewHolder
			itemView.item.text = itemData.genre
			itemView.item.onStateChangeListener = { itemData.state = it }
			itemView.item.state = itemData.state
		}
		
		inner class ViewBinder(val viewHolder: ActivityDatabaseSearchGenreItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}