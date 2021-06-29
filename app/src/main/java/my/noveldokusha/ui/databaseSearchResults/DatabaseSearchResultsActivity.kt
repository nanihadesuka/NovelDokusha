package my.noveldokusha.ui.databaseSearchResults

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.*
import my.noveldokusha.BookMetadata
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiAdapters.ProgressBarAdapter
import my.noveldokusha.uiUtils.*
import java.io.InvalidObjectException
import java.util.*

class DatabaseSearchResultsActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var databaseUrlBase by Extra_String()
		private var text by Extra_String()
		private var genresIncludeId by Extra_StringArrayList()
		private var genresExcludeId by Extra_StringArrayList()
		private var searchMode by Extra_String()
		private var authorName by Extra_String()
		private var urlAuthorPage by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseUrlBase: String, input: SearchMode) : super(ctx, DatabaseSearchResultsActivity::class.java)
		{
			this.databaseUrlBase = databaseUrlBase
			this.searchMode = input::class.simpleName!!
			when (input)
			{
				is SearchMode.Text ->
				{
					this.text = input.text
				}
				is SearchMode.Genres ->
				{
					this.genresIncludeId = input.genresIncludeId
					this.genresExcludeId = input.genresExcludeId
				}
				is SearchMode.AuthorSeries ->
				{
					this.authorName = input.authorName
					this.urlAuthorPage = input.urlAuthorPage
				}
			}
		}
		
		val input
			get() = when (this.searchMode)
			{
				SearchMode.Text::class.simpleName -> SearchMode.Text(text = text)
				SearchMode.Genres::class.simpleName -> SearchMode.Genres(genresIncludeId = genresIncludeId, genresExcludeId = genresExcludeId)
				SearchMode.AuthorSeries::class.simpleName -> SearchMode.AuthorSeries(authorName = authorName, urlAuthorPage = urlAuthorPage)
				else -> throw InvalidObjectException("Invalid SearchMode subclass: $searchMode")
			}
	}
	
	val extras by lazy { IntentData(intent) }
	
	sealed class SearchMode
	{
		data class Text(val text: String) : SearchMode()
		data class Genres(val genresIncludeId: ArrayList<String>, val genresExcludeId: ArrayList<String>) : SearchMode()
		data class AuthorSeries(val authorName: String, val urlAuthorPage: String) : SearchMode()
	}
	
	private val viewModel by viewModels<DatabaseSearchResultsModel>()
	private val viewHolder by lazy { ActivityDatabaseSearchResultsBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { ChaptersArrayAdapter(this@DatabaseSearchResultsActivity, viewModel.database.baseUrl) }
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
			viewModel.fetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				return@fetchTrigger pos >= viewAdapter.recyclerView.itemCount - 3
			}
		}
		
		viewModel.fetchIterator.onCompletedEmpty.observe(this) {
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.fetchIterator.onFetching.observe(this) {
			viewAdapter.progressBar.visible = it
		}
		viewModel.fetchIterator.onSuccess.observe(this) {
			viewAdapter.recyclerView.list = it
		}
		
		supportActionBar!!.let {
			it.title = "Database: " + viewModel.database.name.capitalize(Locale.ROOT)
			it.subtitle = when (val res = extras.input)
			{
				is SearchMode.AuthorSeries -> "Search by author: ${res.authorName}"
				is SearchMode.Genres -> "Search by genre"
				is SearchMode.Text -> "Search by title: " + res.text
			}
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
	private val databaseUrlBase: String
) : MyListAdapter<BookMetadata, ChaptersArrayAdapter.ViewBinder>()
{
	override fun areItemsTheSame(old: BookMetadata, new: BookMetadata) = old.url == new.url
	override fun areContentsTheSame(old: BookMetadata, new: BookMetadata) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(BookListItemBinding.inflate(parent.inflater, parent, false))
	
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
