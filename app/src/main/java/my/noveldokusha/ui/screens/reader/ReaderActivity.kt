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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.drop
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.screens.main.settings.SettingsViewModel
import my.noveldokusha.ui.screens.reader.tools.FontsLoader
import my.noveldokusha.ui.screens.reader.tools.indexOfReaderItem
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.colorAttrRes
import my.noveldokusha.utils.dpToPx
import my.noveldokusha.utils.fadeIn
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class ReaderActivity : BaseActivity() {
    class IntentData : Intent, ReaderStateBundle {
        override var bookUrl by Extra_String()
        override var chapterUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookUrl: String, chapterUrl: String) : super(
            ctx,
            ReaderActivity::class.java
        ) {
            this.bookUrl = bookUrl
            this.chapterUrl = chapterUrl
        }
    }

    private var listIsScrolling = false
    private val fadeInTextAlready = AtomicBoolean(false)
    private fun fadeInText() {
        if (fadeInTextAlready.compareAndSet(false, true))
            viewBind.listView.fadeIn()
    }

    private val viewModel by viewModels<ReaderViewModel>()
    private val viewModelGlobalSettings by viewModels<SettingsViewModel>()

    private val viewBind by lazy { ActivityReaderBinding.inflate(layoutInflater) }
    private val viewAdapter = object {
        val listView by lazy {
            ReaderItemAdapter(
                this@ReaderActivity,
                viewModel.items,
                viewModel.bookUrl,
                fontsLoader,
                appPreferences,
                viewModel.readerSpeaker,
                onChapterStartVisible = { url -> viewModel.readRoutine.setReadStart(url) },
                onChapterEndVisible = { url -> viewModel.readRoutine.setReadEnd(url) },
                onReloadReader = ::reloadReader,
                onClick = { viewModel.showReaderInfoView = !viewModel.showReaderInfoView },
            )
        }
    }

    private val fontsLoader = FontsLoader()

    private fun reloadReader() {
        viewModel.listIsEnabled.postValue(false)
        val currentChapter = viewModel.currentChapter.copy()
        lifecycleScope.coroutineContext.cancelChildren()
        viewModel.reloadReader()
        viewModel.chaptersLoader.loadRestartedInitial(currentChapter)
    }

    override fun onBackPressed() {
        viewModel.onClose()
        super.onBackPressed()
    }

    override fun onDestroy() {
        viewModel.onTranslatorChanged = null
        viewModel.forceUpdateListViewState = null
        viewModel.maintainStartPosition = null
        viewModel.maintainLastVisiblePosition = null
        viewModel.setInitialPosition = null
        viewModel.showInvalidChapterDialog = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)

        viewBind.listView.adapter = viewAdapter.listView

        viewModel.listIsEnabled.observe(this) {
            viewBind.listView.isEnabled = it
        }

        viewModel.onTranslatorChanged = {
            reloadReader()
        }

        viewModel.forceUpdateListViewState = {
            withContext(Dispatchers.Main.immediate) {
                viewAdapter.listView.notifyDataSetChanged()
            }
        }

        viewModel.showInvalidChapterDialog = {
            withContext(Dispatchers.Main.immediate) {
                MaterialDialog(this@ReaderActivity).show {
                    title(text = getString(R.string.invalid_chapter))
                    cornerRadius(16f)
                }
            }
        }

        viewModel.maintainStartPosition = {
            withContext(Dispatchers.Main.immediate) {
                it()
                val titleIndex = (0..viewAdapter.listView.count)
                    .asSequence()
                    .map { viewAdapter.listView.getItem(it) }
                    .indexOfFirst { it is ReaderItem.Title }

                if (titleIndex != -1) {
                    viewBind.listView.setSelection(titleIndex)
                }
            }
        }

        viewModel.setInitialPosition = {
            withContext(Dispatchers.Main.immediate) {
                setInitialChapterPosition(
                    chapterIndex = it.chapterIndex,
                    chapterItemIndex = it.chapterItemIndex,
                    offset = it.itemOffset
                )
                viewModel.listIsEnabled.postValue(true)
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

        viewModel.scrollToTheTop.asLiveData().observe(this) {
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

        viewModel.scrollToTheBottom.asLiveData().observe(this) {
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

        viewModel.readerSpeaker.currentReaderItem.observe(this) {
            scrollToReadingPositionOptional(
                chapterIndex = it.chapterIndex,
                chapterItemIndex = it.chapterItemIndex,
            )
        }

        viewModel.readerSpeaker.scrollToReaderItem.asLiveData().observe(this) {
            if (it !is ReaderItem.Position) return@observe
            scrollToReadingPositionForced(
                chapterIndex = it.chapterIndex,
                chapterItemIndex = it.chapterItemIndex,
            )
        }

        viewModel.readerSpeaker.scrollToFirstChapterItemIndex.asLiveData()
            .observe(this) { chapterIndex ->
                scrollToReadingPositionForced(
                    chapterIndex = chapterIndex,
                    chapterItemIndex = 0,
                )
            }

        viewModel.readerSpeaker.startReadingFromFirstVisibleItem.asLiveData().observe(this) {
            val firstPosition = viewBind.listView.firstVisiblePosition
            viewModel.startSpeaker(
                itemIndex = viewAdapter.listView.fromPositionToIndex(firstPosition)
            )
        }

        viewBind.settings.setContent {
            Theme(
                appPreferences = appPreferences,
                wrapper = {
                    // Necessary so that text knows what color it must be given that the
                    // background is transparent (no Surface parent)
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colors.onSecondary
                    ) { it() }
                }
            ) {
                // Notify manually text font changed for list view
                LaunchedEffect(true) {
                    snapshotFlow { viewModel.textFont }.drop(1)
                        .collect {
                            viewAdapter.listView.notifyDataSetChanged()
                        }
                }

                // Notify manually text size changed for list view
                LaunchedEffect(true) {
                    snapshotFlow { viewModel.textSize }.drop(1)
                        .collect {
                            viewAdapter.listView.notifyDataSetChanged()
                        }
                }

                // Notify manually selectable text changed for list view
                LaunchedEffect(true) {
                    snapshotFlow { viewModel.isTextSelectable }.drop(1)
                        .collect {
                            viewAdapter.listView.notifyDataSetChanged()
                        }
                }

                // Capture back action when viewing info
                BackHandler(enabled = viewModel.showReaderInfoView) {
                    viewModel.showReaderInfoView = false
                }

                // Reader info
                ReaderInfoView(
                    chapterTitle = viewModel.readingPosStats?.run { first.chapter.title } ?: "",
                    chapterCurrentNumber = viewModel.readingPosStats?.run { first.chapterIndex + 1 }
                        ?: 0,
                    chapterPercentageProgress = viewModel.chapterPercentageProgress,
                    chaptersTotalSize = viewModel.orderedChapters.size,
                    textFont = viewModel.textFont,
                    textSize = viewModel.textSize,
                    visible = viewModel.showReaderInfoView,
                    onTextFontChanged = { appPreferences.READER_FONT_FAMILY.value = it },
                    onTextSizeChanged = { appPreferences.READER_FONT_SIZE.value = it },
                    liveTranslationSettingData = viewModel.liveTranslationSettingState,
                    textToSpeechSettingData = viewModel.textToSpeechSettingData,
                    currentFollowSystem = viewModelGlobalSettings.followsSystem,
                    currentTheme = viewModelGlobalSettings.theme,
                    selectableText = viewModel.isTextSelectable,
                    onSelectableTextChange = {
                        appPreferences.READER_SELECTABLE_TEXT.value = it
                    },
                    onFollowSystem = viewModelGlobalSettings::onFollowSystem,
                    onThemeSelected = viewModelGlobalSettings::onThemeSelected,
                )
            }
        }

        viewBind.listView.setOnItemClickListener { _, _, _, _ ->
            viewModel.showReaderInfoView = !viewModel.showReaderInfoView
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
                        viewBind.listView.firstVisiblePosition
                    )
                    updateInfoView()
                    updateReadingState()
                }

                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                    listIsScrolling = scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                }
            })

        // Show reader if text hasn't loaded after 200 ms of waiting
        lifecycleScope.launch(Dispatchers.Main.immediate)
        {
            delay(200)
            viewAdapter.listView.notifyDataSetChanged()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        viewAdapter.listView.notifyDataSetChanged()
    }

    private fun scrollToReadingPositionOptional(chapterIndex: Int, chapterItemIndex: Int) {
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
                item.chapterItemIndex == chapterItemIndex
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

    private fun scrollToReadingPositionForced(chapterIndex: Int, chapterItemIndex: Int) {
        // Search for the item being read otherwise do nothing
        val itemIndex = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemIndex = chapterItemIndex
        )
        if (itemIndex == -1) return
        val itemPosition = viewAdapter.listView.fromIndexToPosition(itemIndex)
        val newOffsetPx = 200.dpToPx(this@ReaderActivity)
        viewAdapter.listView.notifyDataSetChanged()
        viewBind.listView.smoothScrollToPositionFromTop(itemPosition, newOffsetPx, 500)
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
            ReaderViewModel.ReaderState.IDLE -> {
                if (isBottom) {
                    viewModel.chaptersLoader.loadNext()
                }
                if (isTop) {
                    viewModel.chaptersLoader.loadPrevious()
                }
            }
            ReaderViewModel.ReaderState.LOADING -> run {}
            ReaderViewModel.ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun setInitialChapterPosition(
        chapterIndex: Int,
        chapterItemIndex: Int,
        offset: Int
    ) {
        val index = indexOfReaderItem(
            list = viewModel.items,
            chapterIndex = chapterIndex,
            chapterItemIndex = chapterItemIndex
        )
        val position = viewAdapter.listView.fromIndexToPosition(index)
        if (index != -1) {
            viewBind.listView.setSelectionFromTop(position, offset)
        }
        fadeInText()
        viewBind.listView.doOnNextLayout { updateReadingState() }
    }

    fun updateInfoView() {
        val lastVisiblePosition = viewBind.listView.lastVisiblePosition
        if (lastVisiblePosition < 0) return
        val item = viewAdapter.listView.getItem(lastVisiblePosition)
        if (item !is ReaderItem.Position) return

        viewModel.updateInfoViewTo(item.chapterUrl, item.chapterItemIndex)
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
        if (firstVisibleItemIndex == -1 || firstVisibleItemIndex > viewModel.items.lastIndex) return
        val item = viewModel.items[firstVisibleItemIndex]
        if (item is ReaderItem.Position) {
            val offset = viewBind.listView.run { getChildAt(0).top - paddingTop }
            viewModel.currentChapter = ReaderViewModel.ChapterState(
                chapterUrl = item.chapterUrl,
                chapterItemIndex = item.chapterItemIndex,
                offset = offset
            )
        }
    }
}
