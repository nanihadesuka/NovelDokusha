package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityChaptersBinding
import my.noveldokusha.databinding.ActivityChaptersListHeaderBinding
import my.noveldokusha.databinding.ActivityChaptersListItemBinding
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.uiUtils.*
import java.util.*
import kotlin.time.ExperimentalTime

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
		val chapters by lazy { ChaptersArrayAdapter(this@ChaptersActivity, viewModel) { selectionModeBarUpdateVisibility() } }
		val header by lazy { ChaptersHeaderAdapter(this@ChaptersActivity, viewModel) }
	}
	
	@ExperimentalTime
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(extras.bookMetadata)
		
		viewHolder.recyclerView.adapter = ConcatAdapter(viewAdapter.header, viewAdapter.chapters)
		viewHolder.recyclerView.itemAnimator = DefaultItemAnimator()
		viewHolder.recyclerView.itemAnimator = null
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.updateChaptersList() }
		viewModel.onFetching.observe(this) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		viewModel.chaptersWithContextLiveData.observe(this) {
			viewAdapter.chapters.setList(it)
		}
		
		setupSelectionModeBar()
		
		viewHolder.floatingActionButton.setOnClickListener {
			val bookUrl = viewModel.bookMetadata.url
			lifecycleScope.launch(Dispatchers.IO) {
				val lastReadChapter = bookstore.bookLibrary.get(extras.bookMetadata.url)?.lastReadChapter
				                      ?: bookstore.bookChapter.getFirstChapter(extras.bookMetadata.url)?.url
				
				if (lastReadChapter == null)
				{
					toast(R.string.no_chapters.stringRes())
					return@launch
				}
				
				withContext(Dispatchers.Main) {
					ReaderActivity
						.IntentData(this@ChaptersActivity, bookUrl = bookUrl, chapterUrl = lastReadChapter)
						.let(this@ChaptersActivity::startActivity)
				}
			}
		}
		
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
			val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
			viewModel.selectedChaptersUrl.addAll(chapters.map { it.chapter.url })
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewHolder.selectionSelectAllUnderSelected.setOnClickListener {
			val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
			val urls = chapters.dropWhile { !viewModel.selectedChaptersUrl.contains(it.chapter.url) }.map { it.chapter.url }
			viewModel.selectedChaptersUrl.addAll(urls)
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
			CoroutineScope(Dispatchers.IO).launch {
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
		
		val itemLibrary = menu!!.findItem(R.id.action_library_bookmark)!!
		
		val isInLibrary = runBlocking { bookstore.bookLibrary.existInLibrary(viewModel.bookMetadata.url) }
		setMenuIconLibraryState(isInLibrary, itemLibrary)
		
		bookstore.bookLibrary.existInLibraryFlow(viewModel.bookMetadata.url).asLiveData().observe(this) {
			setMenuIconLibraryState(it, itemLibrary)
		}
		return true
	}
	
	private fun setMenuIconLibraryState(isInLibrary: Boolean, item: MenuItem)
	{
		item.icon.setTint(if (isInLibrary) R.color.dark_orange_red.colorIdRes(this) else Color.DKGRAY)
		item.isChecked = isInLibrary
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		R.id.action_library_bookmark ->
		{
			val msg = if (!item.isChecked) R.string.added_to_library else R.string.removed_from_library
			toast(msg.stringRes())
			viewModel.toggleBookmark()
			true
		}
		R.id.action_filter ->
		{
			appSharedPreferences().CHAPTERS_SORT_ASCENDING = when (appSharedPreferences().CHAPTERS_SORT_ASCENDING)
			{
				TERNARY_STATE.active -> TERNARY_STATE.inverse
				TERNARY_STATE.inverse -> TERNARY_STATE.active
				TERNARY_STATE.inactive -> TERNARY_STATE.active
			}
			true
		}
		android.R.id.home -> this.onBackPressed().let { true }
		else -> super.onOptionsItemSelected(item)
	}
}

private class ChaptersArrayAdapter(
	private val context: BaseActivity,
	private val viewModel: ChaptersModel,
	private val selectionModeBarUpdateVisibility: () -> Unit
) : RecyclerView.Adapter<ChaptersArrayAdapter.ViewBinder>()
{
	private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<ChapterWithContext>()
	{
		override fun areItemsTheSame(oldItem: ChapterWithContext, newItem: ChapterWithContext) = oldItem.chapter.url == newItem.chapter.url
		override fun areContentsTheSame(oldItem: ChapterWithContext, newItem: ChapterWithContext) = oldItem == newItem
	})
	
	private val list get() = differ.currentList
	
	fun setList(newList: List<ChapterWithContext>) = differ.submitList(newList)
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
		ViewBinder(ActivityChaptersListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val itemData = list[position]
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
					.IntentData(context, bookUrl = viewModel.bookMetadata.url, chapterUrl = itemData.chapter.url)
					.let(context::startActivity)
			}
		}
		
		itemView.root.setOnLongClickListener {
			toggleItemSelection(itemData, itemView.selected)
			true
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	fun toggleItemSelection(itemData: ChapterWithContext, view: View)
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
			viewHolder.sourceName.text = scrubber.getCompatibleSource(viewModel.bookMetadata.url)?.name ?: ""
			
			viewModel.chaptersWithContextLiveData.observe(context) { list ->
				viewHolder.numberOfChapters.text = list.size.toString()
			}
			viewModel.onError.observe(context) { viewHolder.errorMessage.text = it }
			viewModel.onErrorVisibility.observe(context) {
				viewHolder.errorMessage.visibility = it
			}
			viewHolder.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
			{
				private var expand: Boolean = false
				override fun onLongClick(v: View?): Boolean
				{
					expand = !expand
					viewHolder.errorMessage.maxLines = if (expand) 100 else 10
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

