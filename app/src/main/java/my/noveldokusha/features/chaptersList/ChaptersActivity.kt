package my.noveldokusha.features.chaptersList

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.noveldokusha.composableActions.SetSystemBarTransparent
import my.noveldokusha.composableActions.onDoAskForImage
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.goToDatabaseSearch
import my.noveldokusha.ui.goToReader
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String

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

    val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme(appPreferences = appPreferences) {
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
                    onCoverLongClick = { goToDatabaseSearch(input = viewModel.bookTitle) },
                    onChangeCover = onDoAskForImage { viewModel.saveImageAsCover(it) }
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

    private fun searchBookInDatabase() = goToDatabaseSearch(
        input = viewModel.state.book.value.title
    )

    private fun openBookAtChapter(chapterUrl: String) = goToReader(
        bookUrl = viewModel.state.book.value.url, chapterUrl = chapterUrl
    )
}
