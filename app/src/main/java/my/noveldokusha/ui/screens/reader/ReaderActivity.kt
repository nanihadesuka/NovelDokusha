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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityReaderBinding
import my.noveldokusha.tools.TextSynthesis
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.screens.main.settings.SettingsViewModel
import my.noveldokusha.ui.screens.reader.tools.FontsLoader
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
        viewBind.listView.isEnabled = false
        val currentChapter = viewModel.currentChapter.copy()
        lifecycleScope.coroutineContext.cancelChildren()
        viewModel.reloadReader()
        viewModel.chaptersLoader.loadRestartedInitialChapter(currentChapter)
    }

    override fun onBackPressed() {
        viewModel.readerSpeaker.stop()
        super.onBackPressed()
    }

    override fun onDestroy() {
        viewModel.onTranslatorChanged = null
        viewModel.forceUpdateListViewState = null
        viewModel.maintainStartPosition = null
        viewModel.maintainLastVisiblePosition = null
        viewModel.showInvalidChapterDialog = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)

        viewBind.listView.adapter = viewAdapter.listView

        viewModel.onTranslatorChanged = { reloadReader() }
        viewModel.forceUpdateListViewState = {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                viewAdapter.listView.notifyDataSetChanged()
            }
        }
        viewModel.showInvalidChapterDialog = {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                MaterialDialog(this@ReaderActivity).show {
                    title(text = getString(R.string.invalid_chapter))
                    cornerRadius(16f)
                }
            }
        }
        viewModel.maintainStartPosition = {
            it()
            val titleIndex = (0..viewAdapter.listView.count)
                .asSequence()
                .map { viewAdapter.listView.getItem(it) }
                .indexOfFirst { it is ReaderItem.Title }

            if (titleIndex != -1) {
                viewBind.listView.setSelection(titleIndex)
            }
        }
        viewModel.maintainLastVisiblePosition = {
            val oldSize = viewAdapter.listView.count
            val index = viewBind.listView.lastVisiblePosition
            val indexView = index - viewBind.listView.firstVisiblePosition
            val top = viewBind.listView.getChildAt(indexView).run { top - paddingTop }
            it()
            val displacement = viewAdapter.listView.count - oldSize
            viewBind.listView.setSelectionFromTop(index + displacement, top)
        }

        viewBind.listView.isEnabled = false

        viewModel.chaptersLoader.initialChapterLoadedFlow.asLiveData().observe(this) {
            if (!viewModel.readerSpeaker.settings.isEnabled.value) {
                return@observe
            }
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                when (it) {
                    is ChaptersLoader.InitialLoadCompleted.Initial -> {
                        val initialState = viewModel.getChapterInitialPosition()
                        if (initialState != null) {
                            val (position: Int, offset: Int) = initialState
                            setInitialChapterPosition(position = position, offset = offset)
                        }
                    }
                    is ChaptersLoader.InitialLoadCompleted.Reloaded -> {
                        setInitialChapterPosition(
                            position = it.chapterState.chapterItemIndex,
                            offset = it.chapterState.offset
                        )
                    }
                }
                viewBind.listView.isEnabled = true
            }
        }

        viewModel.readerSpeaker.currentTextLiveData.observe(this) {
            scrollToReadingPosition(it)
        }

        viewModel.readerSpeaker.startReadingFromFirstVisibleItemFlow.asLiveData().observe(this) {
            viewBind.listView.doOnNextLayout { startSpeaking() }
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
                    updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
                    updateInfoView()
                    updateReadingState()
                }

                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                    listIsScrolling = scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = R.attr.colorSurface.colorAttrRes(this)
        viewAdapter.listView.notifyDataSetChanged()
    }

    private fun scrollToReadingPosition(textSynthesis: TextSynthesis) {
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
                item.chapterIndex == textSynthesis.chapterIndex &&
                item is ReaderItem.Position &&
                item.chapterItemIndex == textSynthesis.chapterItemIndex
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
            ReaderViewModel.ReaderState.IDLE -> when {
                isBottom && viewModel.chaptersLoader.loadNextChapter() -> run {}
                isTop && viewModel.chaptersLoader.loadPreviousChapter() -> run {}
            }
            ReaderViewModel.ReaderState.LOADING -> run {}
            ReaderViewModel.ReaderState.INITIAL_LOAD -> run {}
        }
    }

    private fun startSpeaking() {
        val index = viewAdapter.listView.fromPositionToIndex(viewBind.listView.firstVisiblePosition)
        viewModel.startSpeaker(itemIndex = index)
    }

    private fun setInitialChapterPosition(position: Int, offset: Int) {
        val index = viewModel.items.indexOfFirst {
            it is ReaderItem.Position && it.chapterItemIndex == position
        }
        if (index != -1) {
            viewBind.listView.setSelectionFromTop(index, offset)
        }
        viewModel.chaptersLoader.readerState = ReaderViewModel.ReaderState.IDLE
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
        updateCurrentReadingPosSavingState(viewBind.listView.firstVisiblePosition)
        super.onPause()
    }

    private fun updateCurrentReadingPosSavingState(firstVisibleItem: Int) {
        val item = viewAdapter.listView.getItem(firstVisibleItem)
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
