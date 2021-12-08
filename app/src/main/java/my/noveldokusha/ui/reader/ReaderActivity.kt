package my.noveldokusha.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.view.*
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.drop
import my.noveldokusha.*
import my.noveldokusha.databinding.*
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.uiUtils.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.roundToInt

@AndroidEntryPoint
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

    private val viewModel by viewModels<ReaderViewModel>()

    private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }
    private val viewAdapter = object
    {
        val listView by lazy {
            ReaderItemAdapter(
                this@ReaderActivity,
                viewModel.items,
                viewModel.localBookBaseFolder,
                fontsLoader,
                appPreferences,
                onChapterStartVisible = { url -> viewModel.readRoutine.setReadStart(url) },
                onChapterEndVisible = { url -> viewModel.readRoutine.setReadEnd(url) },
            )
        }
    }

    private val fontsLoader = FontsLoader()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)

        viewBind.listView.adapter = viewAdapter.listView

        viewModel.initialLoad { loadInitialChapter() }

        viewModel.readerFontSize.asFlow().drop(1).asLiveData().observe(this) {
            viewAdapter.listView.notifyDataSetChanged()
        }

        viewModel.readerFontFamily.asFlow().drop(1).asLiveData().observe(this) {
            viewAdapter.listView.notifyDataSetChanged()
        }

        viewModel.readingPosStats.observe(this) { (stats, itemPos) ->
            viewBind.infoChapterTitle.text = stats.chapter.title
            viewBind.infoCurrentChapterFromTotal.text = " ${stats.index + 1}/${viewModel.orderedChapters.size}"
            viewBind.infoChapterProgressPercentage.text = when (stats.itemCount)
            {
                0 -> "100%"
                else -> " ${ceil((itemPos.toFloat() / stats.itemCount.toFloat()) * 100f).roundToInt()}%"
            }
        }

        viewBind.settingTextSize.value = appPreferences.READER_FONT_SIZE
        viewBind.settingTextSize.addOnChangeListener { _, value, _ ->
            appPreferences.READER_FONT_SIZE = value
        }
        viewBind.listView.setOnItemLongClickListener { _, _, _, _ ->
            if (viewBind.infoContainer.isVisible)
            {
                viewBind.infoContainer.fadeOutVertical(-200f)
                viewBind.settingTextFontContainer.fadeOutVertical(200f)
                viewBind.settingTextSizeContainer.fadeOutVertical(200f)
            } else
            {
                viewBind.infoContainer.fadeInVertical(-200f)
                viewBind.settingTextFontContainer.fadeInVertical(200f)
                viewBind.settingTextSizeContainer.fadeInVertical(200f)
            }
            true
        }

        viewBind.settingTextFont.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fontsLoader.availableFonts)
        viewBind.settingTextFont.setSelection(fontsLoader.availableFonts.indexOfFirst { it == appPreferences.READER_FONT_FAMILY })
        viewBind.settingTextFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                appPreferences.READER_FONT_FAMILY = fontsLoader.availableFonts[position]
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

        // Show reader if text hasn't loaded after 200 ms of waiting
        lifecycleScope.launch(Dispatchers.Main) {
            delay(200)
            fadeIn()
        }

        // Fullscreen mode that ignores any cutout, notch etc.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.displayCutout())
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
    }

    private fun updateReadingState()
    {
        val firstVisibleItem = viewBind.listView.firstVisiblePosition
        val lastVisibleItem = viewBind.listView.lastVisiblePosition
        val totalItemCount = viewAdapter.listView.count
        val visibleItemCount = if (totalItemCount == 0) 0 else (lastVisibleItem - firstVisibleItem + 1)

        val isTop = visibleItemCount != 0 && firstVisibleItem <= 1
        val isBottom = visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount - 1

        when (viewModel.readerState)
        {
            ReaderViewModel.ReaderState.IDLE -> when
            {
                isBottom && loadNextChapter() -> run {}
                isTop && loadPreviousChapter() -> run {}
            }
            ReaderViewModel.ReaderState.LOADING -> run {}
            ReaderViewModel.ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun loadInitialChapter(): Boolean
    {
        viewModel.readerState = ReaderViewModel.ReaderState.INITIAL_LOAD
        viewAdapter.listView.clear()

        val insert = { item: ReaderItem -> viewAdapter.listView.add(item) }
        val insertAll = { items: Collection<ReaderItem> -> viewAdapter.listView.addAll(items) }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item) }

        val index = viewModel.orderedChapters.indexOfFirst { it.url == viewModel.currentChapter.url }
        if (index == -1)
        {
            MaterialDialog(this).show {
                title(text = getString(R.string.invalid_chapter))
                cornerRadius(16f)
            }
            return false
        }

        val maintainStartPosition = { fn: () -> Unit ->
            fn()
            // This is the position of the item TITLE at initialization
            viewBind.listView.setSelection(2)
        }

        viewBind.listView.isEnabled = false
        return addChapter(index, insert, insertAll, remove, maintainPosition = maintainStartPosition) {
            calculateInitialChapterPosition()
            viewBind.listView.isEnabled = true
        }
    }

    private fun calculateInitialChapterPosition() = lifecycleScope.launch(Dispatchers.Main) {
        val (index: Int, offset: Int) = viewModel.getChapterInitialPosition() ?: return@launch

        // index + 1 because it doesn't take into account the first padding view
        viewBind.listView.setSelectionFromTop(index + 1, offset)
        viewModel.readerState = ReaderViewModel.ReaderState.IDLE
        fadeIn()
        viewBind.listView.doOnNextLayout { updateReadingState() }
    }

    fun updateInfoView()
    {
        val lastVisiblePosition = viewBind.listView.lastVisiblePosition
        if (lastVisiblePosition < 0) return
        val item = viewAdapter.listView.getItem(lastVisiblePosition)
        if (item !is ReaderItem.Position) return

        viewModel.updateInfoViewTo(item.chapterUrl, item.pos)
    }

    override fun onPause()
    {
        updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int)
    {
        val item = viewAdapter.listView.getItem(firstVisibleItem)
        if (item is ReaderItem.Position)
        {
            val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
            viewModel.currentChapter = ChapterState(url = item.chapterUrl, position = item.pos, offset = offset)
        }
    }

    fun loadNextChapter(): Boolean
    {
        viewModel.readerState = ReaderViewModel.ReaderState.LOADING

        val lastItem = viewModel.items.lastOrNull()!!
        if (lastItem is ReaderItem.BOOK_END)
        {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        val insert = { item: ReaderItem -> viewAdapter.listView.add(item) }
        val insertAll = { items: Collection<ReaderItem> -> viewAdapter.listView.addAll(items) }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item) }

        val nextIndex = viewModel.getNextChapterIndex(lastItem.chapterUrl)
        if (nextIndex >= viewModel.orderedChapters.size)
        {
            lifecycleScope.launch(Dispatchers.Main) {
                insert(ReaderItem.BOOK_END(lastItem.chapterUrl))
                viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            }
            return false
        }

        return addChapter(nextIndex, insert, insertAll, remove) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
        }
    }

    fun loadPreviousChapter(): Boolean
    {
        viewModel.readerState = ReaderViewModel.ReaderState.LOADING

        val firstItem = viewModel.items.firstOrNull()!!
        if (firstItem is ReaderItem.BOOK_START)
        {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        var list_index = 0
        val insert = { item: ReaderItem -> viewAdapter.listView.insert(item, list_index); list_index += 1 }
        val insertAll = { items: Collection<ReaderItem> -> items.forEach { insert(it) } }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item); list_index -= 1 }

        val maintainLastVisiblePosition = { fn: () -> Unit ->
            val oldSize = viewAdapter.listView.count
            val lvp = viewBind.listView.lastVisiblePosition
            val ivpView = viewBind.listView.lastVisiblePosition - viewBind.listView.firstVisiblePosition
            val top = viewBind.listView.getChildAt(ivpView).run { top - paddingTop }
            fn()
            val displacement = viewAdapter.listView.count - oldSize
            viewBind.listView.setSelectionFromTop(lvp + displacement, top)
        }

        val previousIndex = viewModel.getPreviousChapterIndex(firstItem.chapterUrl)
        if (previousIndex < 0)
        {
            maintainLastVisiblePosition {
                insert(ReaderItem.BOOK_START(firstItem.chapterUrl))
            }
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        return addChapter(previousIndex, insert, insertAll, remove, maintainLastVisiblePosition) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
        }
    }

    private fun addChapter(
        index: Int,
        insert: ((ReaderItem) -> Unit),
        insertAll: ((Collection<ReaderItem>) -> Unit),
        remove: ((ReaderItem) -> Unit),
        maintainPosition: (() -> Unit) -> Unit = { it() },
        onCompletion: (() -> Unit)
    ): Boolean
    {
        val chapter = viewModel.orderedChapters[index]
        val itemProgressBar = ReaderItem.PROGRESSBAR(chapter.url)
        maintainPosition {
            insert(ReaderItem.DIVIDER(chapter.url))
            insert(ReaderItem.TITLE(chapter.url, 0, chapter.title))
            insert(itemProgressBar)
        }

        lifecycleScope.launch(Dispatchers.Default) {
            when (val res = viewModel.fetchChapterBody(chapter.url))
            {
                is Response.Success ->
                {
                    val items = textToItemsConverter(chapter.url, res.data)
                    withContext(Dispatchers.Main) {
                        viewModel.addChapterStats(chapter, items.size, index)
                        maintainPosition {
                            remove(itemProgressBar)
                            insertAll(items)
                            insert(ReaderItem.DIVIDER(chapter.url))
                        }
                        onCompletion()
                    }
                }
                is Response.Error ->
                {
                    withContext(Dispatchers.Main) {
                        viewModel.addChapterStats(chapter, 1, index)
                        maintainPosition {
                            remove(itemProgressBar)
                            insert(ReaderItem.ERROR(chapter.url, res.message))
                        }
                        onCompletion()
                    }
                }
            }
        }
        return true
    }
}


