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
import my.noveldokusha.BookMetadata
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.uiUtils.*
import java.io.InvalidObjectException
import java.util.*

class DatabaseSearchResultsActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var databaseUrlBase by Extra_String()
		private var text by Extra_String()
		private var genresInclude by Extra_StringArrayList()
		private var genresExclude by Extra_StringArrayList()
		private var searchMode by Extra_String()
		
		private enum class MODE
		{ TEXT, ADVANCED }
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseUrlBase: String, input: SearchMode) : super(ctx, DatabaseSearchResultsActivity::class.java)
		{
			this.databaseUrlBase = databaseUrlBase
			
			this.searchMode = when (input)
			{
				is SearchMode.Text ->
				{
					this.text = input.text
					MODE.TEXT.name
				}
				is SearchMode.Advanced ->
				{
					this.genresInclude = input.genresInclude
					this.genresExclude = input.genresExclude
					MODE.ADVANCED.name
				}
			}
		}
		
		val input
			get() = when (searchMode)
			{
				MODE.TEXT.name -> SearchMode.Text(text = text)
				MODE.ADVANCED.name -> SearchMode.Advanced(genresInclude = genresInclude, genresExclude = genresExclude)
				else -> throw InvalidObjectException("Invalid SearchMode subclass: $searchMode")
			}
	}
	
	val extras by lazy { IntentData(intent) }
	
	sealed class SearchMode
	{
		data class Text(val text: String) : SearchMode()
		data class Advanced(val genresInclude: ArrayList<String>, val genresExclude: ArrayList<String>) : SearchMode()
	}
	
	private val viewModel by viewModels<DatabaseSearchResultsModel>()
	private val viewHolder by lazy { ActivityDatabaseSearchResultsBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { ChaptersArrayAdapter(this@DatabaseSearchResultsActivity, viewModel.searchResults, viewModel.database.baseUrl) }
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
		viewModel.initialization(database = scrubber.getCompatibleDatabase(extras.databaseUrlBase)!!, input = extras.input)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.booksFetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				return@fetchTrigger pos >= viewModel.searchResults.size - 3
			}
		}
		
		viewModel.booksFetchIterator.onError.observe(this) { }
		viewModel.booksFetchIterator.onCompleted.observe(this) { }
		viewModel.booksFetchIterator.onCompletedEmpty.observe(this) {
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.booksFetchIterator.onFetching.observe(this) {
			viewAdapter.progressBar.visible = it
		}
		viewModel.booksFetchIterator.onSuccess.observe(this) {
			viewModel.searchResults.addAll(it.data)
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
	
}

private class ChaptersArrayAdapter(
	private val context: BaseActivity,
	private val list: ArrayList<BookMetadata>,
	private val databaseUrlBase: String
) : RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(BookListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this@ChaptersArrayAdapter.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val itemModel = this.list[position]
		val itemHolder = binder.viewHolder
		itemHolder.title.text = itemModel.title
		itemHolder.title.setOnClickListener {
			DatabaseBookInfoActivity
				.IntentData(context, databaseUrlBase = databaseUrlBase, bookMetadata = itemModel)
				.let(context::startActivity)
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
