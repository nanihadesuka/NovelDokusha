package my.noveldokusha.ui.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.AbsListView
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.colorAttrRes
import my.noveldokusha.utils.fadeIn
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

@AndroidEntryPoint
class ReaderActivity : BaseActivity() {
    class IntentData : Intent {
        var bookUrl by Extra_String()
        var chapterUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookUrl: String, chapterUrl: String) : super(
            ctx,
            ReaderActivity::class.java
        ) {
            this.bookUrl = bookUrl
            this.chapterUrl = chapterUrl
        }
    }

    private val fadeInTextAlready = AtomicBoolean(false)
    private fun fadeInText() {
        if (fadeInTextAlready.compareAndSet(false, true))
            viewBind.listView.fadeIn()
    }

    private val viewModel by viewModels<ReaderViewModel>()

    private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }
    private val viewAdapter = object {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)

        viewBind.listView.adapter = viewAdapter.listView

        viewModel.initialLoad { loadInitialChapter() }

        viewBind.settings.setContent {
            Theme(
                appPreferences = appPreferences,
                wrapper = {
                    // Necessay so that text knows what color it must be given that the
                    // background is transparent (no Surface parent)
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colors.onSecondary
                    ) { it() }
                }
            ) {
                val textFont by remember { appPreferences.READER_FONT_FAMILY.state(lifecycleScope) }
                val textSize by remember { appPreferences.READER_FONT_SIZE.state(lifecycleScope) }
                val stats by viewModel.readingPosStats.observeAsState()
                val percetage by remember {
                    derivedStateOf {
                        val (info, itemPos) = stats ?: return@derivedStateOf 0f
                        when (info.itemCount) {
                            0 -> 100f
                            else -> ceil((itemPos.toFloat() / info.itemCount.toFloat()) * 100f)
                        }
                    }
                }

                // Notify manually text font changed for list view
                LaunchedEffect(true) {
                    snapshotFlow { textFont }.drop(1)
                        .collect { viewAdapter.listView.notifyDataSetChanged() }
                }

                // Notify manually text size changed for list view
                LaunchedEffect(true) {
                    snapshotFlow { textSize }.drop(1)
                        .collect { viewAdapter.listView.notifyDataSetChanged() }
                }

                // Capture back action when viewing info
                BackHandler(enabled = viewModel.showReaderInfoView) {
                    viewModel.showReaderInfoView = false
                }


                // Reader info
                ReaderInfoView(
                    chapterTitle = stats?.run { first.chapter.title } ?: "",
                    chapterCurrentNumber = stats?.run { first.index + 1 } ?: 0,
                    chapterPercentageProgress = percetage,
                    chaptersTotalSize = viewModel.orderedChapters.size,
                    textFont = textFont,
                    textSize = textSize,
                    visible = viewModel.showReaderInfoView,
                    onTextFontChanged = { appPreferences.READER_FONT_FAMILY.value = it },
                    onTextSizeChanged = { appPreferences.READER_FONT_SIZE.value = it }
                )
            }
        }

        viewBind.listView.setOnItemLongClickListener { _, _, _, _ ->
            viewModel.showReaderInfoView = !viewModel.showReaderInfoView
            true
        }

        viewBind.listView.setOnScrollListener(
            object : AbsListView.OnScrollListener {
                override fun onScroll(
                    view: AbsListView?,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
                    updateInfoView()
                    updateReadingState()
                }

                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) = run { }
            })

        // Show reader if text hasn't loaded after 200 ms of waiting
        lifecycleScope.launch(Dispatchers.Main)
        {
            delay(200)
            fadeInText()
        }

        // Fullscreen mode that ignores any cutout, notch etc.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.displayCutout())
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
    }

    private fun updateReadingState() {
        val firstVisibleItem = viewBind.listView.firstVisiblePosition
        val lastVisibleItem = viewBind.listView.lastVisiblePosition
        val totalItemCount = viewAdapter.listView.count
        val visibleItemCount =
            if (totalItemCount == 0) 0 else (lastVisibleItem - firstVisibleItem + 1)

        val isTop = visibleItemCount != 0 && firstVisibleItem <= 1
        val isBottom =
            visibleItemCount != 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount - 1

        when (viewModel.readerState) {
            ReaderViewModel.ReaderState.IDLE -> when {
                isBottom && loadNextChapter() -> run {}
                isTop && loadPreviousChapter() -> run {}
            }
            ReaderViewModel.ReaderState.LOADING -> run {}
            ReaderViewModel.ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun loadInitialChapter(): Boolean {
        viewModel.readerState = ReaderViewModel.ReaderState.INITIAL_LOAD
        viewAdapter.listView.clear()

        val insert = { item: ReaderItem -> viewAdapter.listView.add(item) }
        val insertAll = { items: Collection<ReaderItem> -> viewAdapter.listView.addAll(items) }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item) }

        val index =
            viewModel.orderedChapters.indexOfFirst { it.url == viewModel.currentChapter.url }
        if (index == -1) {
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
        return addChapter(
            index,
            insert,
            insertAll,
            remove,
            maintainPosition = maintainStartPosition
        ) {
            calculateInitialChapterPosition()
            viewBind.listView.isEnabled = true
        }
    }

    private fun calculateInitialChapterPosition() = lifecycleScope.launch(Dispatchers.Main) {
        val (index: Int, offset: Int) = viewModel.getChapterInitialPosition() ?: return@launch

        // index + 1 because it doesn't take into account the first padding view
        viewBind.listView.setSelectionFromTop(index + 1, offset)
        viewModel.readerState = ReaderViewModel.ReaderState.IDLE
        fadeInText()
        viewBind.listView.doOnNextLayout { updateReadingState() }
    }

    fun updateInfoView() {
        val lastVisiblePosition = viewBind.listView.lastVisiblePosition
        if (lastVisiblePosition < 0) return
        val item = viewAdapter.listView.getItem(lastVisiblePosition)
        if (item !is ReaderItem.Position) return

        viewModel.updateInfoViewTo(item.chapterUrl, item.pos)
    }

    override fun onPause() {
        updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int) {
        val item = viewAdapter.listView.getItem(firstVisibleItem)
        if (item is ReaderItem.Position) {
            val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
            viewModel.currentChapter =
                ChapterState(url = item.chapterUrl, position = item.pos, offset = offset)
        }
    }

    fun loadNextChapter(): Boolean {
        viewModel.readerState = ReaderViewModel.ReaderState.LOADING

        val lastItem = viewModel.items.lastOrNull()!!
        if (lastItem is ReaderItem.BOOK_END) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        val insert = { item: ReaderItem -> viewAdapter.listView.add(item) }
        val insertAll = { items: Collection<ReaderItem> -> viewAdapter.listView.addAll(items) }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item) }

        val nextIndex = viewModel.getNextChapterIndex(lastItem.chapterUrl)
        if (nextIndex >= viewModel.orderedChapters.size) {
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

    fun loadPreviousChapter(): Boolean {
        viewModel.readerState = ReaderViewModel.ReaderState.LOADING

        val firstItem = viewModel.items.firstOrNull()!!
        if (firstItem is ReaderItem.BOOK_START) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        var list_index = 0
        val insert =
            { item: ReaderItem -> viewAdapter.listView.insert(item, list_index); list_index += 1 }
        val insertAll = { items: Collection<ReaderItem> -> items.forEach { insert(it) } }
        val remove = { item: ReaderItem -> viewAdapter.listView.remove(item); list_index -= 1 }

        val maintainLastVisiblePosition = { fn: () -> Unit ->
            val oldSize = viewAdapter.listView.count
            val lvp = viewBind.listView.lastVisiblePosition
            val ivpView =
                viewBind.listView.lastVisiblePosition - viewBind.listView.firstVisiblePosition
            val top = viewBind.listView.getChildAt(ivpView).run { top - paddingTop }
            fn()
            val displacement = viewAdapter.listView.count - oldSize
            viewBind.listView.setSelectionFromTop(lvp + displacement, top)
        }

        val previousIndex = viewModel.getPreviousChapterIndex(firstItem.chapterUrl)
        if (previousIndex < 0) {
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
    ): Boolean {
        val chapter = viewModel.orderedChapters[index]
        val itemProgressBar = ReaderItem.PROGRESSBAR(chapter.url)
        maintainPosition {
            insert(ReaderItem.DIVIDER(chapter.url))
            insert(ReaderItem.TITLE(chapter.url, 0, chapter.title))
            insert(itemProgressBar)
        }

        lifecycleScope.launch(Dispatchers.Default) {
            when (val res = viewModel.fetchChapterBody(chapter.url)) {
                is Response.Success -> {
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
                is Response.Error -> {
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


