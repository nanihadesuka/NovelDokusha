package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivitySourceCatalogBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.sourceCatalog.SourceCatalogModel.CatalogItem
import my.noveldokusha.ui.webView.WebViewActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiAdapters.ProgressBarAdapter
import my.noveldokusha.uiUtils.*
import java.util.*
import kotlin.properties.Delegates

class SourceCatalogActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var sourceBaseUrl by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, sourceBaseUrl: String) : super(ctx, SourceCatalogActivity::class.java)
		{
			this.sourceBaseUrl = sourceBaseUrl
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<SourceCatalogModel>()
	private val viewBind by lazy { ActivitySourceCatalogBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { BooksItemAdapter(this@SourceCatalogActivity) }
		val progressBar by lazy { ProgressBarAdapter() }
	}
	
	private val viewLayoutManager = object
	{
		val recyclerView by lazy { LinearLayoutManager(this@SourceCatalogActivity) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		viewModel.initialization(scrubber.getCompatibleSourceCatalog(extras.sourceBaseUrl)!!)
		
		viewBind.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewBind.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewBind.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewBind.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.fetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				val listSize = viewBind.recyclerView.adapter?.itemCount ?: return@fetchTrigger false
				pos >= listSize - 3
			}
		}
		
		viewModel.fetchIterator.onSuccess.observe(this) {
			viewAdapter.recyclerView.list = it
		}
		viewModel.fetchIterator.onError.observe(this) {
			viewBind.errorMessage.visibility = View.VISIBLE
			viewBind.errorMessage.text = it.message
		}
		viewModel.fetchIterator.onCompletedEmpty.observe(this) {
			viewBind.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.fetchIterator.onFetching.observe(this) {
			if (it == true) viewAdapter.progressBar.visible = it
		}
		viewModel.fetchIterator.onReset.observe(this) {
			viewBind.errorMessage.visibility = View.GONE
			viewBind.noResultsMessage.visibility = View.GONE
			viewAdapter.recyclerView.notifyDataSetChanged()
		}
		
		// Done so it updates at the same frame as the new items are added (avoids modifying current list offset)
		viewAdapter.recyclerView.listDiffer.addListListener { _, _ ->
			viewAdapter.progressBar.visible = false
		}
		
		supportActionBar!!.let {
			it.title = "Source"
			it.subtitle = viewModel.source.name.capitalize(Locale.ROOT)
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.source_catalog_menu__appbar, menu)
		
		val searchViewItem = menu!!.findItem(R.id.action_search)
		val searchView = searchViewItem.actionView as SearchView
		val webViewItem = menu.findItem(R.id.webview)
		
		searchViewItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener
		{
			override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true
			
			override fun onMenuItemActionCollapse(item: MenuItem?): Boolean
			{
				viewModel.startCatalogListMode()
				return true
			}
		})
		
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				query?.let { viewModel.startCatalogSearchMode(it) }
				return true
			}
			
			override fun onQueryTextChange(newText: String?): Boolean = true
		})
		
		webViewItem.setOnMenuItemClickListener {
			WebViewActivity.IntentData(this, extras.sourceBaseUrl).let(::startActivity)
			true
		}
		
		
		return true
	}
	
}

private class BooksItemAdapter(val ctx: BaseActivity) : MyListAdapter<CatalogItem, BooksItemAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: CatalogItem, new: CatalogItem) = old.bookMetadata.url == new.bookMetadata.url
	override fun areContentsTheSame(old: CatalogItem, new: CatalogItem) = old.bookMetadata == new.bookMetadata
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(BookListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val itemData = this.list[position]
		val itemBind = viewHolder.viewBind
		viewHolder.itemData = itemData
		
		itemBind.title.text = itemData.bookMetadata.title
		itemBind.title.setOnClickListener {
			ChaptersActivity.IntentData(
				ctx,
				bookMetadata = itemData.bookMetadata
			).let(ctx::startActivity)
		}
		itemBind.title.setOnLongClickListener {
			ctx.lifecycleScope.launch(Dispatchers.IO) {
				bookstore.bookLibrary.toggleBookmark(itemData.bookMetadata)
				val isInLibrary = bookstore.bookLibrary.existInLibrary(itemData.bookMetadata.url)
				val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
				toast(res.stringRes())
			}
			true
		}
	}
	
	private val selectedTextColor by lazy { R.attr.colorAccent.colorAttrRes(ctx) }
	
	inner class ViewHolder(val viewBind: BookListItemBinding) : RecyclerView.ViewHolder(viewBind.root)
	{
		var itemData: CatalogItem? by Delegates.observable(null) { _, oldValue, newValue ->
			isInLibraryObserver.switchLiveData(oldValue, newValue, ctx) { isInLibraryLiveData }
		}
		
		private val unselectedTextColor = viewBind.title.currentTextColor
		private val isInLibraryObserver = Observer<Boolean> { isInLibrary ->
			viewBind.title.setTextColor(if (isInLibrary) selectedTextColor else unselectedTextColor)
		}
	}
}
