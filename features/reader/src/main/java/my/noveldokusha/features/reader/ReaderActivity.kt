package my.noveldokusha.features.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldoksuha.coreui.BaseActivity
import my.noveldoksuha.coreui.composableActions.SetSystemBarTransparent
import my.noveldoksuha.coreui.mappers.toPreferenceTheme
import my.noveldoksuha.coreui.theme.Theme
import my.noveldoksuha.coreui.theme.colorAttrRes
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.utils.Extra_Boolean
import my.noveldokusha.core.utils.Extra_String
import my.noveldokusha.core.utils.dpToPx
import my.noveldokusha.features.reader.domain.ChapterState
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.domain.ReaderState
import my.noveldokusha.features.reader.domain.indexOfReaderItem
import my.noveldokusha.features.reader.ui.ReaderScreen
import my.noveldokusha.features.reader.ui.ReaderViewHandlersActions
import my.noveldokusha.features.reader.ui.bookContent.ReaderBookContent
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.reader.R
import my.noveldokusha.texttospeech.Utterance
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity : BaseActivity() {
    class IntentData : Intent, ReaderStateBundle {
        override var bookUrl by Extra_String()
        override var chapterUrl by Extra_String()
        override var introScrollToSpeaker by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(
            ctx: Context,
            bookUrl: String,
            chapterUrl: String,
            scrollToSpeakingItem: Boolean = false
        ) : super(
            ctx,
            ReaderActivity::class.java
        ) {
            this.bookUrl = bookUrl
            this.chapterUrl = chapterUrl
            this.introScrollToSpeaker = scrollToSpeakingItem
        }
    }

    @Inject
    lateinit var navigationRoutes: NavigationRoutes

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    internal lateinit var readerViewHandlersActions: ReaderViewHandlersActions

    private val viewModel by viewModels<ReaderViewModel>()

    private val listState = LazyListState()

    private data class ScrollToPositionAction(
        val index: Int,
        val offset: Int = 0,
        val animated: Boolean = false,
    )

    private val scrollActions = MutableSharedFlow<ScrollToPositionAction>()

    override fun onBackPressed() {
        viewModel.onCloseManually()
        super.onBackPressed()
    }

    override fun onDestroy() {
        readerViewHandlersActions.invalidate()
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemBar()

        lifecycleScope.launch {
            viewModel.onTranslatorChanged.collect {
                viewModel.reloadReader()
            }
        }

        readerViewHandlersActions.maintainStartPosition = {
            withContext(Dispatchers.Main.immediate) {
                it()
//                val titleIndex = (0..viewAdapter.listView.count)
//                    .indexOfFirst { viewAdapter.listView.getItem(it) is ReaderItem.Title }
//
//                if (titleIndex != -1) {
//                    viewBind.listView.setSelection(titleIndex)
//                }
            }
        }

        readerViewHandlersActions.setInitialPosition = {
            withContext(Dispatchers.Main.immediate) {
                initialScrollToChapterItemPosition(
                    chapterIndex = it.chapterIndex,
                    chapterItemPosition = it.chapterItemPosition,
                    offset = it.chapterItemOffset
                )
            }
        }

        readerViewHandlersActions.maintainLastVisiblePosition = {
            withContext(Dispatchers.Main.immediate) {
//                val oldSize = viewAdapter.listView.count
//                val position = viewBind.listView.lastVisiblePosition
//                val positionView = position - viewBind.listView.firstVisiblePosition
//                val top = viewBind.listView.getChildAt(positionView).run { top - paddingTop }
                it()
//                val displacement = viewAdapter.listView.count - oldSize
//                viewBind.listView.setSelectionFromTop(position + displacement, top)
            }
        }

        viewModel.ttsScrolledToTheTop.asLiveData().observe(this) {
            if (listState.layoutInfo.totalItemsCount < 1) return@observe

            val offset = -300.dpToPx(this)
            lifecycle.coroutineScope.launch {
                scrollActions.emit(ScrollToPositionAction(index = 0, offset = offset))
            }
        }

        viewModel.ttsScrolledToTheBottom.asLiveData().observe(this) {
            if (listState.layoutInfo.totalItemsCount < 2) return@observe

            val offset = -300.dpToPx(this)
            lifecycle.coroutineScope.launch {
                scrollActions.emit(
                    ScrollToPositionAction(
                        index = listState.layoutInfo.totalItemsCount - 1,
                        offset = offset,
                        animated = true
                    )
                )
            }
        }

        viewModel.readerSpeaker.currentReaderItem
            .filter { it.playState == Utterance.PlayState.PLAYING || it.playState == Utterance.PlayState.LOADING }
            .asLiveData().observe(this) {
                scrollToReadingPositionOptional(
                    chapterIndex = it.itemPos.chapterIndex,
                    chapterItemPosition = it.itemPos.chapterItemPosition,
                )
            }

        viewModel.readerSpeaker.scrollToReaderItem.asLiveData().observe(this) {
            if (it !is ReaderItem.Position) return@observe
            scrollToReadingPositionForced(
                chapterIndex = it.chapterIndex,
                chapterItemPosition = it.chapterItemPosition,
            )
        }

        viewModel.readerSpeaker.scrollToChapterTop.asLiveData()
            .observe(this) { chapterIndex ->
                scrollToReadingPositionForced(
                    chapterIndex = chapterIndex,
                    chapterItemPosition = 0,
                )
            }

        viewModel.readerSpeaker.startReadingFromFirstVisibleItem.asLiveData().observe(this) {
            viewModel.startSpeaker(
                itemIndex = listState.firstVisibleItemIndex
            )
        }

        // Set current screen to be kept bright always or not
        snapshotFlow { viewModel.state.settings.keepScreenOn.value }
            .asLiveData()
            .observe(this) { keepScreenOn ->
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (keepScreenOn) window.addFlags(flag) else window.clearFlags(flag)
            }

        setContent {
            Theme(themeProvider) {
                SetSystemBarTransparent()

                // Reader info
                ReaderScreen(
                    state = viewModel.state,
                    onTextFontChanged = { appPreferences.READER_FONT_FAMILY.value = it },
                    onTextSizeChanged = { appPreferences.READER_FONT_SIZE.value = it },
                    onSelectableTextChange = { appPreferences.READER_SELECTABLE_TEXT.value = it },
                    onKeepScreenOn = { appPreferences.READER_KEEP_SCREEN_ON.value = it },
                    onFollowSystem = { appPreferences.THEME_FOLLOW_SYSTEM.value = it },
                    onThemeSelected = { appPreferences.THEME_ID.value = it.toPreferenceTheme },
                    onPressBack = {
                        viewModel.onCloseManually()
                        finish()
                    },
                    onOpenChapterInWeb = {
                        val url = viewModel.state.readerInfo.chapterUrl.value
                        if (url.isNotBlank()) {
                            navigationRoutes.webView(this, url = url).let(::startActivity)
                        }
                    },
                    readerContent = {
                        ReaderBookContent(
                            state = listState,
                            items = viewModel.items,
                            bookUrl = viewModel.bookUrl,
//                            modifier = Modifier.padding(it),
                            currentTextSelectability = { appPreferences.READER_SELECTABLE_TEXT.value },
                            currentSpeakerActiveItem = { viewModel.readerSpeaker.currentTextPlaying.value },
                            fontSize = appPreferences.READER_FONT_SIZE.value.sp,
                            onChapterStartVisible = viewModel::markChapterStartAsSeen,
                            onChapterEndVisible = viewModel::markChapterEndAsSeen,
                            onReloadReader = viewModel::reloadReader,
                            onClick = {
                                viewModel.state.showReaderInfo.value =
                                    !viewModel.state.showReaderInfo.value
                            },
                        )
                    },
                )

                if (viewModel.state.showInvalidChapterDialog.value) {
                    BasicAlertDialog(onDismissRequest = {
                        viewModel.state.showInvalidChapterDialog.value = false
                    }) {
                        Text(stringResource(id = R.string.invalid_chapter))
                    }
                }

                LaunchedEffect(Unit) {
                    scrollActions.collectLatest {
                        if (it.animated) {
                            listState.animateScrollToItem(
                                index = it.index,
                                scrollOffset = it.offset,
                            )
                        } else {
                            listState.scrollToItem(
                                index = it.index,
                                scrollOffset = it.offset
                            )
                        }
                    }
                }
            }
        }


        lifecycle.coroutineScope.launch {
            snapshotFlow {
                listState.firstVisibleItemIndex
            }.collect { firstVisibleItemIndex ->
                updateCurrentReadingPosSavingState(firstVisibleItemIndex = firstVisibleItemIndex)
            }
        }

        lifecycle.coroutineScope.launch {
            snapshotFlow {
                listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.lastIndex
            }.collect { lastVisibleItemIndex ->
                viewModel.updateInfoViewTo(lastVisibleItemIndex)
            }
        }

        lifecycle.coroutineScope.launch {
            snapshotFlow {
                listOf(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.lastIndex,
                    listState.layoutInfo.totalItemsCount
                )
            }.collect { (firstVisibleItemIndex, lastVisibleItemIndex, totalItemCount) ->
                updateReadingState(
                    firstVisiblePosition = firstVisibleItemIndex,
                    lastVisiblePosition = lastVisibleItemIndex,
                    totalItemCount = totalItemCount
                )
            }
        }

        when {
            // Use case: user opens app from media control intent
            viewModel.introScrollToSpeaker -> {
                viewModel.introScrollToSpeaker = false
                val itemPos = viewModel.readerSpeaker.currentTextPlaying.value.itemPos
                scrollToReadingPositionImmediately(
                    chapterIndex = itemPos.chapterIndex,
                    chapterItemPosition = itemPos.chapterItemPosition,
                )
            }
            // Use case: user opens reader on the same book, on the same chapter url (session is maintained)
            readerViewHandlersActions.introScrollToCurrentChapter -> {
                readerViewHandlersActions.introScrollToCurrentChapter = false
                val chapterState = viewModel.readingCurrentChapter
                val chapterStats =
                    viewModel.chaptersLoader.chaptersStats[chapterState.chapterUrl] ?: return
                initialScrollToChapterItemPosition(
                    chapterIndex = chapterStats.orderedChaptersIndex,
                    chapterItemPosition = chapterState.chapterItemPosition,
                    offset = chapterState.offset
                )
            }
        }
    }

    private fun setupSystemBar() {
        enableEdgeToEdge()

        // Fullscreen mode that ignores any cutout, notch etc.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.displayCutout())
        controller.hide(WindowInsetsCompat.Type.systemBars())

        snapshotFlow { viewModel.state.showReaderInfo.value }
            .asLiveData()
            .observe(this) { show ->
                if (show) controller.show(WindowInsetsCompat.Type.statusBars())
                else controller.hide(WindowInsetsCompat.Type.statusBars())
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
    }

    private fun scrollToReadingPositionOptional(chapterIndex: Int, chapterItemPosition: Int) {
        // If user already scrolling ignore
        if (listState.isScrollInProgress) {
            return
        }
        // Search for the item being read otherwise do nothing
        for (visibleItem in listState.layoutInfo.visibleItemsInfo) {
            val item = viewModel.items[visibleItem.index]
            if (
                item.chapterIndex == chapterIndex &&
                item is ReaderItem.Position &&
                item.chapterItemPosition == chapterItemPosition
            ) {
                val currentOffsetPx = visibleItem.offset
                val newOffsetPx = -200.dpToPx(this@ReaderActivity)
                // Scroll if item below new scroll position
                if (true) {
//                if (currentOffsetPx > newOffsetPx) {
                    lifecycle.coroutineScope.launch {
                        scrollActions.emit(
                            ScrollToPositionAction(
                                index = visibleItem.index,
                                offset = newOffsetPx,
                                animated = true,
                            )
                        )
                    }
                }
                return
            }
        }
    }

    private fun scrollToReadingPositionForced(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return

        val newOffsetPx = -200.dpToPx(this@ReaderActivity)
        lifecycle.coroutineScope.launch {
            scrollActions.emit(
                ScrollToPositionAction(
                    index = itemIndex,
                    offset = newOffsetPx,
                    animated = true
                )
            )

        }
    }

    private fun scrollToReadingPositionImmediately(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return

        val newOffsetPx = -200.dpToPx(this@ReaderActivity)
        lifecycle.coroutineScope.launch {
            scrollActions.emit(
                ScrollToPositionAction(
                    index = itemIndex,
                    offset = newOffsetPx,
                )
            )
        }
    }

    private fun updateReadingState(
        firstVisiblePosition: Int,
        lastVisiblePosition: Int,
        totalItemCount: Int,
    ) {
        val visibleItemCount = when (totalItemCount) {
            0 -> 0
            else -> lastVisiblePosition - firstVisiblePosition + 1
        }

        val isTop = visibleItemCount != 0 && firstVisiblePosition <= 1
        val isBottom =
            visibleItemCount != 0 && (firstVisiblePosition + visibleItemCount) >= totalItemCount - 1

        when (viewModel.chaptersLoader.readerState) {
            ReaderState.IDLE -> {
                if (isBottom) {
                    viewModel.chaptersLoader.tryLoadNext()
                }
                if (isTop) {
                    viewModel.chaptersLoader.tryLoadPrevious()
                }
            }
            ReaderState.LOADING -> run {}
            ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun initialScrollToChapterItemPosition(
        chapterIndex: Int,
        chapterItemPosition: Int,
        offset: Int
    ) {
        val index = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (index == -1) return

        lifecycle.coroutineScope.launch {
            scrollActions.emit(
                ScrollToPositionAction(
                    index = index,
                    offset = offset
                )
            )
        }
    }

    override fun onPause() {
        updateCurrentReadingPosSavingState(
            firstVisibleItemIndex = listState.firstVisibleItemIndex
        )
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItemIndex: Int) {
        val item = viewModel.items.getOrNull(firstVisibleItemIndex) ?: return
        if (item !is ReaderItem.Position) return

        val offset = listState.firstVisibleItemScrollOffset
        viewModel.readingCurrentChapter = ChapterState(
            chapterUrl = item.chapterUrl,
            chapterItemPosition = item.chapterItemPosition,
            offset = offset
        )
    }
}
