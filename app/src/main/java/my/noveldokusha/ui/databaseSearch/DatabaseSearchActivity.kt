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
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityDatabaseSearchBinding
import my.noveldokusha.databinding.ActivityDatabaseSearchGenreItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.inflater
import my.noveldokusha.uiViews.Checkbox3StatesView
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class DatabaseSearchActivity : BaseActivity()
{
	class IntentData : Intent, DatabaseSearchStateBundle
	{
		override var databaseBaseUrl by Extra_String()

		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseBaseUrl: String) : super(ctx, DatabaseSearchActivity::class.java)
		{
			this.databaseBaseUrl = databaseBaseUrl
		}
	}

	private val viewModel by viewModels<DatabaseSearchViewModel>()
	private val viewBind by lazy { ActivityDatabaseSearchBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { GenresAdapter() }
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)

		supportActionBar!!.let {
			it.title = "Database"
			it.subtitle = viewModel.database.name.capitalize(Locale.ROOT)
			it.setDisplayHomeAsUpEnabled(true)
		}

		viewBind.listView.adapter = viewAdapter.listView
		viewBind.listView.itemAnimator = DefaultItemAnimator()
		viewBind.searchByGenreButton.setOnClickListener { _ ->
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

	override fun onCreateOptionsMenu(menu: Menu): Boolean
	{
		menuInflater.inflate(R.menu.search_menu__appbar, menu)

		val searchViewItem = menu.findItem(R.id.action_search)
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

private class GenresAdapter : MyListAdapter<DatabaseSearchViewModel.Item, GenresAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: DatabaseSearchViewModel.Item, new: DatabaseSearchViewModel.Item) = old.genreId == new.genreId
	override fun areContentsTheSame(old: DatabaseSearchViewModel.Item, new: DatabaseSearchViewModel.Item) = old.state == new.state

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(ActivityDatabaseSearchGenreItemBinding.inflate(parent.inflater, parent, false))

	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val itemData = list[position]
		val itemBind = viewHolder.viewHolder
		itemBind.item.text = itemData.genre
		itemBind.item.onStateChangeListener = { itemData.state = it }
		itemBind.item.state = itemData.state
	}

	class ViewHolder(val viewHolder: ActivityDatabaseSearchGenreItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
