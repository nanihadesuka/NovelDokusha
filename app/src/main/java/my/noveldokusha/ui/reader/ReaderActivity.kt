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
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.databinding.ActivityReaderListItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.fadeInVertical
import my.noveldokusha.uiUtils.fadeOutVertical
import my.noveldokusha.uiUtils.inflater
import kotlin.math.ceil

class ReaderActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var bookUrl by Extra_String()
		var bookSelectedChapterUrl by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, bookUrl: String, bookSelectedChapterUrl: String) : super(ctx, ReaderActivity::class.java)
		{
			this.bookUrl = bookUrl
			this.bookSelectedChapterUrl = bookSelectedChapterUrl
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModels<ReaderModel>()
	private val viewHolder by lazy { ActivityReaderBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { ItemArrayAdapter(this@ReaderActivity, viewModel.items) }
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
		setContentView(viewHolder.root)
		
		viewModel.initialization(bookUrl = extras.bookUrl, bookSelectedChapterUrl = extras.bookSelectedChapterUrl)
		
		sharedPreferences.registerOnSharedPreferenceChangeListener(listenerSharedPreferences)
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.settingTextSize.value = sharedPreferences.READER_FONT_SIZE
		
		loadInitialChapter()
		
		viewHolder.settingTextSize.addOnChangeListener { _, value, _ ->
			sharedPreferences.READER_FONT_SIZE = value
		}
		viewHolder.listView.setOnItemLongClickListener { _, _, _, _ ->
			if (viewHolder.infoContainer.isVisible)
			{
				viewHolder.infoContainer.fadeOutVertical(-200f)
				viewHolder.settingTextFontContainer.fadeOutVertical(200f)
				viewHolder.settingTextSizeContainer.fadeOutVertical(200f)
			}
			else
			{
				viewHolder.infoContainer.fadeInVertical(-200f)
				viewHolder.settingTextFontContainer.fadeInVertical(200f)
				viewHolder.settingTextSizeContainer.fadeInVertical(200f)
			}
			true
		}
		
		viewHolder.settingTextFont.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, availableFonts)
		viewHolder.settingTextFont.setSelection(availableFonts.indexOfFirst { it == sharedPreferences.READER_FONT_FAMILY })
		viewHolder.settingTextFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
		{
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
			{
				sharedPreferences.READER_FONT_FAMILY = availableFonts[position]
			}
			
			override fun onNothingSelected(parent: AdapterView<*>?) = Unit
		}
		
		@Suppress("DEPRECATION")
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
	}
	
	private fun loadInitialChapter()
	{
		viewModel.state = ReaderModel.State.INITIAL_LOAD
		viewAdapter.listView.clear()
		
		val insert = { item: Item -> viewAdapter.listView.add(item) }
		val insertAll = { items: Collection<Item> -> viewAdapter.listView.addAll(items) }
		val remove = { item: Item -> viewAdapter.listView.remove(item) }
		
		val chapter = viewModel.orderedChapters.find { it.url == viewModel.bookSelectedChapterUrl }!!
		addChapter(chapter, insert, insertAll, remove) {
			viewModel.state = ReaderModel.State.INITIAL_LOAD_COMPLETED
		}
	}
	
	fun updateInfoView()
	{
		val lastVisiblePosition = viewHolder.listView.lastVisiblePosition
		if (lastVisiblePosition < 0) return
		val item = viewAdapter.listView.getItem(lastVisiblePosition)
		if (item !is Item.Position) return
		
		val currentChapterUrl = item.url
		lifecycleScope.launch(Dispatchers.Default) {
			val index = viewModel.orderedChapters.indexOfFirst { it.url == currentChapterUrl }
			val pos = index + 1
			val itemPos = item.pos.toFloat()
			val title = viewModel.orderedChapters[index].title
			withContext(Dispatchers.Main) {
				viewHolder.infoChapterTitle.text = title
				viewHolder.infoCurrentChapterFromTotal.text = " $pos/${viewModel.orderedChapters.size}"
				val itemMaxPos = viewModel.chaptersSize.getOrDefault(item.url, 0).coerceAtLeast(1).toFloat()
				viewHolder.infoChapterProgressPercentage.text = " ${ceil((itemPos / itemMaxPos) * 100f)}%"
			}
		}
	}
	
	override fun onStart()
	{
		super.onStart()
		viewHolder.listView.setOnScrollListener(object : AbsListView.OnScrollListener
		{
			override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int)
			{
				updateInfoView()
				
				val isTop = visibleItemCount != 0 && firstVisibleItem == 0
				val isBottom = visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) == totalItemCount
				if (isTop && viewModel.isTop_firstCall)
				{
					// Ignore first event
					viewModel.isTop_firstCall = false
					return
				}
				
				when (viewModel.state)
				{
					ReaderModel.State.IDLE ->
					{
						if (isTop) loadPreviousChapter()
						else if (isBottom) loadNextChapter()
						updateCurrentReadingPosSavingState(firstVisibleItem)
						return
					}
					ReaderModel.State.LOADING ->
					{
						updateCurrentReadingPosSavingState(firstVisibleItem)
					}
					ReaderModel.State.INITIAL_LOAD -> return
					ReaderModel.State.INITIAL_LOAD_COMPLETED -> lifecycleScope.launch {
						
						val (index: Int, offset: Int) = getChapterInitialPosition(
							bookUrl = viewModel.bookUrl,
							chapterUrl = viewModel.bookSelectedChapterUrl,
							items = viewModel.items
						)
						runOnUiThread {
							viewHolder.listView.setSelectionFromTop(index, offset)
							viewModel.state = ReaderModel.State.IDLE
						}
					}
				}
			}
			
			override fun onScrollStateChanged(view: AbsListView?, scrollState: Int)
			{
			}
		})
	}
	
	private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int)
	{
		val item = viewModel.items[firstVisibleItem]
		if (item is Item.Position)
		{
			val offset = viewHolder.listView.run { getChildAt(0).top - paddingTop }
			viewModel.currentChapter.url = item.url
			viewModel.currentChapter.position = item.pos
			viewModel.currentChapter.offset = offset
			viewModel.bookSelectedChapterUrl = item.url
		}
	}
	
	override fun onPause()
	{
		updateCurrentReadingPosSavingState(viewHolder.listView.firstVisiblePosition)
		viewModel.saveLastReadPositionState()
		super.onPause()
	}
	
	override fun onBackPressed()
	{
		updateCurrentReadingPosSavingState(viewHolder.listView.firstVisiblePosition)
		viewModel.saveLastReadPositionState()
		super.onBackPressed()
	}
	
	fun loadNextChapter()
	{
		viewModel.state = ReaderModel.State.LOADING
		
		val lastItem = viewModel.items.lastOrNull()!!
		if (lastItem is Item.BOOK_END)
		{
			viewModel.state = ReaderModel.State.IDLE
			return
		}
		
		val insert = { item: Item -> viewAdapter.listView.add(item) }
		val insertAll = { items: Collection<Item> -> viewAdapter.listView.addAll(items) }
		val remove = { item: Item -> viewAdapter.listView.remove(item) }
		
		val nextIndex = viewModel.orderedChapters.indexOfFirst { it.url == lastItem.url } + 1
		if (nextIndex >= viewModel.orderedChapters.size)
		{
			insert(Item.BOOK_END(lastItem.url))
			viewModel.state = ReaderModel.State.IDLE
			return
		}
		
		val chapter = viewModel.orderedChapters[nextIndex]
		addChapter(chapter, insert, insertAll, remove) {
			viewModel.state = ReaderModel.State.IDLE
		}
	}
	
	fun loadPreviousChapter()
	{
		viewModel.state = ReaderModel.State.LOADING
		
		val firstItem = viewModel.items.firstOrNull()!!
		if (firstItem is Item.BOOK_START)
		{
			viewModel.state = ReaderModel.State.IDLE
			return
		}
		
		var list_index = 0
		val insert = { item: Item -> viewAdapter.listView.insert(item, list_index); list_index += 1 }
		val insertAll = { items: Collection<Item> -> items.forEach { insert(it) } }
		val remove = { item: Item -> viewAdapter.listView.remove(item); list_index -= 1 }
		
		val maintainLastVisiblePosition = { fn: () -> Unit ->
			val oldSize = viewAdapter.listView.count
			val lvp = viewHolder.listView.lastVisiblePosition
			val ivpView = viewHolder.listView.lastVisiblePosition - viewHolder.listView.firstVisiblePosition
			val top = viewHolder.listView.getChildAt(ivpView).run { top - paddingTop }
			fn()
			val displacement = viewAdapter.listView.count - oldSize
			viewHolder.listView.setSelectionFromTop(lvp + displacement, top)
		}
		
		val previousIndex = viewModel.orderedChapters.indexOfFirst { it.url == firstItem.url } - 1
		if (previousIndex < 0)
		{
			maintainLastVisiblePosition {
				insert(Item.BOOK_START(firstItem.url))
			}
			viewModel.state = ReaderModel.State.IDLE
			return
		}
		
		val chapter = viewModel.orderedChapters[previousIndex]
		addChapter(chapter, insert, insertAll, remove, maintainLastVisiblePosition) {
			viewModel.state = ReaderModel.State.IDLE
		}
	}
	
	private fun textToItems(chapterUrl: String, text: String): List<Item>
	{
		val paragraphs = text
			.splitToSequence("\n\n")
			.filter { it.isNotBlank() }
			.map { it + "\n" }.withIndex().iterator()
		
		return sequence {
			for ((index, paragraph) in paragraphs)
			{
				val item = when
				{
					index == 0 -> Item.BODY_START(chapterUrl, index + 1, paragraph)
					!paragraphs.hasNext() -> Item.BODY_END(chapterUrl, index + 1, paragraph)
					else -> Item.BODY(chapterUrl, index + 1, paragraph)
				}
				yield(item)
			}
		}.toList()
	}
	
	private fun addChapter(
		chapter: bookstore.Chapter,
		insert: ((Item) -> Unit),
		insertAll: ((Collection<Item>) -> Unit),
		remove: ((Item) -> Unit),
		maintainPosition: (() -> Unit) -> Unit = { it() },
		onCompletion: (() -> Unit)
	)
	{
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
					runOnUiThread {
						viewModel.chaptersSize[chapter.url] = items.size
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
					runOnUiThread {
						maintainPosition {
							remove(itemProgressBar)
							insert(Item.ERROR(chapter.url, res.message))
						}
						onCompletion()
					}
				}
			}
		}
	}
	
	sealed class Item(open val url: String)
	{
		interface Position
		{
			val pos: Int
		}
		
		data class TITLE(override val url: String, override val pos: Int, val text: String) : Item(url), Position
		data class BODY_START(override val url: String, override val pos: Int, val text: String) : Item(url), Position
		data class BODY_END(override val url: String, override val pos: Int, val text: String) : Item(url), Position
		data class BODY(override val url: String, override val pos: Int, val text: String) : Item(url), Position
		class PROGRESSBAR(override val url: String) : Item(url)
		class DIVIDER(override val url: String) : Item(url)
		class BOOK_END(override val url: String) : Item(url)
		class BOOK_START(override val url: String) : Item(url)
		class ERROR(override val url: String, val text: String) : Item(url)
	}
	
	inner class ItemArrayAdapter(context: Context, list: ArrayList<Item>) : ArrayAdapter<Item>(context, 0, list)
	{
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
		{
			val item = this.getItem(position)!!
			val itemView = when (convertView)
			{
				null -> ActivityReaderListItemBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
				else -> ActivityReaderListItemBinding.bind(convertView)
			}
			
			itemView.progressBar.visibility = View.GONE
			itemView.divider.visibility = View.GONE
			itemView.title.visibility = View.GONE
			itemView.error.visibility = View.GONE
			itemView.body.visibility = View.GONE
			itemView.title.text = ""
			itemView.title.typeface = getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY)
			itemView.error.text = ""
			itemView.body.text = ""
			itemView.body.textSize = sharedPreferences.READER_FONT_SIZE
			itemView.body.typeface = getFontFamilyNORMAL(sharedPreferences.READER_FONT_FAMILY)
			
			when (item)
			{
				is Item.BODY ->
				{
					itemView.body.visibility = View.VISIBLE
					itemView.body.text = item.text
				}
				is Item.BODY_START ->
				{
					itemView.body.visibility = View.VISIBLE
					itemView.body.text = item.text
					viewModel.readRoutine?.setReadStart(item.url)
				}
				is Item.BODY_END ->
				{
					itemView.body.visibility = View.VISIBLE
					itemView.body.text = item.text
					viewModel.readRoutine?.setReadEnd(item.url)
				}
				is Item.PROGRESSBAR -> itemView.progressBar.visibility = View.VISIBLE
				is Item.DIVIDER -> itemView.divider.visibility = View.VISIBLE
				is Item.TITLE ->
				{
					itemView.title.visibility = View.VISIBLE
					itemView.title.text = item.text
				}
				is Item.BOOK_END ->
				{
					itemView.title.visibility = View.VISIBLE
					itemView.title.text = getString(R.string.reader_no_more_chapters)
				}
				is Item.BOOK_START ->
				{
					itemView.title.visibility = View.VISIBLE
					itemView.title.text = getString(R.string.reader_first_chapter)
				}
				is Item.ERROR ->
				{
					itemView.error.visibility = View.VISIBLE
					itemView.error.text = item.text
				}
			}
			return itemView.root
		}
	}
}