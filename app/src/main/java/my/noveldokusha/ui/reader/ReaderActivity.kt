package my.noveldokusha.ui.reader

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.databinding.*
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.uiUtils.*
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.roundToInt

class ReaderActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var bookUrl by Extra_String()
		var chapterUrl by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, bookUrl: String, chapterUrl: String) : super(ctx, ReaderActivity::class.java)
		{
			this.bookUrl = bookUrl
			this.chapterUrl = chapterUrl
		}
	}
	
	private val fadeInAlready = AtomicBoolean(false)
	private fun fadeIn()
	{
		if (fadeInAlready.compareAndSet(false, true))
			viewBind.listView.fadeIn()
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<ReaderModel>()
	private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { ItemArrayAdapter(this@ReaderActivity, viewModel, viewModel.items, extras.bookUrl) }
	}
	
	val listenerSharedPreferences = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		when (key)
		{
			sharedPreferences::READER_FONT_SIZE.name -> viewAdapter.listView.notifyDataSetChanged()
			sharedPreferences::READER_FONT_FAMILY.name -> viewAdapter.listView.notifyDataSetChanged()
		}
	}
	
	val availableFonts = listOf(
		"casual",
		"cursive",
		"monospace",
		"sans-serif",
		"sans-serif-black",
		"sans-serif-condensed",
		"sans-serif-condensed-light",
		"sans-serif-light",
		"sans-serif-medium",
		"sans-serif-smallcaps",
		"sans-serif-thin",
		"serif",
		"serif-monospace"
	)
	
	private val fontFamilyNORMALCache = mutableMapOf<String, Typeface>()
	private val fontFamilyBOLDCache = mutableMapOf<String, Typeface>()
	fun getFontFamilyNORMAL(name: String) = fontFamilyNORMALCache.getOrPut(name) { Typeface.create(name, Typeface.NORMAL) }
	fun getFontFamilyBOLD(name: String) = fontFamilyBOLDCache.getOrPut(name) { Typeface.create(name, Typeface.BOLD) }
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		
		viewBind.listView.adapter = viewAdapter.listView
		
		viewModel.initialization(bookUrl = extras.bookUrl, selectedChapter = extras.chapterUrl) {
			loadInitialChapter()
		}
		
		sharedPreferences.registerOnSharedPreferenceChangeListener(listenerSharedPreferences)
		
		viewBind.settingTextSize.value = sharedPreferences.READER_FONT_SIZE
		viewBind.settingTextSize.addOnChangeListener { _, value, _ ->
			sharedPreferences.READER_FONT_SIZE = value
		}
		viewBind.listView.setOnItemLongClickListener { _, _, _, _ ->
			if (viewBind.infoContainer.isVisible)
			{
				viewBind.infoContainer.fadeOutVertical(-200f)
				viewBind.settingTextFontContainer.fadeOutVertical(200f)
				viewBind.settingTextSizeContainer.fadeOutVertical(200f)
			}
			else
			{
				viewBind.infoContainer.fadeInVertical(-200f)
				viewBind.settingTextFontContainer.fadeInVertical(200f)
				viewBind.settingTextSizeContainer.fadeInVertical(200f)
			}
			true
		}
		
		viewBind.settingTextFont.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, availableFonts)
		viewBind.settingTextFont.setSelection(availableFonts.indexOfFirst { it == sharedPreferences.READER_FONT_FAMILY })
		viewBind.settingTextFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
		{
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
			{
				sharedPreferences.READER_FONT_FAMILY = availableFonts[position]
			}
			
			override fun onNothingSelected(parent: AdapterView<*>?) = Unit
		}
		
		viewBind.listView.setOnScrollListener(object : AbsListView.OnScrollListener
		{
			override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int)
			{
				updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
				updateInfoView()
				updateReadingState()
			}
			
			override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) = run { }
		})
		
		// Show reader if text hasn't load after 200 ms of waiting
		lifecycleScope.launch(Dispatchers.Default) {
			delay(200)
			withContext(Dispatchers.Main) { fadeIn() }
		}
		
		@Suppress("DEPRECATION")
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
	}
	
	private fun updateReadingState()
	{
		val firstVisibleItem = viewBind.listView.firstVisiblePosition
		val lastVisibleItem = viewBind.listView.lastVisiblePosition
		val totalItemCount = viewAdapter.listView.count
		val visibleItemCount = if (totalItemCount == 0) 0 else (lastVisibleItem - firstVisibleItem + 1)
		
		val isTop = visibleItemCount != 0 && firstVisibleItem <= 1
		val isBottom = visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount - 1
		
		when (viewModel.state)
		{
			ReaderModel.State.IDLE -> when
			{
				isBottom && loadNextChapter() -> run {}
				isTop && loadPreviousChapter() -> run {}
			}
			ReaderModel.State.LOADING -> run {}
			ReaderModel.State.INITIAL_LOAD -> run {}
		}
	}
	
	private fun loadInitialChapter(): Boolean
	{
		viewModel.state = ReaderModel.State.INITIAL_LOAD
		viewAdapter.listView.clear()
		
		val insert = { item: Item -> viewAdapter.listView.add(item) }
		val insertAll = { items: Collection<Item> -> viewAdapter.listView.addAll(items) }
		val remove = { item: Item -> viewAdapter.listView.remove(item) }
		
		val index = viewModel.orderedChapters.indexOfFirst { it.url == viewModel.currentChapter.url }
		if (index == -1)
		{
			MaterialDialog(this).show {
				title(text = "Invalid chapter")
				cornerRadius(16f)
			}
			return false
		}
		
		val maintainStartPosition = { fn: () -> Unit ->
			fn()
			// This is the position of the item TITLE at initialization
			viewBind.listView.setSelection(2)
		}
		
		return addChapter(index, insert, insertAll, remove, maintainPosition = maintainStartPosition) {
			calculateInitialChapterPosition()
		}
	}
	
	private fun calculateInitialChapterPosition() = lifecycleScope.launch(Dispatchers.Default) {
		val (index: Int, offset: Int) = getChapterInitialPosition(
			bookUrl = viewModel.bookUrl,
			chapter = viewModel.chaptersStats[viewModel.currentChapter.url]!!.chapter,
			items = viewModel.items
		)
		
		withContext(Dispatchers.Main) {
			// index + 1 because it doesn't take into account the first padding view
			viewBind.listView.setSelectionFromTop(index + 1, offset)
			viewModel.state = ReaderModel.State.IDLE
			fadeIn()
			viewBind.listView.doOnNextLayout { updateReadingState() }
		}
	}
	
	fun updateInfoView()
	{
		val lastVisiblePosition = viewBind.listView.lastVisiblePosition
		if (lastVisiblePosition < 0) return
		val item = viewAdapter.listView.getItem(lastVisiblePosition)
		if (item !is Item.Position) return
		
		val stats = viewModel.chaptersStats.get(item.url) ?: return
		viewBind.infoChapterTitle.text = stats.chapter.title
		viewBind.infoCurrentChapterFromTotal.text = " ${stats.index + 1}/${viewModel.orderedChapters.size}"
		viewBind.infoChapterProgressPercentage.text = when (stats.size)
		{
			0 -> "100%"
			else -> " ${ceil((item.pos.toFloat() / stats.size.toFloat()) * 100f).roundToInt()}%"
		}
	}
	
	override fun onPause()
	{
		updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
		super.onPause()
	}
	
	private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int)
	{
		val item = viewAdapter.listView.getItem(firstVisibleItem)
		if (item is Item.Position)
		{
			val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
			viewModel.currentChapter = ChapterState(url = item.url, position = item.pos, offset = offset)
		}
	}
	
	fun loadNextChapter(): Boolean
	{
		viewModel.state = ReaderModel.State.LOADING
		
		val lastItem = viewModel.items.lastOrNull()!!
		if (lastItem is Item.BOOK_END)
		{
			viewModel.state = ReaderModel.State.IDLE
			return false
		}
		
		val insert = { item: Item -> viewAdapter.listView.add(item) }
		val insertAll = { items: Collection<Item> -> viewAdapter.listView.addAll(items) }
		val remove = { item: Item -> viewAdapter.listView.remove(item) }
		
		val nextIndex = viewModel.chaptersStats[lastItem.url]!!.index + 1
		if (nextIndex >= viewModel.orderedChapters.size)
		{
			lifecycleScope.launch(Dispatchers.Main) {
				insert(Item.BOOK_END(lastItem.url))
				viewModel.state = ReaderModel.State.IDLE
			}
			return false
		}
		
		return addChapter(nextIndex, insert, insertAll, remove) {
			viewModel.state = ReaderModel.State.IDLE
		}
	}
	
	fun loadPreviousChapter(): Boolean
	{
		viewModel.state = ReaderModel.State.LOADING
		
		val firstItem = viewModel.items.firstOrNull()!!
		if (firstItem is Item.BOOK_START)
		{
			viewModel.state = ReaderModel.State.IDLE
			return false
		}
		
		var list_index = 0
		val insert = { item: Item -> viewAdapter.listView.insert(item, list_index); list_index += 1 }
		val insertAll = { items: Collection<Item> -> items.forEach { insert(it) } }
		val remove = { item: Item -> viewAdapter.listView.remove(item); list_index -= 1 }
		
		val maintainLastVisiblePosition = { fn: () -> Unit ->
			val oldSize = viewAdapter.listView.count
			val lvp = viewBind.listView.lastVisiblePosition
			val ivpView = viewBind.listView.lastVisiblePosition - viewBind.listView.firstVisiblePosition
			val top = viewBind.listView.getChildAt(ivpView).run { top - paddingTop }
			fn()
			val displacement = viewAdapter.listView.count - oldSize
			viewBind.listView.setSelectionFromTop(lvp + displacement, top)
		}
		
		val previousIndex = viewModel.chaptersStats[firstItem.url]!!.index - 1
		if (previousIndex < 0)
		{
			maintainLastVisiblePosition {
				insert(Item.BOOK_START(firstItem.url))
			}
			viewModel.state = ReaderModel.State.IDLE
			return false
		}
		
		return addChapter(previousIndex, insert, insertAll, remove, maintainLastVisiblePosition) {
			viewModel.state = ReaderModel.State.IDLE
		}
	}
	
	private fun addChapter(
		index: Int,
		insert: ((Item) -> Unit),
		insertAll: ((Collection<Item>) -> Unit),
		remove: ((Item) -> Unit),
		maintainPosition: (() -> Unit) -> Unit = { it() },
		onCompletion: (() -> Unit)
	): Boolean
	{
		val chapter = viewModel.orderedChapters[index]
		val itemProgressBar = Item.PROGRESSBAR(chapter.url)
		maintainPosition {
			insert(Item.DIVIDER(chapter.url))
			insert(Item.TITLE(chapter.url, 0, chapter.title))
			insert(itemProgressBar)
		}
		
		lifecycleScope.launch(Dispatchers.Default) {
			when (val res = bookstore.bookChapterBody.fetchBody(chapter.url))
			{
				is Response.Success ->
				{
					val items = textToItems(chapter.url, res.data)
					withContext(Dispatchers.Main) {
						viewModel.chaptersStats[chapter.url] = ReaderModel.ChapterStats(size = items.size, chapter = chapter, index = index)
						maintainPosition {
							remove(itemProgressBar)
							insertAll(items)
							insert(Item.DIVIDER(chapter.url))
						}
						onCompletion()
					}
				}
				is Response.Error ->
				{
					withContext(Dispatchers.Main) {
						viewModel.chaptersStats[chapter.url] = ReaderModel.ChapterStats(size = 1, chapter = chapter, index = index)
						maintainPosition {
							remove(itemProgressBar)
							insert(Item.ERROR(chapter.url, res.message))
						}
						onCompletion()
					}
				}
			}
		}
		return true
	}
}

sealed class Item
{
	abstract val url: String
	
	interface Position
	{
		val pos: Int
	}
	
	enum class LOCATION
	{ FIRST, MIDDLE, LAST }
	
	data class TITLE(override val url: String, override val pos: Int, val text: String) : Item(), Position
	data class BODY(override val url: String, override val pos: Int, val text: String, val location: LOCATION) : Item(), Position
	{
		val image by lazy { EpubXMLFileParser.extractImgEntry(text) }
	}
	
	class PROGRESSBAR(override val url: String) : Item()
	class DIVIDER(override val url: String) : Item()
	class BOOK_END(override val url: String) : Item()
	class BOOK_START(override val url: String) : Item()
	class ERROR(override val url: String, val text: String) : Item()
	class PADDING(override val url: String) : Item()
}

private fun textToItems(chapterUrl: String, text: String): List<Item>
{
	val paragraphs = text
		.splitToSequence("\n\n")
		.filter { it.isNotBlank() }
		.withIndex().iterator()
	
	return sequence {
		for ((index, paragraph) in paragraphs)
		{
			val item = when
			{
				index == 0 -> Item.BODY(chapterUrl, index + 1, paragraph, Item.LOCATION.FIRST)
				!paragraphs.hasNext() -> Item.BODY(chapterUrl, index + 1, paragraph, Item.LOCATION.LAST)
				else -> Item.BODY(chapterUrl, index + 1, paragraph, Item.LOCATION.MIDDLE)
			}
			yield(item)
		}
	}.toList()
}

private class ItemArrayAdapter(
	val activity: ReaderActivity,
	val viewModel: ReaderModel,
	val list: ArrayList<Item>,
	val bookUrl: String,
	
	) :
	ArrayAdapter<Item>(activity, 0, list)
{
	val localBookImageBaseDir: File by lazy {
		Paths.get(App.folderBooks.path, bookUrl.removePrefix("local://")).toFile()
	}
	
	override fun getCount() = super.getCount() + 2
	override fun getItem(position: Int): Item = when (position)
	{
		0 -> topPadding
		this.count - 1 -> bottomPadding
		else -> super.getItem(position - 1)!!
	}
	
	val topPadding = Item.PADDING("")
	val bottomPadding = Item.PADDING("")
	
	override fun getViewTypeCount(): Int = 9
	override fun getItemViewType(position: Int) = when (val item = getItem(position))
	{
		is Item.BODY -> if (item.image != null) 0 else 1
		is Item.BOOK_END -> 2
		is Item.BOOK_START -> 2
		is Item.DIVIDER -> 3
		is Item.ERROR -> 4
		is Item.PADDING -> 5
		is Item.PROGRESSBAR -> 6
		is Item.TITLE -> 7
	}
	
	private fun viewBody(item: Item.BODY, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemBodyBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemBodyBinding.bind(convertView)
		}
		
		val paragraph = item.text + "\n"
		bind.body.text = paragraph
		bind.body.textSize = activity.run { sharedPreferences.READER_FONT_SIZE }
		bind.body.typeface = activity.run { getFontFamilyNORMAL(sharedPreferences.READER_FONT_FAMILY) }
		
		when (item.location)
		{
			Item.LOCATION.FIRST -> viewModel.readRoutine.setReadStart(item.url)
			Item.LOCATION.LAST -> viewModel.readRoutine.setReadEnd(item.url)
			else -> run {}
		}
		return bind.root
	}
	
	private fun viewImage(item: Item.BODY, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemImageBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemImageBinding.bind(convertView)
		}
		
		val imgEntry = item.image!!
		bind.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
			dimensionRatio = "1:${imgEntry.yrel}"
		}
		
		// Glide uses current imageView size to load the bitmap best optimized for it, but current
		// size corresponds to the last image (different size) and the view layout only updates to
		// the new values on next redraw. Execute Glide loading call in the next (parent) layout
		// update to let it get the correct values.
		// (Avoids getting "blurry" images)
		bind.imageContainer.doOnNextLayout {
			Glide.with(activity)
				.load(File(localBookImageBaseDir, imgEntry.path))
				.error(R.drawable.ic_baseline_error_outline_24)
				.transition(DrawableTransitionOptions.withCrossFade())
				.into(bind.image)
		}
		
		when (item.location)
		{
			Item.LOCATION.FIRST -> viewModel.readRoutine.setReadStart(item.url)
			Item.LOCATION.LAST -> viewModel.readRoutine.setReadEnd(item.url)
			else -> run {}
		}
		
		return bind.root
	}
	
	private fun viewBookEnd(item: Item.BOOK_END, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemSpecialTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
		}
		bind.specialTitle.text = R.string.reader_no_more_chapters.stringRes()
		bind.specialTitle.typeface = activity.run { getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY) }
		return bind.root
	}
	
	private fun viewBookStart(item: Item.BOOK_START, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemSpecialTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
		}
		bind.specialTitle.text = R.string.reader_first_chapter.stringRes()
		bind.specialTitle.typeface = activity.run { getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY) }
		return bind.root
	}
	
	private fun viewProgressbar(item: Item.PROGRESSBAR, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemProgressBarBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemProgressBarBinding.bind(convertView)
		}
		return bind.root
	}
	
	private fun viewDivider(item: Item.DIVIDER, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemDividerBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemDividerBinding.bind(convertView)
		}
		return bind.root
	}
	
	private fun viewError(item: Item.ERROR, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemErrorBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemErrorBinding.bind(convertView)
		}
		bind.error.text = item.text
		return bind.root
	}
	
	private fun viewPadding(item: Item.PADDING, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemPaddingBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemPaddingBinding.bind(convertView)
		}
		return bind.root
	}
	
	private fun viewTitle(item: Item.TITLE, convertView: View?, parent: ViewGroup): View
	{
		val bind = when (convertView)
		{
			null -> ActivityReaderListItemTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
			else -> ActivityReaderListItemTitleBinding.bind(convertView)
		}
		bind.title.text = item.text
		bind.title.typeface = activity.run { getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY) }
		return bind.root
	}
	
	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = when (val item = getItem(position))
	{
		is Item.BODY -> when (item.image)
		{
			null -> viewBody(item, convertView, parent)
			else -> viewImage(item, convertView, parent)
		}
		is Item.BOOK_END -> viewBookEnd(item, convertView, parent)
		is Item.BOOK_START -> viewBookStart(item, convertView, parent)
		is Item.DIVIDER -> viewDivider(item, convertView, parent)
		is Item.ERROR -> viewError(item, convertView, parent)
		is Item.PADDING -> viewPadding(item, convertView, parent)
		is Item.PROGRESSBAR -> viewProgressbar(item, convertView, parent)
		is Item.TITLE -> viewTitle(item, convertView, parent)
	}
}
