package my.noveldokusha.features.chapterslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.BaseActivity
import my.noveldoksuha.coreui.composableActions.SetSystemBarTransparent
import my.noveldoksuha.coreui.composableActions.onDoAskForImage
import my.noveldoksuha.coreui.theme.Theme
import my.noveldokusha.core.utils.Extra_String
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.tooling.local_database.BookMetadata
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersActivity : BaseActivity() {
    class IntentData : Intent, ChapterStateBundle {
        override var rawBookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(
            ctx,
            ChaptersActivity::class.java
        ) {
            this.rawBookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    internal lateinit var navigationRoutes: NavigationRoutes

    private val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme(themeProvider = themeProvider) {
                SetSystemBarTransparent()
                ChaptersScreen(
                    state = viewModel.state,
                    onLibraryToggle = viewModel::toggleBookmark,
                    onSearchBookInDatabase = ::searchBookInDatabase,
                    onResumeReading = ::onOpenLastActiveChapter,
                    onPressBack = ::onBackPressed,
                    onSelectedDeleteDownloads = viewModel::deleteDownloadsSelected,
                    onSelectedDownload = viewModel::downloadSelected,
                    onSelectedSetRead = viewModel::setAsReadSelected,
                    onSelectedSetUnread = viewModel::setAsUnreadSelected,
                    onSelectedInvertSelection = viewModel::invertSelection,
                    onSelectAllChapters = viewModel::selectAll,
                    onCloseSelectionBar = viewModel::unselectAll,
                    onChapterClick = { openBookAtChapter(chapterUrl = it.chapter.url) },
                    onChapterLongClick = viewModel::onChapterLongClick,
                    onSelectionModeChapterClick = viewModel::onSelectionModeChapterClick,
                    onSelectionModeChapterLongClick = viewModel::onSelectionModeChapterLongClick,
                    onChapterDownload = viewModel::onChapterDownload,
                    onPullRefresh = viewModel::onPullRefresh,
                    onCoverLongClick = { searchBookInDatabase(input = viewModel.bookTitle) },
                    onChangeCover = onDoAskForImage { viewModel.saveImageAsCover(it) },
                    onOpenInBrowser = { navigationRoutes.webView(this, url = it).let(::startActivity) },
                    onGlobalSearchClick = { navigationRoutes.globalSearch(this, text = it).let(::startActivity) }
                )
            }
        }
    }

    private fun onOpenLastActiveChapter() {
        lifecycleScope.launch {
            val lastReadChapter = viewModel.getLastReadChapter()
                ?: viewModel.state.chapters.minByOrNull { it.chapter.position }?.chapter?.url
                ?: return@launch

            openBookAtChapter(chapterUrl = lastReadChapter)
        }
    }

    private fun searchBookInDatabase(
        input: String = viewModel.state.book.value.title
    ) = navigationRoutes.databaseSearch(
        this,
        input = input
    ).let(::startActivity)

    private fun openBookAtChapter(chapterUrl: String) = navigationRoutes.reader(
        this, bookUrl = viewModel.state.book.value.url, chapterUrl = chapterUrl
    ).let(::startActivity)
}
