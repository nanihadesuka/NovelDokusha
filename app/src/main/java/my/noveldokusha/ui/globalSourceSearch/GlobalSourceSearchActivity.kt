package my.noveldokusha.ui.globalSourceSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityGlobalSourceSearchBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchListItemBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchResultItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity

class GlobalSourceSearchActivity : BaseActivity()
{
	class Extras(val input: String)
	{
		fun intent(ctx: Context) = Intent(ctx, GlobalSourceSearchActivity::class.java).also {
			it.putExtra(::input.name, input)
		}
	}
	
	private val extras = object
	{
		fun input(): String = intent.extras!!.getString(Extras::input.name)!!
	}
	
	private val viewModel by viewModels<GlobalSourceSearchModel>()
	private val viewHolder by lazy { ActivityGlobalSourceSearchBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { GlobalArrayAdapter(viewModel.globalResults) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(extras.input())
		
		viewHolder.recyclerView.adapter = viewAdapter.recyclerView
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		viewAdapter.recyclerView.notifyDataSetChanged()
		viewModel.globalResultsUpdated.observe(this) {
			viewAdapter.recyclerView.notifyDataSetChanged()
		}
		
		supportActionBar!!.let {
			it.title = "Global source search"
			it.subtitle = viewModel.input
			it.setDisplayHomeAsUpEnabled(true)
		}
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
	
	private inner class GlobalArrayAdapter(private val list: ArrayList<GlobalSourceSearchModel.SourceResults>) : RecyclerView.Adapter<GlobalArrayAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			ViewBinder(ActivityGlobalSourceSearchListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = this.list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemHolder = binder.viewHolder
			itemHolder.name.text = itemData.source.name
			val adapter = LocalArrayAdapter(itemData.results)
			itemHolder.localRecyclerView.adapter = adapter
			adapter.notifyDataSetChanged()
			itemData.resultsUpdated.removeObservers(this@GlobalSourceSearchActivity)
			itemData.resultsUpdated.observe(this@GlobalSourceSearchActivity) {
				itemHolder.localRecyclerView.visibility = if (itemData.results.isEmpty()) View.GONE else View.VISIBLE
				itemHolder.noResultsMessage.visibility = if (itemData.results.isEmpty()) View.VISIBLE else View.GONE
				itemHolder.progressBar.visibility = View.GONE
				adapter.notifyDataSetChanged()
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
	
	private inner class LocalArrayAdapter(private val list: MutableList<bookstore.BookMetadata>) : RecyclerView.Adapter<LocalArrayAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			ViewBinder(ActivityGlobalSourceSearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = this.list.size
		
		override fun onBindViewHolder(holder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemHolder = holder.viewHolder
			itemHolder.name.text = itemData.title
			itemHolder.name.setOnClickListener() {
				val intent = ChaptersActivity
					.Extras(bookUrl = itemData.url, bookTitle = itemData.title)
					.intent(this@GlobalSourceSearchActivity)
				startActivity(intent)
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchResultItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}