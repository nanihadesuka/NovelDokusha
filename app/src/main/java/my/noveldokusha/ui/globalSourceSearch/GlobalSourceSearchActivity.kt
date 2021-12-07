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
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.databinding.ActivityGlobalSourceSearchBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchListItemBinding
import my.noveldokusha.databinding.ActivityGlobalSourceSearchResultItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiAdapters.ProgressBarHorizontalAdapter
import my.noveldokusha.uiUtils.*
import kotlin.properties.Delegates

@AndroidEntryPoint
class GlobalSourceSearchActivity : BaseActivity()
{
	class IntentData : Intent, GlobalSourceSearchStateBundle
	{
		override var input by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, input: String) : super(ctx, GlobalSourceSearchActivity::class.java)
		{
			this.input = input
		}
	}
	
	private val viewModel by viewModels<GlobalSourceSearchViewModel>()
	private val viewBind by lazy { ActivityGlobalSourceSearchBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val recyclerView by lazy { GlobalArrayAdapter(this@GlobalSourceSearchActivity) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		
		viewBind.recyclerView.adapter = viewAdapter.recyclerView
		viewBind.recyclerView.itemAnimator = DefaultItemAnimator()
		viewAdapter.recyclerView.list = viewModel.globalResults
		
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

private class GlobalArrayAdapter(private val context: BaseActivity) : MyListAdapter<SourceResults, GlobalArrayAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: SourceResults, new: SourceResults) = old.source == new.source
	override fun areContentsTheSame(old: SourceResults, new: SourceResults) = false
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(context, ActivityGlobalSourceSearchListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val viewData = this.list[position]
		val viewBind = viewHolder.viewBind
		
		viewBind.name.text = viewData.source.name
		viewBind.recyclerView.visibility = View.VISIBLE
		viewBind.noResultsMessage.visibility = View.GONE
		viewBind.recyclerView.layoutManager?.onRestoreInstanceState(viewData.savedState)
		viewBind.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
			viewData.booksFetchIterator.fetchTrigger {
				val pos = viewHolder.layoutManager.findLastVisibleItemPosition()
				return@fetchTrigger pos >= viewHolder.recyclerViewAdapter.itemCount - 3
			}
		}
		
		viewHolder.itemData = viewData
	}
	
	class ViewHolder(
		val ctx: BaseActivity,
		val viewBind: ActivityGlobalSourceSearchListItemBinding
	) : RecyclerView.ViewHolder(viewBind.root)
	{
		var itemData: SourceResults? by Delegates.observable(null) { _, oldValue, newValue ->
			oldValue?.savedState = viewBind.recyclerView.layoutManager?.onSaveInstanceState()
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
			viewBind.recyclerView.visibility = View.GONE
			viewBind.noResultsMessage.visibility = View.VISIBLE
		}
		
		val onSuccess = Observer<List<BookMetadata>> {
			recyclerViewAdapter.list = it
		}
		
		val layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
		
		init
		{
			viewBind.recyclerView.adapter = ConcatAdapter(recyclerViewAdapter, progressBarHorizontalAdapter)
			viewBind.recyclerView.layoutManager = layoutManager
		}
	}
}

private class LocalArrayAdapter(private val context: BaseActivity) : MyListAdapter<BookMetadata, LocalArrayAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: BookMetadata, new: BookMetadata) = old.url == new.url
	override fun areContentsTheSame(old: BookMetadata, new: BookMetadata) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(ActivityGlobalSourceSearchResultItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val viewData = this.list[position]
		val viewBind = viewHolder.viewBind
		viewBind.name.text = viewData.title
		viewBind.name.setOnClickListener {
			ChaptersActivity.IntentData(
				context,
				bookMetadata = BookMetadata(title = viewData.title, url = viewData.url)
			).let(context::startActivity)
		}
	}
	
	inner class ViewHolder(val viewBind: ActivityGlobalSourceSearchResultItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}
