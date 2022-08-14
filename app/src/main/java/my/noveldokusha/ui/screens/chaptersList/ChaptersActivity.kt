package my.noveldokusha.ui.screens.chaptersList

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.rememberResolvedBookImagePath
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.composeViews.ToolbarModeSearch
import my.noveldokusha.ui.screens.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.screens.reader.ReaderActivity
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersActivity : BaseActivity() {
    class IntentData : Intent, ChapterStateBundle {
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(
            ctx,
            ChaptersActivity::class.java
        ) {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    lateinit var scraper: Scraper

    val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
            val focusRequester = remember { FocusRequester() }
            var toolbarMode by rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
            val listState = rememberLazyListState()
            val sourceName = remember(viewModel.book.url) {
                scraper.getCompatibleSource(viewModel.bookUrl)?.name ?: ""
            }

            Theme(appPreferences = appPreferences) {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colors.isLight
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                var isRefreshingDelayed by remember {
                    mutableStateOf(viewModel.isRefreshing)
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { viewModel.isRefreshing }
                        .distinctUntilChanged()
                        .collectLatest {
                            if (it) delay(200)
                            isRefreshingDelayed = it
                        }
                }

                Box {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshingDelayed),
                        onRefresh = {
                            isRefreshingDelayed = true
                            toasty.show(R.string.updating_book_info)
                            viewModel.reloadAll()
                        }
                    ) {
                        ChaptersListView(
                            header = {
                                val image = viewModel.book.coverImageUrl?.let {
                                    rememberResolvedBookImagePath(
                                        bookUrl = viewModel.book.url,
                                        imagePath = it
                                    ).value
                                } ?: R.drawable.ic_baseline_empty_24

                                HeaderView(
                                    bookTitle = viewModel.bookTitle,
                                    sourceName = sourceName,
                                    numberOfChapters = viewModel.chaptersWithContext.size,
                                    bookCover = image,
                                    description = viewModel.book.description,
                                    onSearchBookInDatabase = ::searchBookInDatabase,
                                    onOpenInBrowser = ::openInBrowser,
                                )
                            },
                            list = viewModel.chaptersWithContext,
                            selectedChapters = viewModel.selectedChaptersUrl,
                            error = viewModel.error,
                            listState = listState,
                            onClick = ::onClickChapter,
                            onLongClick = ::onLongClickChapter,
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxHeight()
                            .padding(top = 100.dp, bottom = 0.dp)
                    ) {
                        LazyColumnScrollbar(listState)
                    }

                    when (toolbarMode) {
                        ToolbarMode.MAIN -> {
                            val alpha by remember {
                                derivedStateOf {
                                    if (listState.firstVisibleItemIndex != 0) return@derivedStateOf 1f
                                    val first =
                                        listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                            ?: return@derivedStateOf 0f
                                    val value = (-first.offset - 10).coerceIn(0, 150).toFloat()
                                    val maxvalue = (first.size).coerceIn(1, 150).toFloat()
                                    value / maxvalue
                                }
                            }
                            MainToolbar(
                                bookTitle = viewModel.book.title,
                                isBookmarked = viewModel.book.inLibrary,
                                alpha = alpha,
                                onClickBookmark = ::bookmarkToggle,
                                onClickSortChapters = viewModel::toggleChapterSort,
                                onClickChapterTitleSearch = {
                                    toolbarMode = ToolbarMode.SEARCH
                                },
                                topPadding = 38.dp,
                                height = 56.dp
                            )
                        }
                        ToolbarMode.SEARCH -> ToolbarModeSearch(
                            focusRequester = focusRequester,
                            searchText = viewModel.searchText,
                            onSearchTextChange = { viewModel.searchText = it },
                            onClose = {
                                focusManager.clearFocus()
                                toolbarMode = ToolbarMode.MAIN
                            },
                            onTextDone = {},
                            color = MaterialTheme.colors.primary,
                            showUnderline = true,
                            topPadding = 38.dp,
                            height = 56.dp,
                            placeholderText = stringResource(id = R.string.search_chapter_title)
                        )
                    }

                    FloatingActionButton(
                        onClick = ::onOpenLastActiveChapter,
                        backgroundColor = ColorAccent,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 30.dp, bottom = 100.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
                            contentDescription = stringResource(id = R.string.open_last_read_chapter),
                            tint = Color.White
                        )
                    }

                    AnimatedVisibility(
                        visible = viewModel.selectedChaptersUrl.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 160.dp),
                    ) {
                        BackHandler(onBack = viewModel::closeSelectionMode)
                        SelectionToolsBar(
                            onDeleteDownload = viewModel::deleteDownloadSelected,
                            onDownload = viewModel::downloadSelected,
                            onSetRead = viewModel::setSelectedAsRead,
                            onSetUnread = viewModel::setSelectedAsUnread,
                            onSelectAllChapters = viewModel::selectAll,
                            onSelectAllChaptersAfterSelectedOnes = viewModel::selectAllAfterSelectedOnes,
                            onCloseSelectionbar = viewModel::closeSelectionMode,
                        )
                    }
                }
            }
        }
    }

    fun onClickChapter(chapter: ChapterWithContext) {
        when {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            viewModel.selectedChaptersUrl.isNotEmpty() ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
            else -> openBookAtChapter(chapter.chapter.url)
        }
    }

    fun onLongClickChapter(chapter: ChapterWithContext) {
        when {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            else ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
        }
    }

    fun onOpenLastActiveChapter() {
        val bookUrl = viewModel.bookMetadata.url
        lifecycleScope.launch(Dispatchers.IO) {
            val lastReadChapter = viewModel.getLastReadChapter()
            if (lastReadChapter == null) {
                toasty.show(R.string.no_chapters)
                return@launch
            }

            withContext(Dispatchers.Main) {
                ReaderActivity
                    .IntentData(
                        this@ChaptersActivity,
                        bookUrl = bookUrl,
                        chapterUrl = lastReadChapter
                    )
                    .let(this@ChaptersActivity::startActivity)
            }
        }
    }


    fun bookmarkToggle() {
        lifecycleScope.launch {
            val isBookmarked = viewModel.toggleBookmark()
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            withContext(Dispatchers.Main) {
                toasty.show(msg)
            }
        }
    }

    fun searchBookInDatabase() {
        DatabaseSearchResultsActivity
            .IntentData(
                this,
                "https://www.novelupdates.com/",
                DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookTitle)
            )
            .let(this::startActivity)
    }

    fun openInBrowser() {
        Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse(viewModel.bookMetadata.url)
        }.let(this::startActivity)
    }


    fun openBookAtChapter(chapterUrl: String) {
        ReaderActivity
            .IntentData(
                this,
                bookUrl = viewModel.bookMetadata.url,
                chapterUrl = chapterUrl
            )
            .let(::startActivity)
    }
}
