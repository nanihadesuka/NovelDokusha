package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
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
	private val viewHolder by lazy { ActivitySourceCatalogBinding.inflate(layoutInflater) }
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
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(scrubber.getCompatibleSourceCatalog(extras.sourceBaseUrl)!!)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.fetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				val listSize = viewHolder.recyclerView.adapter?.itemCount ?: return@fetchTrigger false
				pos >= listSize - 3
			}
		}
		
		viewModel.fetchIterator.onSuccess.observe(this) {
			viewAdapter.recyclerView.setList(it)
		}
		viewModel.fetchIterator.onError.observe(this) {
			viewHolder.errorMessage.visibility = View.VISIBLE
			viewHolder.errorMessage.text = it.message
		}
		viewModel.fetchIterator.onCompletedEmpty.observe(this) {
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.fetchIterator.onFetching.observe(this) {
			viewAdapter.progressBar.visible = it
		}
		viewModel.fetchIterator.onReset.observe(this) {
			viewHolder.errorMessage.visibility = View.GONE
			viewHolder.noResultsMessage.visibility = View.GONE
			viewAdapter.recyclerView.notifyDataSetChanged()
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
	
	class BooksItemAdapter(val ctx: BaseActivity) : RecyclerView.Adapter<BooksItemAdapter.ViewBinder>()
	{
		private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<CatalogItem>()
		{
			override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem) = oldItem.bookMetadata.url == newItem.bookMetadata.url
			override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem) = oldItem.bookMetadata == newItem.bookMetadata
		})
		
		private val list get() = differ.currentList
		
		fun setList(newList: List<CatalogItem>) = differ.submitList(newList)
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
			ViewBinder(BookListItemBinding.inflate(parent.inflater, parent, false))
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			binder.itemData = itemData
			
			itemView.title.text = itemData.bookMetadata.title
			itemView.title.setOnClickListener {
				ChaptersActivity.IntentData(
					ctx,
					bookMetadata = itemData.bookMetadata
				).let(ctx::startActivity)
			}
			itemView.title.setOnLongClickListener {
				ctx.lifecycleScope.launch(Dispatchers.IO) {
					bookstore.bookLibrary.toggleBookmark(itemData.bookMetadata)
					val isInLibrary = bookstore.bookLibrary.existInLibrary(itemData.bookMetadata.url)
					val res = if (isInLibrary) R.string.added_to_library else R.string.removed_from_library
					toast(res.stringRes())
				}
				true
			}
			
			binder.addBottomMargin { position == list.lastIndex }
		}
		
		inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			var itemData: CatalogItem? by Delegates.observable(null) { _, oldValue, newValue ->
				isInLibraryObserver.switchLiveData(oldValue, newValue, ctx) { isInLibraryLiveData }
			}
			
			private val unselectedTextColor = viewHolder.title.currentTextColor
			private val selectedTextColor by lazy { ContextCompat.getColor(ctx, R.color.dark_green) }
			private val isInLibraryObserver = Observer<Boolean> { isInLibrary ->
				viewHolder.title.setTextColor(if (isInLibrary) selectedTextColor else unselectedTextColor)
			}
		}
	}
}