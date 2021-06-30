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
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.databinding.ActivityReaderListItemBinding
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.uiUtils.*
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
		setContentView(viewBind.root)
		
		viewModel.initialization(bookUrl = extras.bookUrl, selectedChapter = extras.chapterUrl) {
			loadInitialChapter()
		}
		
		viewBind.listView.adapter = viewAdapter.listView
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
				updateInfoView()
				
				val isTop = visibleItemCount != 0 && firstVisibleItem == 0
				val isBottom = visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) == totalItemCount
				
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
				}
			}
			
			override fun onScrollStateChanged(view: AbsListView?, scrollState: Int)
			{
			}
		})
		
		// Show reader if text hasn't load after 200 ms of waiting
		lifecycleScope.launch(Dispatchers.Default) {
			delay(200)
			withContext(Dispatchers.Main) { fadeIn() }
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
		
		val index = viewModel.orderedChapters.indexOfFirst { it.url == viewModel.currentChapter.url }
		if (index == -1)
		{
			MaterialDialog(this).show {
				title(text = "Invalid chapter")
				cornerRadius(16f)
			}
			return
		}
		
		addChapter(index, insert, insertAll, remove) {
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
			viewBind.listView.setSelectionFromTop(index, offset)
			viewModel.state = ReaderModel.State.IDLE
			fadeIn()
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
		viewBind.infoChapterProgressPercentage.text = " ${ceil((item.pos.toFloat() / stats.size.toFloat()) * 100f).roundToInt()}%"
	}
	
	private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int)
	{
		val item = viewModel.items[firstVisibleItem]
		if (item is Item.Position)
		{
			val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
			viewModel.currentChapter = ChapterState(url = item.url, position = item.pos, offset = offset)
		}
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
		
		val nextIndex = viewModel.chaptersStats[lastItem.url]!!.index + 1
		if (nextIndex >= viewModel.orderedChapters.size)
		{
			insert(Item.BOOK_END(lastItem.url))
			viewModel.state = ReaderModel.State.IDLE
			return
		}
		
		addChapter(nextIndex, insert, insertAll, remove) {
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
			return
		}
		
		addChapter(previousIndex, insert, insertAll, remove, maintainLastVisiblePosition) {
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
		index: Int,
		insert: ((Item) -> Unit),
		insertAll: ((Collection<Item>) -> Unit),
		remove: ((Item) -> Unit),
		maintainPosition: (() -> Unit) -> Unit = { it() },
		onCompletion: (() -> Unit)
	)
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
					runOnUiThread {
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
					runOnUiThread {
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
	
	inner class ItemArrayAdapter(context: Context, val list: ArrayList<Item>) : ArrayAdapter<Item>(context, 0, list)
	{
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
		{
			val itemData = this.getItem(position)!!
			val itemBind = when (convertView)
			{
				null -> ActivityReaderListItemBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
				else -> ActivityReaderListItemBinding.bind(convertView)
			}
			
			itemBind.progressBar.visibility = View.GONE
			itemBind.divider.visibility = View.GONE
			itemBind.title.visibility = View.GONE
			itemBind.specialTitle.visibility = View.GONE
			itemBind.error.visibility = View.GONE
			itemBind.body.visibility = View.GONE
			itemBind.title.text = ""
			itemBind.specialTitle.text = ""
			itemBind.title.typeface = getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY)
			itemBind.specialTitle.typeface = getFontFamilyBOLD(sharedPreferences.READER_FONT_FAMILY)
			itemBind.error.text = ""
			itemBind.body.text = ""
			itemBind.body.textSize = sharedPreferences.READER_FONT_SIZE
			itemBind.body.typeface = getFontFamilyNORMAL(sharedPreferences.READER_FONT_FAMILY)
			
			when (itemData)
			{
				is Item.BODY ->
				{
					itemBind.body.visibility = View.VISIBLE
					itemBind.body.text = itemData.text
				}
				is Item.BODY_START ->
				{
					itemBind.body.visibility = View.VISIBLE
					itemBind.body.text = itemData.text
					viewModel.readRoutine.setReadStart(itemData.url)
				}
				is Item.BODY_END ->
				{
					itemBind.body.visibility = View.VISIBLE
					itemBind.body.text = itemData.text
					viewModel.readRoutine.setReadEnd(itemData.url)
				}
				is Item.PROGRESSBAR ->
				{
					itemBind.progressBar.visibility = View.VISIBLE
					itemBind.progressBar.addBottomMargin { position == list.lastIndex }
				}
				is Item.DIVIDER ->
				{
					itemBind.divider.visibility = View.VISIBLE
					itemBind.title.addTopMargin { position == 0 }
				}
				is Item.TITLE ->
				{
					itemBind.title.visibility = View.VISIBLE
					itemBind.title.text = itemData.text
				}
				is Item.BOOK_END ->
				{
					itemBind.specialTitle.visibility = View.VISIBLE
					itemBind.specialTitle.text = getString(R.string.reader_no_more_chapters)
					itemBind.specialTitle.addBottomMargin(800) { position == list.lastIndex }
				}
				is Item.BOOK_START ->
				{
					itemBind.specialTitle.visibility = View.VISIBLE
					itemBind.specialTitle.text = getString(R.string.reader_first_chapter)
					itemBind.specialTitle.addTopMargin(500) { position == 0 }
				}
				is Item.ERROR ->
				{
					itemBind.error.visibility = View.VISIBLE
					itemBind.error.text = itemData.text
					itemBind.specialTitle.addBottomMargin(800) { position == list.lastIndex }
				}
			}
			
			return itemBind.root
		}
	}
}