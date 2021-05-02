package my.noveldokusha.ui.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
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
			viewModel.list.addAll(it.data)
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
	
	inner class BooksItemAdapter(private val list: List<bookstore.BookMetadata>) : RecyclerView.Adapter<BooksItemAdapter.ViewBinder>()
	{
		var defaultTextColor = 0
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			val binder = ViewBinder(BookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
			defaultTextColor = binder.viewHolder.title.currentTextColor
			return binder
		}
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			
			itemView.title.text = itemData.title
			
			binder.exist.removeObservers(this@SourceCatalogActivity)
			binder.exist = bookstore.bookLibrary.existFlow(itemData.url).asLiveData()
			binder.exist.observe(this@SourceCatalogActivity) { inLibrary ->
				if (inLibrary)
					itemView.title.setTextColor(ContextCompat.getColor(this@SourceCatalogActivity, R.color.dark_green))
				else itemView.title.setTextColor(defaultTextColor)
			}
			
			itemView.title.setOnClickListener() {
				val intent = ChaptersActivity.Extras(bookUrl = itemData.url, bookTitle = itemData.title).intent(this@SourceCatalogActivity)
				startActivity(intent)
			}
			
			itemView.title.setOnLongClickListener {
				lifecycleScope.launch(Dispatchers.IO) { bookstore.bookLibrary.toggleBookmark(itemData) }
				true
			}
			binder.addBottomMargin { position == list.lastIndex  }
		}
		
		inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			var exist: LiveData<Boolean> = liveData { }
		}
	}
}