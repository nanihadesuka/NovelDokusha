package my.noveldokusha.ui.globalSourceSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.BooksFetchIterator
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityGlobalSourceSearchBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchListItemBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchResultItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiUtils.ProgressBarAdapter
import my.noveldokusha.uiUtils.addBottomMargin
import my.noveldokusha.uiUtils.addRightMargin

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
		val recyclerView by lazy { GlobalArrayAdapter(this@GlobalSourceSearchActivity, viewModel.globalResults) }
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
		
		viewModel.globalResults.forEach {
			it.booksFetchIterator.onSuccess.observe(this) { res ->
				it.list.addAll(res.data)
				viewAdapter.recyclerView.notifyDataSetChanged()
			}
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
}

private class GlobalArrayAdapter(
	private val context: BaseActivity,
	private val list: ArrayList<GlobalSourceSearchModel.SourceResults>
) : RecyclerView.Adapter<GlobalArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(ActivityGlobalSourceSearchListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	
	override fun getItemCount() = this.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		binder.lastItemData?.also {
			it.booksFetchIterator.onFetching.removeObserver(binder.onFetching)
			it.booksFetchIterator.onCompletedEmpty.removeObserver(binder.onCompletedEmpty)
		}
		
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		val viewAdapter = object
		{
			val recyclerView = LocalArrayAdapter(context, viewModel.list, viewModel.booksFetchIterator)
			val progressBar = binder.progressBarAdapter
		}
		
		binder.lastItemData = viewModel
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.adapter?.notifyDataSetChanged()
		
		viewHolder.name.text = viewModel.source.name
		viewHolder.recyclerView.visibility = View.VISIBLE
		viewHolder.noResultsMessage.visibility = View.GONE
		
		viewModel.booksFetchIterator.onFetching.observe(context, binder.onFetching)
		viewModel.booksFetchIterator.onCompletedEmpty.observe(context, binder.onCompletedEmpty)
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	{
		var lastItemData: GlobalSourceSearchModel.SourceResults? = null
		val onFetching = Observer<Boolean> {
			progressBarAdapter.visible = it
		}
		val onCompletedEmpty = Observer<Unit> {
			viewHolder.recyclerView.visibility = View.GONE
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		
		val progressBarAdapter = ProgressBarAdapter()
	}
}

private class LocalArrayAdapter(
	private val context: BaseActivity,
	private val list: MutableList<bookstore.BookMetadata>,
	private val booksFetchIterator: BooksFetchIterator
) : RecyclerView.Adapter<LocalArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(ActivityGlobalSourceSearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	
	override fun getItemCount() = this.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		viewHolder.name.text = viewModel.title
		viewHolder.name.setOnClickListener {
			val intent = ChaptersActivity
				.Extras(bookUrl = viewModel.url, bookTitle = viewModel.title)
				.intent(context)
			context.startActivity(intent)
		}
		
		binder.addRightMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchResultItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
