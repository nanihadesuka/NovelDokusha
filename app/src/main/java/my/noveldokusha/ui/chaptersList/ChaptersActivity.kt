package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import my.noveldokusha.BookMetadata
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityChaptersBinding
import my.noveldokusha.databinding.ActivityChaptersListHeaderBinding
import my.noveldokusha.databinding.ActivityChaptersListItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.uiUtils.*
import java.util.*

class ChaptersActivity : BaseActivity()
{
	class IntentData : Intent
	{
		val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
		private var bookUrl by Extra_String()
		private var bookTitle by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, bookMetadata: BookMetadata) : super(ctx, ChaptersActivity::class.java)
		{
			this.bookUrl = bookMetadata.url
			this.bookTitle = bookMetadata.title
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<ChaptersModel>()
	private val viewHolder by lazy { ActivityChaptersBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val chapters by lazy { ChaptersArrayAdapter(this@ChaptersActivity, viewModel.chapters, viewModel) { selectionModeBarUpdateVisibility() } }
		val header by lazy { ChaptersHeaderAdapter(this@ChaptersActivity, viewModel) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(extras.bookMetadata)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.header, viewAdapter.chapters)
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.loadChapters(false) }
		
		viewModel.refresh.observe(this) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		
		viewModel.chaptersWithContextLiveData.observe(this) {
			viewAdapter.chapters.setList(it)
		}
		
		setupSelectionModeBar()
		
		supportActionBar!!.let {
			it.title = "Chapters"
			it.setDisplayHomeAsUpEnabled(true)
		}
	}
	
	fun selectionModeBarUpdateVisibility()
	{
		if (viewModel.selectedChaptersUrl.isEmpty() && viewHolder.selectionModeBar.isVisible)
			viewHolder.selectionModeBar.fadeOutVertical(displacement = 200f)
		else if (viewModel.selectedChaptersUrl.isNotEmpty() && !viewHolder.selectionModeBar.isVisible)
			viewHolder.selectionModeBar.fadeInVertical(displacement = 200f)
	}
	
	fun setupSelectionModeBar()
	{
		selectionModeBarUpdateVisibility()
		
		viewHolder.selectionSelectAll.setOnClickListener {
			viewModel.selectedChaptersUrl.addAll(viewModel.chapters.map { it.chapter.url })
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewHolder.selectionSelectAllUnderSelected.setOnClickListener {
			val index = viewModel.chapters.indexOfFirst { viewModel.selectedChaptersUrl.contains(it.chapter.url) }
			if (index == -1) return@setOnClickListener
			
			viewModel.selectedChaptersUrl.addAll(viewModel.chapters.drop(index).map { it.chapter.url })
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewHolder.selectionModeSetAsUnread.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookChapter.setAsUnread(list) }
		}
		
		viewHolder.selectionModeSetAsRead.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookChapter.setAsRead(list) }
		}
		
		viewHolder.selectionModeDownload.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			GlobalScope.launch(Dispatchers.IO) {
				list.forEach { bookstore.bookChapterBody.fetchBody(it) }
			}
		}
		
		viewHolder.selectionModeDeleteDownload.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) {
				bookstore.bookChapterBody.removeRows(list)
			}
		}
		
		viewHolder.selectionClose.setOnClickListener {
			viewModel.selectedChaptersUrl.clear()
			selectionModeBarUpdateVisibility()
			viewAdapter.chapters.notifyDataSetChanged()
		}
	}
	
	override fun onBackPressed() = when
	{
		viewHolder.selectionModeBar.isVisible ->
		{
			viewModel.selectedChaptersUrl.clear()
			selectionModeBarUpdateVisibility()
			viewAdapter.chapters.notifyDataSetChanged()
		}
		else -> super.onBackPressed()
	}
	
	override fun onCreateOptionsMenu(menu: Menu?): Boolean
	{
		menuInflater.inflate(R.menu.chapters_list_menu__appbar, menu)
		
		val itemBookmark = menu!!.findItem(R.id.action_bookmarked)!!
		
		setBookmarkIconActive(runBlocking { bookstore.bookLibrary.exist(viewModel.bookMetadata.url) }, itemBookmark)
		
		bookstore.bookLibrary.existFlow(viewModel.bookMetadata.url).asLiveData().observe(this) { bookmarked ->
			
			setBookmarkIconActive(bookmarked, itemBookmark)
			this.bookmarked = bookmarked
		}
		return true
	}
	
	var bookmarked: Boolean = false
	
	private fun setBookmarkIconActive(active: Boolean, item: MenuItem)
	{
		val color = if (active) ContextCompat.getColor(this, R.color.dark_orange_red) else Color.DKGRAY
		item.icon.setTint(color)
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		R.id.action_bookmarked ->
		{
			toast(if (!bookmarked) "Bookmark added" else "Bookmark removed")
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookLibrary.toggleBookmark(viewModel.bookMetadata) }
			true
		}
		android.R.id.home -> this.onBackPressed().let { true }
		else -> super.onOptionsItemSelected(item)
	}
}

private class ChaptersArrayAdapter(
	private val context: BaseActivity,
	private val list: ArrayList<bookstore.ChapterDao.ChapterWithContext>,
	private val viewModel: ChaptersModel,
	private val selectionModeBarUpdateVisibility: () -> Unit
) :
	RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
{
	private inner class Diff(private val aNew: List<bookstore.ChapterDao.ChapterWithContext>) : DiffUtil.Callback()
	{
		override fun getOldListSize(): Int = list.size
		override fun getNewListSize(): Int = aNew.size
		override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].chapter.url == aNew[newPos].chapter.url
		override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
			(list[oldPos].chapter.read == aNew[newPos].chapter.read) &&
			(list[oldPos].downloaded == aNew[newPos].downloaded) &&
			(list[oldPos].lastReadChapter == aNew[newPos].lastReadChapter)
	}
	
	fun setList(newList: List<bookstore.ChapterDao.ChapterWithContext>) = DiffUtil.calculateDiff(Diff(newList)).let {
		val isEmpty = list.isEmpty()
		list.clear()
		list.addAll(newList)
		if (isEmpty) notifyDataSetChanged() else it.dispatchUpdatesTo(this)
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
		ViewBinder(ActivityChaptersListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this@ChaptersArrayAdapter.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val itemData = this.list[position]
		val itemView = binder.viewHolder
		
		itemView.title.text = itemData.chapter.title
		itemView.title.alpha = if (itemData.chapter.read) 0.5f else 1.0f
		itemView.downloaded.visibility = if (itemData.downloaded) View.VISIBLE else View.INVISIBLE
		itemView.currentlyReading.visibility = if (itemData.lastReadChapter) View.VISIBLE else View.INVISIBLE
		itemView.selected.visibility = if (viewModel.selectedChaptersUrl.contains(itemData.chapter.url)) View.VISIBLE else View.INVISIBLE
		
		itemView.root.setOnClickListener {
			if (viewModel.selectedChaptersUrl.isNotEmpty())
				toggleItemSelection(itemData, itemView.selected)
			else
			{
				ReaderActivity
					.IntentData(context, bookUrl = viewModel.bookMetadata.url, bookSelectedChapterUrl = itemData.chapter.url)
					.let(context::startActivity)
			}
		}
		
		itemView.root.setOnLongClickListener {
			toggleItemSelection(itemData, itemView.selected)
			true
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	fun toggleItemSelection(itemData: bookstore.ChapterDao.ChapterWithContext, view: View)
	{
		fun <T> MutableSet<T>.removeOrAdd(value: T) = contains(value).also { if (it) remove(value) else add(value) }
		val isRemoved = viewModel.selectedChaptersUrl.removeOrAdd(itemData.chapter.url)
		view.visibility = if (isRemoved) View.INVISIBLE else View.VISIBLE
		selectionModeBarUpdateVisibility()
	}
	
	inner class ViewBinder(val viewHolder: ActivityChaptersListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}

private class ChaptersHeaderAdapter(
	val context: BaseActivity,
	val viewModel: ChaptersModel
) : RecyclerView.Adapter<ChaptersHeaderAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
	{
		return ViewBinder(ActivityChaptersListHeaderBinding.inflate(parent.inflater, parent, false))
	}
	
	override fun getItemCount() = 1
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
	}
	
	private inner class ViewBinder(val viewHolder: ActivityChaptersListHeaderBinding) : RecyclerView.ViewHolder(viewHolder.root)
	{
		init
		{
			viewHolder.bookTitle.text = viewModel.bookMetadata.title
			viewHolder.sourceName.text = viewModel.sourceName
			
			viewModel.chaptersWithContextLiveData.observe(context) { list ->
				viewHolder.numberOfChapters.text = list.size.toString()
			}
			viewModel.errorMessage.observe(context) { viewHolder.errorMessage.text = it }
			viewModel.errorMessageVisibility.observe(context) { viewHolder.errorMessage.visibility = it }
			viewModel.errorMessageMaxLines.observe(context) { viewHolder.errorMessage.maxLines = it }
			viewHolder.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
			{
				private var expand: Boolean = false
				override fun onLongClick(v: View?): Boolean
				{
					expand = !expand
					when (expand)
					{
						true -> viewModel.errorMessageMaxLines.postValue(100)
						false -> viewModel.errorMessageMaxLines.postValue(10)
					}
					return true
				}
			})
			viewHolder.databaseSearchButton.setOnClickListener {
				DatabaseSearchResultsActivity
					.IntentData(context, "https://www.novelupdates.com/", DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookMetadata.title))
					.let(context::startActivity)
			}
			
			viewHolder.webpageOpenButton.setOnClickListener {
				Intent(Intent.ACTION_VIEW).also {
					it.data = Uri.parse(viewModel.bookMetadata.url)
				}.let(context::startActivity)
			}
		}
	}
}

