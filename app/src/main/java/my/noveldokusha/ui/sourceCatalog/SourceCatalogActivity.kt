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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivitySourceCatalogBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.sourceCatalog.SourceCatalogModel.CatalogItem
import my.noveldokusha.uiUtils.ProgressBarAdapter
import my.noveldokusha.uiUtils.addBottomMargin
import java.util.*

class SourceCatalogActivity : BaseActivity()
{
	class Extras(val sourceBaseUrl: String)
	{
		fun intent(ctx: Context) = Intent(ctx, SourceCatalogActivity::class.java).also {
			it.putExtra(::sourceBaseUrl.name, sourceBaseUrl)
		}
	}
	
	private val extras = object
	{
		fun sourceBaseUrl() = intent.extras!!.get(Extras::sourceBaseUrl.name)!! as String
	}
	
	private val viewModel by viewModels<SourceCatalogModel>()
	private val viewHolder by lazy { ActivitySourceCatalogBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { BooksItemAdapter(viewModel.list) }
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
		viewModel.initialization(scrubber.getCompatibleSourceCatalog(extras.sourceBaseUrl())!!)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.layoutManager = viewLayoutManager.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.booksFetchIterator.fetchTrigger {
				val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
				pos >= viewModel.list.size - 3
			}
		}
		
		viewModel.booksFetchIterator.onSuccess.observe(this) {
			viewModel.list.addAll(it.data.map(::CatalogItem))
			viewAdapter.recyclerView.notifyDataSetChanged()
		}
		viewModel.booksFetchIterator.onError.observe(this) {
			viewHolder.errorMessage.visibility = View.VISIBLE
			viewHolder.errorMessage.text = it.message
		}
		viewModel.booksFetchIterator.onCompletedEmpty.observe(this) {
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		viewModel.booksFetchIterator.onFetching.observe(this) {
			viewAdapter.progressBar.visible = it
		}
		viewModel.booksFetchIterator.onReset.observe(this) {
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
		
		return true
	}
	
	inner class BooksItemAdapter(private val list: List<CatalogItem>) : RecyclerView.Adapter<BooksItemAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
			ViewBinder(BookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			binder.setObservers(itemData)
			
			itemView.title.text = itemData.bookMetadata.title
			itemView.title.setOnClickListener {
				val intent = ChaptersActivity
					.Extras(bookUrl = itemData.bookMetadata.url, bookTitle = itemData.bookMetadata.title)
					.intent(this@SourceCatalogActivity)
				startActivity(intent)
			}
			itemView.title.setOnLongClickListener {
				lifecycleScope.launch(Dispatchers.IO) { bookstore.bookLibrary.toggleBookmark(itemData.bookMetadata) }
				true
			}
			
			binder.addBottomMargin { position == list.lastIndex }
		}
		
		inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			private var itemDataLast: CatalogItem? = null
			
			private val unselectedTextColor = viewHolder.title.currentTextColor
			private val selectedTextColor by lazy { ContextCompat.getColor(this@SourceCatalogActivity, R.color.dark_green) }
			
			val isInLibraryObserver = Observer<Boolean> { isInLibrary ->
				viewHolder.title.setTextColor(if (isInLibrary) selectedTextColor else unselectedTextColor)
			}
			
			fun setObservers(itemData: CatalogItem)
			{
				itemDataLast?.isInLibraryLiveData?.removeObserver(isInLibraryObserver)
				itemData.isInLibraryLiveData.observe(this@SourceCatalogActivity, isInLibraryObserver)
				itemDataLast = itemData
			}
		}
	}
}