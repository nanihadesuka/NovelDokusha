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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.BookMetadata
import my.noveldokusha.BooksFetchIterator
import my.noveldokusha.databinding.ActivityGlobalSourceSearchBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchListItemBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchResultItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiUtils.*
import kotlin.properties.Delegates

class GlobalSourceSearchActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var input by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, input: String) : super(ctx, GlobalSourceSearchActivity::class.java)
		{
			this.input = input
		}
	}
	
	private val extras by lazy { IntentData(intent) }
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
		viewModel.initialization(extras.input)
		
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
		ViewBinder(ActivityGlobalSourceSearchListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		val viewAdapter = object
		{
			val recyclerView = LocalArrayAdapter(context, viewModel.list, viewModel.booksFetchIterator)
			val progressBar = binder.progressBarHorizontalAdapter
		}
		binder.itemData = viewModel
		
		viewModel.let {
			val (pos, offset) = Pair(it.position, it.positionOffset)
			if (pos != null && offset != null)
				binder.layoutManager.scrollToPositionWithOffset(pos, offset)
		}
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
		viewHolder.recyclerView.adapter?.notifyDataSetChanged()
		
		viewHolder.name.text = viewModel.source.name
		viewHolder.recyclerView.visibility = View.VISIBLE
		viewHolder.noResultsMessage.visibility = View.GONE
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			
			binder.layoutManager.findFirstVisibleItemPosition().let {
				viewModel.position = it
				viewModel.positionOffset = viewHolder.recyclerView.run { getChildAt(0).left - paddingLeft }
			}
			
			viewModel.booksFetchIterator.fetchTrigger {
				val pos = binder.layoutManager.findLastVisibleItemPosition()
				return@fetchTrigger pos >= viewModel.list.size - 3
			}
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	{
		var itemData: GlobalSourceSearchModel.SourceResults? by Delegates.observable(null) { _, oldValue, newValue ->
			onFetching.switchLiveData(oldValue, newValue, context) { booksFetchIterator.onFetching }
			onCompletedEmpty.switchLiveData(oldValue, newValue, context) { booksFetchIterator.onCompletedEmpty }
		}
		val onFetching = Observer<Boolean> {
			progressBarHorizontalAdapter.visible = it
		}
		val onCompletedEmpty = Observer<Unit> {
			viewHolder.recyclerView.visibility = View.GONE
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		
		val progressBarHorizontalAdapter = ProgressBarHorizontalAdapter()
		val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
		
		init
		{
			viewHolder.recyclerView.layoutManager = layoutManager
		}
	}
}

private class LocalArrayAdapter(
	private val context: BaseActivity,
	private val list: MutableList<BookMetadata>,
	private val booksFetchIterator: BooksFetchIterator
) : RecyclerView.Adapter<LocalArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(ActivityGlobalSourceSearchResultItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		viewHolder.name.text = viewModel.title
		viewHolder.name.setOnClickListener {
			ChaptersActivity.IntentData(
				context,
				bookMetadata = BookMetadata(title = viewModel.title, url = viewModel.url)
			).let(context::startActivity)
		}
		
		binder.addRightMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchResultItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
