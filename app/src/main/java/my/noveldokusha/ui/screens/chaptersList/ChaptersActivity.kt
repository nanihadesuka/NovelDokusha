package my.noveldokusha.ui.screens.chaptersList

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.goToDatabaseSearch
import my.noveldokusha.ui.goToReader
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
                    onPullRefresh = viewModel::onPullRefresh
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
