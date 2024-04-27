package my.noveldokusha.features.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.AbsListView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.composableActions.SetSystemBarTransparent
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.features.main.settings.SettingsViewModel
import my.noveldokusha.features.reader.tools.FontsLoader
import my.noveldokusha.features.reader.tools.indexOfReaderItem
import my.noveldokusha.tools.Utterance
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.goToWebViewWithUrl
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_Boolean
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.colorAttrRes
import my.noveldokusha.utils.dpToPx
import my.noveldokusha.utils.fadeIn

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

    private var listIsScrolling = false
    private val fadeInTextLiveData = MutableLiveData<Boolean>(false)

    private val viewModel by viewModels<ReaderViewModel>()
    private val viewModelGlobalSettings by viewModels<SettingsViewModel>()

    private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }
    private val viewAdapter = object {
        val listView by lazy {
            ReaderItemAdapter(
                this@ReaderActivity,
                viewModel.items,
                viewModel.bookUrl,
                currentTextSelectability = { appPreferences.READER_SELECTABLE_TEXT.value },
                currentFontSize = { appPreferences.READER_FONT_SIZE.value },
                currentTypeface = { fontsLoader.getTypeFaceNORMAL(appPreferences.READER_FONT_FAMILY.value) },
                currentTypefaceBold = { fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value) },
                currentSpeakerActiveItem = { viewModel.readerSpeaker.currentTextPlaying.value },
                onChapterStartVisible = viewModel::markChapterStartAsSeen,
                onChapterEndVisible = viewModel::markChapterEndAsSeen,
                onReloadReader = viewModel::reloadReader,
                onClick = {
                    viewModel.state.showReaderInfo.value = !viewModel.state.showReaderInfo.value
                },
            )
        }
    }

    private val fontsLoader = FontsLoader()

    override fun onBackPressed() {
        viewModel.onCloseManually()
        super.onBackPressed()
    }

    override fun onDestroy() {
        viewModel.onViewDestroyed()
        super.onDestroy()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBind.listView.adapter = viewAdapter.listView

        fadeInTextLiveData.distinctUntilChanged().observe(this) {
            if (it) {
                viewBind.listView.fadeIn(durationMillis = 150)
            }
        }

        lifecycleScope.launch {
            viewModel.onTranslatorChanged.collect {
                viewModel.reloadReader()
            }
        }
        viewModel.forceUpdateListViewState = {
            withContext(Dispatchers.Main.immediate) {
                viewAdapter.listView.notifyDataSetChanged()
            }
        }

        viewModel.maintainStartPosition = {
            withContext(Dispatchers.Main.immediate) {
                it()
                val titleIndex = (0..viewAdapter.listView.count)
                    .indexOfFirst { viewAdapter.listView.getItem(it) is ReaderItem.Title }

                if (titleIndex != -1) {
                    viewBind.listView.setSelection(titleIndex)
                }
            }
        }

        viewModel.setInitialPosition = {
            withContext(Dispatchers.Main.immediate) {
                initialScrollToChapterItemPosition(
                    chapterIndex = it.chapterIndex,
                    chapterItemPosition = it.chapterItemPosition,
                    offset = it.chapterItemOffset
                )
            }
        }

        viewModel.maintainLastVisiblePosition = {
            withContext(Dispatchers.Main.immediate) {
                val oldSize = viewAdapter.listView.count
                val position = viewBind.listView.lastVisiblePosition
                val positionView = position - viewBind.listView.firstVisiblePosition
                val top = viewBind.listView.getChildAt(positionView).run { top - paddingTop }
                it()
                val displacement = viewAdapter.listView.count - oldSize
                viewBind.listView.setSelectionFromTop(position + displacement, top)
            }
        }

        viewModel.ttsScrolledToTheTop.asLiveData().observe(this) {
            viewAdapter.listView.notifyDataSetChanged()
            if (viewAdapter.listView.count < 1) {
                return@observe
            }
            viewBind.listView.smoothScrollToPositionFromTop(
                1,
                300.dpToPx(this),
                250
            )
        }

        viewModel.ttsScrolledToTheBottom.asLiveData().observe(this) {
            viewAdapter.listView.notifyDataSetChanged()
            if (viewAdapter.listView.count < 2) {
                return@observe
            }
            viewBind.listView.smoothScrollToPositionFromTop(
                viewAdapter.listView.count - 2,
                300.dpToPx(this),
                250
            )
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
            val firstPosition = viewBind.listView.firstVisiblePosition
            viewModel.startSpeaker(
                itemIndex = viewAdapter.listView.getFirstVisibleItemIndexGivenPosition(firstPosition)
            )
        }

        // Notify manually text font changed for list view
        snapshotFlow { viewModel.state.settings.style.textFont.value }.drop(1)
            .asLiveData()
            .observe(this) { viewAdapter.listView.notifyDataSetChanged() }

        // Notify manually text size changed for list view
        snapshotFlow { viewModel.state.settings.style.textSize.value }.drop(1)
            .asLiveData()
            .observe(this) { viewAdapter.listView.notifyDataSetChanged() }

        // Notify manually selectable text changed for list view
        snapshotFlow { viewModel.state.settings.isTextSelectable.value }.drop(1)
            .asLiveData()
            .observe(this) { viewAdapter.listView.notifyDataSetChanged() }

        // Set current screen to be kept bright always or not
        snapshotFlow { viewModel.state.settings.keepScreenOn.value }
            .asLiveData()
            .observe(this) { keepScreenOn ->
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (keepScreenOn) window.addFlags(flag) else window.clearFlags(flag)
            }

        setContent {
            Theme(appPreferences) {
                SetSystemBarTransparent()

                // Reader info
                ReaderScreen(
                    state = viewModel.state,
                    onTextFontChanged = { appPreferences.READER_FONT_FAMILY.value = it },
                    onTextSizeChanged = { appPreferences.READER_FONT_SIZE.value = it },
                    onSelectableTextChange = { appPreferences.READER_SELECTABLE_TEXT.value = it },
                    onKeepScreenOn = { appPreferences.READER_KEEP_SCREEN_ON.value = it },
                    onFollowSystem = viewModelGlobalSettings::onFollowSystemChange,
                    onThemeSelected = viewModelGlobalSettings::onThemeChange,
                    onPressBack = {
                        viewModel.onCloseManually()
                        finish()
                    },
                    onOpenChapterInWeb = {
                        val url = viewModel.state.readerInfo.chapterUrl.value
                        if (url.isNotBlank()) {
                            goToWebViewWithUrl(url)
                        }
                    },
                    readerContent = {
                        AndroidView(factory = { viewBind.root })
                    },
                )

                if (viewModel.state.showInvalidChapterDialog.value) {
                    BasicAlertDialog(onDismissRequest = {
                        viewModel.state.showInvalidChapterDialog.value = false
                    }
                    ) {
                        Text(stringResource(id = R.string.invalid_chapter))
                    }
                }
            }
        }

        viewBind.listView.setOnItemClickListener { _, _, _, _ ->
            viewModel.state.showReaderInfo.value = !viewModel.state.showReaderInfo.value
        }

        viewBind.listView.setOnScrollListener(
            object : AbsListView.OnScrollListener {
                override fun onScroll(
                    view: AbsListView?,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    updateCurrentReadingPosSavingState(
                        firstVisibleItemIndex = viewAdapter.listView.fromPositionToIndex(
                            viewBind.listView.firstVisiblePosition
                        )
                    )
                    updateInfoView()
                    updateReadingState()
                }

                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                    listIsScrolling = scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                }
            })

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
        viewAdapter.listView.notifyDataSetChanged()
        lifecycleScope.launch {
            delay(200)
            fadeInTextLiveData.postValue(true)
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
            viewModel.introScrollToCurrentChapter -> {
                viewModel.introScrollToCurrentChapter = false
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

    private fun scrollToReadingPositionOptional(chapterIndex: Int, chapterItemPosition: Int) {
        // If user already scrolling ignore
        if (listIsScrolling) {
            viewAdapter.listView.notifyDataSetChanged()
            return
        }
        // Search for the item being read otherwise do nothing
        val firstIndex = viewBind.listView.firstVisiblePosition
        val lastIndex = viewBind.listView.lastVisiblePosition
        for (index in firstIndex..lastIndex) {
            val item = viewAdapter.listView.getItem(index)
            if (
                item.chapterIndex == chapterIndex &&
                item is ReaderItem.Position &&
                item.chapterItemPosition == chapterItemPosition
            ) {
                val viewIndex = index - viewBind.listView.firstVisiblePosition
                val currentOffsetPx =
                    viewBind.listView.getChildAt(viewIndex).run { top - paddingTop }
                val newOffsetPx = 200.dpToPx(this@ReaderActivity)
                viewAdapter.listView.notifyDataSetChanged()
                // Scroll if item below new scroll position
                if (currentOffsetPx > newOffsetPx) {
                    viewBind.listView.smoothScrollToPositionFromTop(index, newOffsetPx, 1000)
                }
                return
            }
        }
        viewAdapter.listView.notifyDataSetChanged()
    }

    private fun scrollToReadingPositionForced(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return
        val itemPosition = viewAdapter.listView.fromIndexToPosition(itemIndex)
        val newOffsetPx = 200.dpToPx(this@ReaderActivity)
        viewAdapter.listView.notifyDataSetChanged()
        viewBind.listView.smoothScrollToPositionFromTop(itemPosition, newOffsetPx, 500)
        viewAdapter.listView.notifyDataSetChanged()
    }

    private fun scrollToReadingPositionImmediately(chapterIndex: Int, chapterItemPosition: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemPosition = chapterItemPosition
        )
        if (itemIndex == -1) return
        val itemPosition = viewAdapter.listView.fromIndexToPosition(itemIndex)
        val newOffsetPx = 200.dpToPx(this@ReaderActivity)
        viewAdapter.listView.notifyDataSetChanged()
        viewBind.listView.setSelectionFromTop(itemPosition, newOffsetPx)
        viewAdapter.listView.notifyDataSetChanged()
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
        val position = viewAdapter.listView.fromIndexToPosition(index)
        if (index != -1) {
            viewBind.listView.setSelectionFromTop(position, offset)
        }
        fadeInTextLiveData.postValue(true)
        viewBind.listView.doOnNextLayout { updateReadingState() }
    }

    fun updateInfoView() {
        val lastVisiblePosition = viewBind.listView.lastVisiblePosition
        val itemIndex = viewAdapter.listView.fromPositionToIndex(lastVisiblePosition)
        viewModel.updateInfoViewTo(itemIndex)
    }

    override fun onPause() {
        updateCurrentReadingPosSavingState(
            firstVisibleItemIndex = viewAdapter.listView.fromPositionToIndex(
                viewBind.listView.firstVisiblePosition
            )
        )
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItemIndex: Int) {
        val item = viewModel.items.getOrNull(firstVisibleItemIndex) ?: return
        if (item !is ReaderItem.Position) return

        val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
        viewModel.readingCurrentChapter = ChapterState(
            chapterUrl = item.chapterUrl,
            chapterItemPosition = item.chapterItemPosition,
            offset = offset
        )
    }
}
