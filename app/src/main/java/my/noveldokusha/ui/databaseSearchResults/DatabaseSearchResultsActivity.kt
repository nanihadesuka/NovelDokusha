package my.noveldokusha.ui.databaseSearchResults

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsBinding
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.uiUtils.ProgressBarAdapter
import java.io.InvalidObjectException
import java.util.*

class DatabaseSearchResultsActivity : BaseActivity()
{
	class Extras(val databaseUrlBase: String, val input: DatabaseSearchResultsModel.SearchMode)
	{
		fun intent(ctx: Context) = Intent(ctx, DatabaseSearchResultsActivity::class.java).also {
			it.putExtra(::databaseUrlBase.name, databaseUrlBase)
			val subclassName: String = when (input)
			{
				is DatabaseSearchResultsModel.SearchMode.Text ->
				{
					it.putExtra(input::text.name, input.text)
					DatabaseSearchResultsModel.SearchMode.Text::class.simpleName!!
				}
				is DatabaseSearchResultsModel.SearchMode.Advanced ->
				{
					it.putStringArrayListExtra(input::genresInclude.name, input.genresInclude)
					it.putStringArrayListExtra(input::genresExclude.name, input.genresExclude)
					DatabaseSearchResultsModel.SearchMode.Advanced::class.simpleName!!
				}
			}
			it.putExtra(DatabaseSearchResultsModel.SearchMode.name, subclassName)
		}
	}
	
	private val extras = object
	{
		fun databaseUrlBase(): String = intent.extras!!.getString(Extras::databaseUrlBase.name)!!
		fun input(): DatabaseSearchResultsModel.SearchMode = when (intent.extras!!.getString(DatabaseSearchResultsModel.SearchMode.name)!!)
		{
			DatabaseSearchResultsModel.SearchMode.Text::class.simpleName!! -> DatabaseSearchResultsModel.SearchMode.Text(text = intent.extras!!.getString(DatabaseSearchResultsModel.SearchMode.Text::text.name)!!)
			DatabaseSearchResultsModel.SearchMode.Advanced::class.simpleName!! -> DatabaseSearchResultsModel.SearchMode.Advanced(
				genresInclude = intent.extras!!.getStringArrayList(DatabaseSearchResultsModel.SearchMode.Advanced::genresInclude.name)!!,
				genresExclude = intent.extras!!.getStringArrayList(DatabaseSearchResultsModel.SearchMode.Advanced::genresExclude.name)!!
			)
			else -> throw InvalidObjectException("Invalid SearchMode subclass name")
		}
	}
	
	private val viewModel by viewModels<DatabaseSearchResultsModel>()
	private val viewHolder by lazy { ActivityDatabaseSearchResultsBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { ChaptersArrayAdapter(viewModel.searchResults) }
		val progressBar by lazy { ProgressBarAdapter() }
	}
	private val viewLayoutManager = object
	{
		val recyclerView by lazy { LinearLayoutManager(this@DatabaseSearchResultsActivity) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(database = scrubber.getCompatibleDatabase(extras.databaseUrlBase())!!, input = extras.input())
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			when (viewModel.state)
			{
				DatabaseSearchResultsModel.STATE.IDLE ->
				{
					val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
					if (pos >= viewModel.searchResults.size - 3)
					{
						viewAdapter.progressBar.visible = true
						viewModel.loadMore()
					}
				}
				DatabaseSearchResultsModel.STATE.LOADING -> Unit
				DatabaseSearchResultsModel.STATE.END -> Unit
			}
		}
		
		viewModel.searchResultsUpdate.observe(this) {
			viewAdapter.progressBar.visible = false
			viewHolder.noResultsMessage.visibility = if (viewModel.searchResults.isEmpty()) View.VISIBLE else View.GONE
			viewAdapter.recyclerView.notifyDataSetChanged()
		}
		
		supportActionBar!!.let {
			it.title = "Database search"
			it.subtitle = viewModel.database.name.capitalize(Locale.ROOT)
			it.setDisplayHomeAsUpEnabled(true)
		}
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		android.R.id.home -> this.onBackPressed().let { true }
		else -> super.onOptionsItemSelected(item)
	}
	
	private inner class ChaptersArrayAdapter(private val list: ArrayList<bookstore.BookMetadata>) : RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			ViewBinder(ActivityDatabaseSearchResultsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = this@ChaptersArrayAdapter.list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemHolder = binder.viewHolder
			itemHolder.title.text = itemData.title
			itemHolder.title.setOnClickListener {
				val intent = DatabaseBookInfoActivity
					.Extras(databaseUrlBase = viewModel.database.baseUrl, bookMetadata = itemData)
					.intent(this@DatabaseSearchResultsActivity)
				startActivity(intent)
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityDatabaseSearchResultsListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
	
}