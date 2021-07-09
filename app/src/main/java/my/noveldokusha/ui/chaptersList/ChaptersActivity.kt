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
import my.noveldokusha.uiAdapters.MyListAdapter
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
	private val viewBind by lazy { ActivityChaptersBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val chapters by lazy { ChaptersArrayAdapter(this@ChaptersActivity, viewModel) { selectionModeBarUpdateVisibility() } }
		val header by lazy { ChaptersHeaderAdapter(this@ChaptersActivity, viewModel) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		viewModel.initialization(extras.bookMetadata)
		
		viewBind.recyclerView.adapter = ConcatAdapter(viewAdapter.header, viewAdapter.chapters)
		viewBind.recyclerView.itemAnimator = DefaultItemAnimator()
		viewBind.recyclerView.itemAnimator = null
		viewBind.swipeRefreshLayout.setOnRefreshListener { viewModel.updateChaptersList() }
		viewModel.onFetching.observe(this) { viewBind.swipeRefreshLayout.isRefreshing = it }
		viewModel.chaptersWithContextLiveData.observe(this) {
			viewAdapter.chapters.list = it
		}
		
		setupSelectionModeBar()
		
		viewBind.floatingActionButton.setOnClickListener {
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
		if (viewModel.selectedChaptersUrl.isEmpty() && viewBind.selectionModeBar.isVisible)
			viewBind.selectionModeBar.fadeOutVertical(displacement = 200f)
		else if (viewModel.selectedChaptersUrl.isNotEmpty() && !viewBind.selectionModeBar.isVisible)
			viewBind.selectionModeBar.fadeInVertical(displacement = 200f)
	}
	
	fun setupSelectionModeBar()
	{
		selectionModeBarUpdateVisibility()
		
		viewBind.selectionSelectAll.setOnClickListener {
			val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
			viewModel.selectedChaptersUrl.addAll(chapters.map { it.chapter.url })
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewBind.selectionSelectAllUnderSelected.setOnClickListener {
			val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
			val urls = chapters.dropWhile { !viewModel.selectedChaptersUrl.contains(it.chapter.url) }.map { it.chapter.url }
			viewModel.selectedChaptersUrl.addAll(urls)
			viewAdapter.chapters.notifyDataSetChanged()
		}
		
		viewBind.selectionModeSetAsUnread.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookChapter.setAsUnread(list) }
		}
		
		viewBind.selectionModeSetAsRead.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) { bookstore.bookChapter.setAsRead(list) }
		}
		
		viewBind.selectionModeDownload.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			CoroutineScope(Dispatchers.IO).launch {
				list.forEach { bookstore.bookChapterBody.fetchBody(it) }
			}
		}
		
		viewBind.selectionModeDeleteDownload.setOnClickListener {
			val list = viewModel.selectedChaptersUrl.toList()
			lifecycleScope.launch(Dispatchers.IO) {
				bookstore.bookChapterBody.removeRows(list)
			}
		}
		
		viewBind.selectionClose.setOnClickListener {
			viewModel.selectedChaptersUrl.clear()
			selectionModeBarUpdateVisibility()
			viewAdapter.chapters.notifyDataSetChanged()
		}
	}
	
	override fun onBackPressed() = when
	{
		viewBind.selectionModeBar.isVisible ->
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
		
		menu!!.findItem(R.id.action_library_bookmark)!!.also { menuItem ->
			val isInLibrary = runBlocking { bookstore.bookLibrary.existInLibrary(viewModel.bookMetadata.url) }
			setMenuIconLibraryState(isInLibrary, menuItem)
			bookstore.bookLibrary
				.existInLibraryFlow(viewModel.bookMetadata.url)
				.asLiveData()
				.observe(this) { setMenuIconLibraryState(it, menuItem) }
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
) : MyListAdapter<ChapterWithContext, ChaptersArrayAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: ChapterWithContext, new: ChapterWithContext) = old.chapter.url == new.chapter.url
	override fun areContentsTheSame(old: ChapterWithContext, new: ChapterWithContext) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(ActivityChaptersListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val itemData = list[position]
		val itemBind = viewHolder.viewBind
		
		itemBind.title.text = itemData.chapter.title
		itemBind.title.alpha = if (itemData.chapter.read) 0.5f else 1.0f
		itemBind.downloaded.visibility = if (itemData.downloaded) View.VISIBLE else View.INVISIBLE
		itemBind.currentlyReading.visibility = if (itemData.lastReadChapter) View.VISIBLE else View.INVISIBLE
		itemBind.selected.visibility = if (viewModel.selectedChaptersUrl.contains(itemData.chapter.url)) View.VISIBLE else View.INVISIBLE
		
		itemBind.root.setOnClickListener {
			if (viewModel.selectedChaptersUrl.isNotEmpty())
				toggleItemSelection(itemData, itemBind.selected)
			else
			{
				ReaderActivity
					.IntentData(context, bookUrl = viewModel.bookMetadata.url, chapterUrl = itemData.chapter.url)
					.let(context::startActivity)
			}
		}
		
		itemBind.root.setOnLongClickListener {
			toggleItemSelection(itemData, itemBind.selected)
			true
		}
	}
	
	fun toggleItemSelection(itemData: ChapterWithContext, view: View)
	{
		fun <T> MutableSet<T>.removeOrAdd(value: T) = contains(value).also { if (it) remove(value) else add(value) }
		val isRemoved = viewModel.selectedChaptersUrl.removeOrAdd(itemData.chapter.url)
		view.visibility = if (isRemoved) View.INVISIBLE else View.VISIBLE
		selectionModeBarUpdateVisibility()
	}
	
	inner class ViewHolder(val viewBind: ActivityChaptersListItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}

private class ChaptersHeaderAdapter(
	val context: BaseActivity,
	val viewModel: ChaptersModel
) : RecyclerView.Adapter<ChaptersHeaderAdapter.ViewHolder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(ActivityChaptersListHeaderBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = 1
	
	override fun onBindViewHolder(binder: ViewHolder, position: Int): Unit = run { }
	
	private inner class ViewHolder(val viewBind: ActivityChaptersListHeaderBinding) : RecyclerView.ViewHolder(viewBind.root)
	{
		init
		{
			viewBind.bookTitle.text = viewModel.bookMetadata.title
			viewBind.sourceName.text = scrubber.getCompatibleSource(viewModel.bookMetadata.url)?.name ?: ""
			
			viewModel.chaptersWithContextLiveData.observe(context) { list ->
				viewBind.numberOfChapters.text = list.size.toString()
			}
			viewModel.onError.observe(context) { viewBind.errorMessage.text = it }
			viewModel.onErrorVisibility.observe(context) {
				viewBind.errorMessage.visibility = it
			}
			viewBind.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
			{
				private var expand: Boolean = false
				override fun onLongClick(v: View?): Boolean
				{
					expand = !expand
					viewBind.errorMessage.maxLines = if (expand) 100 else 10
					return true
				}
			})
			viewBind.databaseSearchButton.setOnClickListener {
				DatabaseSearchResultsActivity
					.IntentData(context, "https://www.novelupdates.com/", DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookMetadata.title))
					.let(context::startActivity)
			}
			
			viewBind.webpageOpenButton.setOnClickListener {
				Intent(Intent.ACTION_VIEW).also {
					it.data = Uri.parse(viewModel.bookMetadata.url)
				}.let(context::startActivity)
			}
		}
	}
}

