package my.noveldokusha.ui.reader

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.databinding.ActivityReaderListItemBinding
import my.noveldokusha.ui.BaseActivity

class ReaderActivity : BaseActivity()
{
	class Extras(val bookUrl: String, val bookSelectedChapterUrl: String)
	{
		fun intent(ctx: Context) = Intent(ctx, ReaderActivity::class.java).also {
			it.putExtra(::bookUrl.name, bookUrl)
			it.putExtra(::bookSelectedChapterUrl.name, bookSelectedChapterUrl)
		}
	}
	
	private val viewModel by viewModels<ReaderModel>()
	private val viewHolder by lazy { ActivityReaderBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val listView by lazy { ItemArrayAdapter(this@ReaderActivity, viewModel.items) }
	}
	
	private val extras = object
	{
		fun bookUrl() = intent.extras!!.getString(Extras::bookUrl.name)!!
		fun bookSelectedChapterUrl() = intent.extras!!.getString(Extras::bookSelectedChapterUrl.name)!!
	}
	
	private val preferences by lazy {
		object
		{
			val preferences = getSharedPreferences("READER", MODE_PRIVATE)
			
			var textSize: Float = preferences.getFloat(::textSize.name, 14f)
				set(value)
				{
					preferences.edit().putFloat(::textSize.name, value).apply()
					field = value
				}
				get() = preferences.getFloat(::textSize.name, 14f)
			
			val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				when (key)
				{
					::textSize.name -> viewAdapter.listView.notifyDataSetChanged()
				}
			}
			
			init
			{
				preferences.registerOnSharedPreferenceChangeListener(listener)
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		
		viewModel.initialization(bookUrl = extras.bookUrl(), bookSelectedChapterUrl = extras.bookSelectedChapterUrl())
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.settingTextSize.value = preferences.textSize
		
		loadInitialChapter()
		
		viewHolder.settingTextSize.addOnChangeListener { _, value, _ ->
			preferences.textSize = value
		}
		viewHolder.listView.setOnItemLongClickListener { parent, view, position, id ->
			viewHolder.settingsPanel.visibility = if (viewHolder.settingsPanel.isVisible) View.INVISIBLE else View.VISIBLE
			true
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
	
	override fun onStart()
	{
		super.onStart()
		viewHolder.listView.setOnScrollListener(object : AbsListView.OnScrollListener
		{
			override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int)
			{
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
			while (paragraphs.hasNext())
			{
				val (index, paragraph) = paragraphs.next()
				when
				{
					index == 0 -> yield(Item.BODY_START(chapterUrl, index + 1, paragraph) as Item)
					!paragraphs.hasNext() -> yield(Item.BODY_END(chapterUrl, index + 1, paragraph) as Item)
					else -> yield(Item.BODY(chapterUrl, index + 1, paragraph) as Item)
				}
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
		private val inflater = LayoutInflater.from(super.getContext())
		
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
		{
			val item = this.getItem(position)!!
			val itemView = when (convertView)
			{
				null -> ActivityReaderListItemBinding.inflate(inflater, parent, false).also { it.root.tag = it }
				else -> ActivityReaderListItemBinding.bind(convertView)
			}
			
			itemView.progressBar.visibility = View.GONE
			itemView.divider.visibility = View.GONE
			itemView.title.visibility = View.GONE
			itemView.error.visibility = View.GONE
			itemView.body.visibility = View.GONE
			itemView.title.text = ""
			itemView.error.text = ""
			itemView.body.text = ""
			itemView.body.textSize = preferences.textSize
			
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