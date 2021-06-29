package my.noveldokusha.ui.globalSourceSearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import my.noveldokusha.BookMetadata
import my.noveldokusha.databinding.ActivityGlobalSourceSearchBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchListItemBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchResultItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiAdapters.ProgressBarHorizontalAdapter
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
		
		supportActionBar!!.let {
			it.title = "Global source search"
			it.subtitle = extras.input
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
	private val list: ArrayList<SourceResults>
) : RecyclerView.Adapter<GlobalArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(context, ActivityGlobalSourceSearchListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		binder.itemData = viewModel
		
		viewHolder.name.text = viewModel.source.name
		viewHolder.recyclerView.visibility = View.VISIBLE
		viewHolder.noResultsMessage.visibility = View.GONE
		viewHolder.recyclerView.layoutManager?.onRestoreInstanceState(viewModel.savedState)
		
		viewHolder.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewModel.booksFetchIterator.fetchTrigger {
				val pos = binder.layoutManager.findLastVisibleItemPosition()
				return@fetchTrigger pos >= binder.recyclerViewAdapter.itemCount - 3
			}
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	class ViewBinder(
		val ctx: BaseActivity,
		val viewHolder: ActivityGlobalSourceSearchListItemBinding
	) : RecyclerView.ViewHolder(viewHolder.root)
	{
		var itemData: SourceResults? by Delegates.observable(null) { _, oldValue, newValue ->
			oldValue?.savedState = viewHolder.recyclerView.layoutManager?.onSaveInstanceState()
			onFetching.switchLiveData(oldValue, newValue, ctx) { booksFetchIterator.onFetching }
			onCompletedEmpty.switchLiveData(oldValue, newValue, ctx) { booksFetchIterator.onCompletedEmpty }
			onSuccess.switchLiveData(oldValue, newValue, ctx) { booksFetchIterator.onSuccess }
		}
		
		val recyclerViewAdapter = LocalArrayAdapter(ctx)
		val progressBarHorizontalAdapter = ProgressBarHorizontalAdapter()
		
		val onFetching = Observer<Boolean> {
			progressBarHorizontalAdapter.visible = it
		}
		val onCompletedEmpty = Observer<Unit> {
			viewHolder.recyclerView.visibility = View.GONE
			viewHolder.noResultsMessage.visibility = View.VISIBLE
		}
		
		val onSuccess = Observer<List<BookMetadata>> {
			recyclerViewAdapter.list = it
		}
		
		val layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
		
		init
		{
			viewHolder.recyclerView.adapter = ConcatAdapter(recyclerViewAdapter, progressBarHorizontalAdapter)
			viewHolder.recyclerView.layoutManager = layoutManager
		}
	}
}

private class LocalArrayAdapter(private val context: BaseActivity) : MyListAdapter<BookMetadata, LocalArrayAdapter.ViewBinder>()
{
	override fun areItemsTheSame(old: BookMetadata, new: BookMetadata) = old.url == new.url
	override fun areContentsTheSame(old: BookMetadata, new: BookMetadata) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(ActivityGlobalSourceSearchResultItemBinding.inflate(parent.inflater, parent, false))
	
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
		
		binder.addRightMargin(500) { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: ActivityGlobalSourceSearchResultItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
