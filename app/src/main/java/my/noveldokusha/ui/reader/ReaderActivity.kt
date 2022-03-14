package my.noveldokusha.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.R
import my.noveldokusha.scraper.Response
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.*
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class ReaderActivity : ComponentActivity() {
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


    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel by viewModels<ReaderViewModel>()
    private val fontsLoader = FontsLoader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initialLoad { loadInitialChapter() }

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

        setContent {
            Theme(appPreferences = appPreferences) {
                val defaultTextStyle = LocalTextStyle.current
                val textStyle by derivedStateOf {
                    defaultTextStyle.copy(
                        fontSize = viewModel.readerFontSize.sp,
                        fontFamily = fontsLoader.getFontFamilyNORMAL(viewModel.readerFontFamily)
                    )
                }
                val chapterTitle: String by derivedStateOf {
                    val (stats, _) = viewModel.readingPosStats ?: return@derivedStateOf ""
                    stats.chapter.title
                }
                val chapterCurrentNumber: Int by derivedStateOf {
                    val (stats, _) = viewModel.readingPosStats ?: return@derivedStateOf 0
                    stats.index + 1
                }
                val chapterPercentageProgress: Float by derivedStateOf {
                    val (stats, pos) = viewModel.readingPosStats ?: return@derivedStateOf 0f
                    when (stats.itemCount) {
                        0 -> 100f
                        else -> ceil((pos.toFloat() / stats.itemCount.toFloat()) * 100f)
                    }
                }
                val chaptersTotalSize: Int = remember { viewModel.orderedChapters.size }

                var showInfoView by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(
                    viewModel.listState.firstVisibleItemIndex,
                    viewModel.listState.layoutInfo.totalItemsCount
                ) {
                    val items = viewModel.listState.layoutInfo.visibleItemsInfo
                    val firstItem = viewModel.list.getOrNull(items.first().index)
                    val lastItem = viewModel.list.getOrNull(items.last().index)

                    // Update info overlay data
                    if (firstItem is ReaderItem.Position) {
                        viewModel.updateInfoViewTo(firstItem.chapterUrl, firstItem.pos)
                    }

                    // Update reading state when one reaches start/end of a chapter
                    listOf(firstItem, lastItem).filterIsInstance<ReaderItem.BODY>().forEach {
                        when (it.location) {
                            ReaderItem.LOCATION.FIRST ->
                                viewModel.readRoutine.setReadStart(it.chapterUrl)
                            ReaderItem.LOCATION.LAST ->
                                viewModel.readRoutine.setReadEnd(it.chapterUrl)
                            else -> run { }
                        }
                    }

                    updateCurrentReadingPosSavingState()
                }

                LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showInfoView = !showInfoView }
                            )
                        }
                ) {
                    item (key = "#firstOne#"){
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                    items(
                        items = viewModel.list,
                        key = { it.key }
                    ) {
                        ReaderItemView(
                            item = it,
                            localBookBaseFolder = viewModel.localBookBaseFolder,
                            textStyle = textStyle
                        )
                    }
                    item (key = "#lastOne#"){
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }

                val isIDLE by remember {
                    derivedStateOf { viewModel.readerState == ReaderViewModel.ReaderState.IDLE }
                }
                if (isIDLE) {
                    viewModel.listState.OnTopReached(2) { loadPreviousChapter() }
                    viewModel.listState.OnBottonReached(2) { loadNextChapter() }
                }

                AnimatedVisibility(
                    visible = showInfoView,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = fadeOut()
                ) {
                    BackHandler(true) { showInfoView = false }
                    ReaderInfoView(
                        chapterTitle = chapterTitle,
                        chapterCurrentNumber = chapterCurrentNumber,
                        chapterPercentageProgress = chapterPercentageProgress,
                        chaptersTotalSize = chaptersTotalSize,
                        textSize = viewModel.readerFontSize,
                        textFont = viewModel.readerFontFamily,
                        onTextSizeChanged = {
                            viewModel.appPreferences.READER_FONT_SIZE = it
                        },
                        onTextFontChanged = {
                            viewModel.appPreferences.READER_FONT_FAMILY = it
                        }
                    )
                }
            }
        }
    }

    private fun loadInitialChapter(): Boolean {
        viewModel.readerState = ReaderViewModel.ReaderState.INITIAL_LOAD

        val maintainLastVisiblePosition = { fn: () -> Unit ->
            fn()
            viewModel.viewModelScope.launch {
                viewModel.listState.scrollToItem(2)
            }
            Unit
        }

        val insert = { item: ReaderItem ->
            viewModel.list.add(item)
            Unit
        }
        val insertAll = { items: Collection<ReaderItem> ->
            viewModel.list.addAll(items)
            Unit
        }
        val remove = { item: ReaderItem ->
            viewModel.list.remove(item)
            Unit
        }

        val index = viewModel.orderedChapters.indexOfFirst {
            it.url == viewModel.currentChapter.url
        }
        if (index == -1) {
            MaterialDialog(this).show {
                title(text = getString(R.string.invalid_chapter))
                cornerRadius(16f)
            }
            return false
        }


        return addChapter(
            index,
            insert,
            insertAll,
            remove,
            maintainLastVisiblePosition
        ) {
            calculateInitialChapterPosition()
        }
    }

    private fun calculateInitialChapterPosition() = lifecycleScope.launch(Dispatchers.Main) {
        val (index: Int, offset: Int) = viewModel.getChapterInitialPosition() ?: return@launch

        // first items are always in this order: padding, divider, title
        viewModel.listState.scrollToItem(index + 1, offset)
        viewModel.readerState = ReaderViewModel.ReaderState.IDLE
    }

    override fun onPause() {
        updateCurrentReadingPosSavingState()
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState() {
        // Remember the liststate has the extra padding items
        val item = viewModel.list.getOrNull(viewModel.listState.firstVisibleItemIndex - 1)
        if (item !is ReaderItem.Position)
            return

        viewModel.currentChapter = ChapterState(
            url = item.chapterUrl,
            position = item.pos,
            offset = viewModel.listState.firstVisibleItemScrollOffset
        )
    }

    fun loadNextChapter(): Boolean {
        viewModel.readerState = ReaderViewModel.ReaderState.LOADING

        val lastItem = viewModel.list.lastOrNull()!!
        if (lastItem is ReaderItem.BOOK_END) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        val insert = { item: ReaderItem ->
            viewModel.list.add(item)
            Unit
        }
        val insertAll = { items: Collection<ReaderItem> ->
            viewModel.list.addAll(items)
            Unit
        }
        val remove = { item: ReaderItem ->
            viewModel.list.remove(item)
            Unit
        }

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

        val firstItem = viewModel.list.firstOrNull()!!
        if (firstItem is ReaderItem.BOOK_START) {
            viewModel.readerState = ReaderViewModel.ReaderState.IDLE
            return false
        }

        var list_index = 0
        val insert = { item: ReaderItem -> viewModel.list.add(list_index, item); list_index += 1 }
        val insertAll = { items: Collection<ReaderItem> -> items.forEach { insert(it) } }
        val remove = { item: ReaderItem -> viewModel.list.remove(item); list_index -= 1 }

        val maintainLastVisiblePosition = scope@{ fn: () -> Unit ->
            val oldSize = viewModel.list.size
            val lastVisibleItem =
                viewModel.listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@scope
            val lastVisibleItemIndex = lastVisibleItem.index
            val lastVisibleItemOffset = lastVisibleItem.offset
            fn()
            val displacement = viewModel.list.size - oldSize
            viewModel.viewModelScope.launch {
                viewModel.listState.scrollToItem(
                    lastVisibleItemIndex + displacement,
                    lastVisibleItemOffset
                )
            }
            Unit
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
