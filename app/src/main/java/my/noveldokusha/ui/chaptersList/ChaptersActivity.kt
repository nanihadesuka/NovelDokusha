package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityChaptersBinding
import my.noveldokusha.databinding.ActivityChaptersListHeaderBinding
import my.noveldokusha.databinding.ActivityChaptersListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsModel
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.uiUtils.addBottomMargin
import java.util.*

class ChaptersActivity : BaseActivity()
{
	class Extras(val bookUrl: String, val bookTitle: String)
	{
		fun intent(ctx: Context) = Intent(ctx, ChaptersActivity::class.java).also {
			it.putExtra(::bookUrl.name, bookUrl)
			it.putExtra(::bookTitle.name, bookTitle)
		}
	}
	
	private val extras = object
	{
		fun bookMetadata() = bookstore.BookMetadata(
			title = intent.extras!!.getString(Extras::bookTitle.name)!!,
			url = intent.extras!!.getString(Extras::bookUrl.name)!!
		)
	}
	
	private val viewModel by viewModels<ChaptersModel>()
	private val viewHolder by lazy { ActivityChaptersBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val chapters by lazy { ChaptersArrayAdapter(viewModel.chapters) }
		val header by lazy { ChaptersHeaderAdapter() }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(extras.bookMetadata())
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.header, viewAdapter.chapters)
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.loadChapters(false) }
		
		viewModel.refresh.observe(this) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		
		viewModel.chaptersItemsLiveData.observe(this) {
			viewAdapter.chapters.setList(it)
			viewModel.numberOfChapters.value = it.size
		}
		
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
			lifecycleScope.launch(Dispatchers.IO) {
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
			viewHolder.selectionModeBar.visibility = View.INVISIBLE
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewHolder.selectionModeBar.visibility = if (viewModel.selectedChaptersUrl.isNotEmpty()) View.VISIBLE else View.INVISIBLE
		
		supportActionBar!!.let {
			it.title = "Chapters"
			it.setDisplayHomeAsUpEnabled(true)
		}
	}
	
	override fun onBackPressed() = when (viewHolder.selectionModeBar.visibility)
	{
		View.VISIBLE ->
		{
			viewModel.selectedChaptersUrl.clear()
			viewHolder.selectionModeBar.visibility = View.INVISIBLE
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
			Toast.makeText(this, if (!bookmarked) "Bookmark added" else "Bookmark removed", Toast.LENGTH_SHORT).show()
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookLibrary.toggleBookmark(viewModel.bookMetadata) }
			true
		}
		android.R.id.home -> this.onBackPressed().let { true }
		else -> super.onOptionsItemSelected(item)
	}
	
	private inner class ChaptersArrayAdapter(private val list: ArrayList<ChaptersModel.ChapterItem>) :
		RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
	{
		private inner class Diff(private val new: List<ChaptersModel.ChapterItem>) : DiffUtil.Callback()
		{
			override fun getOldListSize(): Int = list.size
			override fun getNewListSize(): Int = new.size
			override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].chapter.url == new[newPos].chapter.url
			override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].chapter == new[newPos].chapter
		}
		
		fun setList(newList: List<ChaptersModel.ChapterItem>) = DiffUtil.calculateDiff(Diff(newList)).let {
			val isEmpty = list.isEmpty()
			list.clear()
			list.addAll(newList)
			if (isEmpty) notifyDataSetChanged() else it.dispatchUpdatesTo(this)
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
			ViewBinder(ActivityChaptersListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = this@ChaptersArrayAdapter.list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			binder.lastItemData?.also {
				it.currentlyLastReadChapterLiveData.removeObserver(binder.lastReadObserver)
				it.downloadedLiveData.removeObserver(binder.downloadedObserver)
			}
			
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			binder.lastItemData = itemData
			
			itemView.currentlyReading.visibility = View.INVISIBLE
			itemView.downloaded.visibility = View.INVISIBLE
			
			itemData.currentlyLastReadChapterLiveData.observe(this@ChaptersActivity, binder.lastReadObserver)
			itemData.downloadedLiveData.observe(this@ChaptersActivity, binder.downloadedObserver)
			
			itemView.title.text = itemData.chapter.title
			itemView.title.alpha = if (itemData.chapter.read) 0.5f else 1.0f
			
			itemView.selected.visibility =
				if (viewModel.selectedChaptersUrl.contains(itemData.chapter.url)) View.VISIBLE else View.INVISIBLE
			
			itemView.root.setOnClickListener {
				
				if (viewModel.selectedChaptersUrl.isNotEmpty())
				{
					val isSelected = viewModel.selectedChaptersUrl.contains(itemData.chapter.url)
					if (isSelected) viewModel.selectedChaptersUrl.remove(itemData.chapter.url)
					else viewModel.selectedChaptersUrl.add(itemData.chapter.url)
					
					if (viewModel.selectedChaptersUrl.isEmpty())
						viewHolder.selectionModeBar.visibility = View.INVISIBLE
					
					notifyDataSetChanged()
					return@setOnClickListener
				}
				
				val intent = ReaderActivity
					.Extras(bookUrl = viewModel.bookMetadata.url, bookSelectedChapterUrl = itemData.chapter.url)
					.intent(this@ChaptersActivity)
				this@ChaptersActivity.startActivity(intent)
			}
			
			itemView.root.setOnLongClickListener {
				if (viewModel.selectedChaptersUrl.isEmpty())
				{
					viewModel.selectedChaptersUrl.add(itemData.chapter.url)
					viewHolder.selectionModeBar.visibility = View.VISIBLE
					notifyDataSetChanged()
				}
				else
				{
					val isSelected = viewModel.selectedChaptersUrl.contains(itemData.chapter.url)
					if (isSelected) viewModel.selectedChaptersUrl.remove(itemData.chapter.url)
					else viewModel.selectedChaptersUrl.add(itemData.chapter.url)
					
					if (viewModel.selectedChaptersUrl.isEmpty())
						viewHolder.selectionModeBar.visibility = View.INVISIBLE
					
					notifyDataSetChanged()
				}
				true
			}
			
			binder.addBottomMargin { position == list.lastIndex }
		}
		
		inner class ViewBinder(val viewHolder: ActivityChaptersListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
		{
			var lastItemData: ChaptersModel.ChapterItem? = null
			val lastReadObserver: Observer<Boolean> = Observer { currentlyReading ->
				viewHolder.currentlyReading.visibility = if (currentlyReading) View.VISIBLE else View.INVISIBLE
			}
			val downloadedObserver: Observer<Boolean> = Observer { downloaded ->
				viewHolder.downloaded.visibility = if (downloaded) View.VISIBLE else View.INVISIBLE
			}
		}
	}
	
	inner class ChaptersHeaderAdapter : RecyclerView.Adapter<ChaptersHeaderAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			return ViewBinder(ActivityChaptersListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		}
		
		override fun getItemCount() = 1
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			binder.viewHolder.bookTitle.text = viewModel.bookTitle
			binder.viewHolder.sourceName.text = viewModel.sourceName
			
			viewModel.numberOfChapters.observe(this@ChaptersActivity) {
				binder.viewHolder.numberOfChapters.text = it.toString()
				binder.viewHolder.numberOfChapters.visibility = if (it.toString().isEmpty()) View.INVISIBLE else View.VISIBLE
			}
			viewModel.errorMessage.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.text = it }
			viewModel.errorMessageVisibility.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.visibility = it }
			viewModel.errorMessageMaxLines.observe(this@ChaptersActivity) { binder.viewHolder.errorMessage.maxLines = it }
			binder.viewHolder.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
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
			binder.viewHolder.databaseSearchButton.setOnClickListener {
				DatabaseSearchResultsActivity
					.Extras(scrubber.database.NovelUpdates.baseUrl, DatabaseSearchResultsModel.SearchMode.Text(viewModel.bookMetadata.title))
					.intent(this@ChaptersActivity).let(::startActivity)
			}
			
			binder.viewHolder.webpageOpenButton.setOnClickListener {
				Intent(Intent.ACTION_VIEW).also {
					it.data = Uri.parse(this@ChaptersActivity.viewModel.bookMetadata.url)
				}.let(::startActivity)
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityChaptersListHeaderBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}

