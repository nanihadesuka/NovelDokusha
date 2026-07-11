package my.noveldokusha.features.chapterslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.chapterslist.R
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

    @Inject
    internal lateinit var scraper: Scraper

    private val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Compute whether the "Switch source" option should be shown.
        // It's only shown for TimoTxt sources (where alternative TimoTxt
        // sources exist to switch to).
        val alternatives = scraper.getAlternativeSources(viewModel.bookUrl)
        val showSwitchSource = alternatives.isNotEmpty()

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
                    onSelectedSetReadUpToChapterRead = viewModel::setAsReadUpToSelected,
                    onSelectedSetReadUpToChapterUnread = viewModel::setAsReadUpToUnSelected,
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
                    onGlobalSearchClick = { navigationRoutes.globalSearch(this, text = it).let(::startActivity) },
                    onSwitchSource = { showSwitchSourceDialog(alternatives) },
                    showSwitchSource = showSwitchSource,
                )
            }
        }
    }

    /**
     * Show a dialog listing alternative TimoTxt sources for the current book.
     * When the user picks one, launch a new ChaptersActivity with the
     * converted URL — this "transfers" the novel to the selected source.
     *
     * The book is NOT removed from the original source; the user can switch
     * back at any time. Both sources will show the same novel in their
     * respective catalogs/library.
     */
    private fun showSwitchSourceDialog(
        alternatives: List<Pair<my.noveldokusha.scraper.SourceInterface.Catalog, String>>
    ) {
        if (alternatives.isEmpty()) return

        val labels = alternatives.map { (_, url) ->
            // Derive a human-readable source name from the URL host
            val host = runCatching {
                android.net.Uri.parse(url).host ?: url
            }.getOrDefault(url)
            when {
                host.contains("translate.goog") -> "TimoTxt (Google Translate)"
                host.contains("gemini.goog") -> "TimoTxt (Gemini AI)"
                host.contains("timotxt.com") -> "TimoTxt (Original Chinese)"
                else -> host
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.switch_source_dialog_title)
            .setItems(labels) { _, which ->
                val (_, convertedUrl) = alternatives[which]
                // Launch a new ChaptersActivity with the converted URL.
                // This opens the same novel in the alternative source.
                val bookMetadata = BookMetadata(
                    url = convertedUrl,
                    title = viewModel.state.book.value.title
                )
                ChaptersActivity.IntentData(this, bookMetadata).let(::startActivity)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
