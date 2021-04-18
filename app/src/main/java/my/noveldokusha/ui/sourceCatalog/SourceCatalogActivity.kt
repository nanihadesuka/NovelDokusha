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
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivitySourceCatalogBinding
import my.noveldokusha.databinding.ActivitySourceCatalogListviewItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
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
		val listView by lazy { BooksItemAdapter(viewModel.catalogList) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(scrubber.getCompatibleSourceCatalog(extras.sourceBaseUrl())!!)
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.listView.layoutManager = LinearLayoutManager(this)
		viewHolder.listView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener {
			when (viewModel.mode)
			{
				SourceCatalogModel.Mode.MAIN -> viewModel.loadCatalog()
				SourceCatalogModel.Mode.BAR_SEARCH -> Unit
			}
		}
		
		viewModel.catalogListUpdates.observe(this) {
			viewAdapter.listView.notifyDataSetChanged()
			viewHolder.textNotice.visibility = if (viewModel.catalogList.isEmpty()) View.VISIBLE else View.GONE
		}
		viewModel.refreshing.observe(this) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		viewModel.loading.observe(this) { visible -> viewHolder.progressBar.visibility = if (visible) View.VISIBLE else View.GONE }
		
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
				runOnUiThread {
					viewHolder.textNotice.visibility = View.GONE
					viewHolder.progressBar.visibility = View.GONE
				}
				viewModel.exitSearchCatalogMode()
				return true
			}
		})
		
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				viewModel.exitLoadCatalogMode()
				runOnUiThread {
					viewHolder.progressBar.visibility = View.VISIBLE
					viewHolder.textNotice.visibility = View.GONE
				}
				viewModel.searchCatalog(query ?: "")
				return true
			}
			
			override fun onQueryTextChange(newText: String?): Boolean = true
		})
		
		return true
	}
	
	inner class BooksItemAdapter(private val list: ArrayList<bookstore.BookMetadata>) : RecyclerView.Adapter<BooksItemAdapter.ViewBinder>()
	{
		var defaultTextColor = 0
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			val binder = ViewBinder(ActivitySourceCatalogListviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
				viewModel.toggleBookmark(itemData)
				true
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivitySourceCatalogListviewItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			var exist: LiveData<Boolean> = liveData { }
		}
	}
}