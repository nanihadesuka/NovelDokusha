package my.noveldokusha.ui.screens.chaptersList

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.goToDatabaseSearch
import my.noveldokusha.ui.goToReader
import my.noveldokusha.ui.goToWebBrowser
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.ui.theme.isLight
import my.noveldokusha.utils.Extra_String

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

    val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {

            Theme(appPreferences = appPreferences) {
                val context by rememberUpdatedState(LocalContext.current)
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colorScheme.isLight()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }
                ChaptersScreen(
                    state = viewModel.state,
                    onSearchBookInDatabase = ::searchBookInDatabase,
                    onPressBack = ::onBackPressed,
                    onLibraryToggle = viewModel::toggleBookmark,
                    onResumeReading = ::onOpenLastActiveChapter,
                    onSelectionModeChapterClick = viewModel::onSelectionModeChapterClick,
                    onSelectionModeChapterLongClick = viewModel::onSelectionModeChapterLongClick,
                    onChapterClick = { openBookAtChapter(chapterUrl = it.chapter.url) },
                    onChapterLongClick = viewModel::onChapterLongClick,
                    onChapterDownload = viewModel::onChapterDownload,
                    onSelectedDownload = viewModel::downloadSelected,
                    onSelectedDeleteDownloads = viewModel::deleteDownloadsSelected,
                    onSelectedSetRead = viewModel::setAsReadSelected,
                    onSelectedSetUnread = viewModel::setAsUnreadSelected,
                    onCloseSelectionBar = viewModel::unselectAll,
                    onSelectAllChapters = viewModel::selectAll,
                    onSelectedInvertSelection = viewModel::invertSelection,
                )

//                var isRefreshingDelayed by remember {
//                    mutableStateOf(viewModel.isRefreshing)
//                }
//
//                LaunchedEffect(Unit) {
//                    snapshotFlow { viewModel.isRefreshing }
//                        .distinctUntilChanged()
//                        .collectLatest {
//                            if (it) delay(200)
//                            isRefreshingDelayed = it
//                        }
//                }
//
//                Box {
//                    val pullRefreshState = rememberPullRefreshState(
//                        refreshing = isRefreshingDelayed,
//                        onRefresh = {
//                            toasty.show(R.string.updating_book_info)
//                            viewModel.reloadAll()
//                        }
//                    )
//
//
//                        PullRefreshIndicator(
//                            refreshing = isRefreshingDelayed,
//                            state = pullRefreshState,
//                            modifier = Modifier.align(Alignment.TopCenter)
//                        )
//                    }
//
//                    Box(
//                        Modifier
//                            .fillMaxHeight()
//                            .padding(top = 100.dp, bottom = 0.dp)
//                    ) {
//                        InternalLazyColumnScrollbar(listState)
//                    }
//
//                    FloatingActionButton(
//                        onClick = ::onOpenLastActiveChapter,
//                        backgroundColor = ColorAccent,
//                        modifier = Modifier
//                            .align(Alignment.BottomEnd)
//                            .padding(end = 30.dp, bottom = 100.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
//                            contentDescription = stringResource(id = R.string.open_last_read_chapter),
//                            tint = Color.White
//                        )
//                    }
//
//                    AnimatedVisibility(
//                        visible = viewModel.selectedChaptersUrl.isNotEmpty(),
//                        enter = fadeIn() + slideInVertically { it },
//                        exit = fadeOut() + slideOutVertically { it },
//                        modifier = Modifier
//                            .align(Alignment.BottomCenter)
//                            .padding(bottom = 160.dp),
//                    ) {
//                        BackHandler(onBack = viewModel::closeSelectionMode)
//                        SelectionToolsBar(
//                            onDeleteDownload = viewModel::deleteDownloadSelected,
//                            onDownload = viewModel::downloadSelected,
//                            onSetRead = viewModel::setSelectedAsRead,
//                            onSetUnread = viewModel::setSelectedAsUnread,
//                            onSelectAllChapters = viewModel::selectAll,
//                            onSelectAllChaptersAfterSelectedOnes = viewModel::selectAllAfterSelectedOnes,
//                            onCloseSelectionbar = viewModel::closeSelectionMode,
//                        )
//                    }
//                }
            }
        }
    }

    private fun onOpenLastActiveChapter() {
        lifecycleScope.launch {
            val lastReadChapter = viewModel.getLastReadChapter() ?: return@launch
            openBookAtChapter(chapterUrl = lastReadChapter)
        }
    }

    private fun searchBookInDatabase() = goToDatabaseSearch(
        input = viewModel.state.book.value.title
    )

    private fun openInBrowser() = goToWebBrowser(
        url = viewModel.state.book.value.url
    )

    private fun openBookAtChapter(chapterUrl: String) = goToReader(
        bookUrl = viewModel.state.book.value.url, chapterUrl = chapterUrl
    )
}
