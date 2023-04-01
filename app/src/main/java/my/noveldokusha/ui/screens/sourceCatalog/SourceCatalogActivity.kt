package my.noveldokusha.ui.screens.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.composeViews.AnimatedTransition
import my.noveldokusha.ui.composeViews.BooksVerticalView
import my.noveldokusha.ui.composeViews.TopAppBarSearch
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.webView.WebViewActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.capitalize
import my.noveldokusha.utils.copyToClipboard
import java.util.Locale


@AndroidEntryPoint
class SourceCatalogActivity : BaseActivity() {
    class IntentData : Intent, SourceCatalogStateBundle {
        override var sourceBaseUrl by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, sourceBaseUrl: String) : super(
            ctx,
            SourceCatalogActivity::class.java
        ) {
            this.sourceBaseUrl = sourceBaseUrl
        }
    }

    private val viewModel by viewModels<SourceCatalogViewModel>()

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val focusRequester = remember { FocusRequester() }
            val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
            var optionsExpanded by rememberSaveable { mutableStateOf(false) }

            Theme(appPreferences = appPreferences) {
                Column {
                    AnimatedTransition(targetState = viewModel.toolbarMode) { target ->
                        when (target) {
                            ToolbarMode.MAIN -> ToolbarMain(
                                title = stringResource(R.string.catalog),
                                subtitle = viewModel.source.name.capitalize(Locale.ROOT),
                                onOpenSourceWebPage = ::openSourceWebPage,
                                onPressMoreOptions = { optionsExpanded = !optionsExpanded },
                                onPressSearchForTitle = {
                                    viewModel.toolbarMode = ToolbarMode.SEARCH
                                },
                                optionsDropDownView = {
                                    OptionsDropDown(
                                        expanded = optionsExpanded,
                                        onDismiss = { optionsExpanded = !optionsExpanded },
                                        listLayoutMode = appPreferences.BOOKS_LIST_LAYOUT_MODE.value,
                                        onSelectListLayout = {
                                            appPreferences.BOOKS_LIST_LAYOUT_MODE.value = it
                                        }
                                    )
                                }
                            )
                            ToolbarMode.SEARCH -> TopAppBarSearch(
                                focusRequester = focusRequester,
                                searchText = viewModel.searchText,
                                onClose = {
                                    focusManager.clearFocus()
                                    viewModel.toolbarMode = ToolbarMode.MAIN
                                    viewModel.startCatalogListMode()
                                },
                                onTextDone = { viewModel.startCatalogSearchMode(viewModel.searchText) },
                                placeholderText = stringResource(R.string.search_by_title),
                                onSearchTextChange = { viewModel.searchText = it }
                            )
                        }
                    }
                    BooksVerticalView(
                        layoutMode = viewModel.listLayout,
                        list = viewModel.fetchIterator.list,
                        state = viewModel.listState,
                        error = viewModel.fetchIterator.error,
                        loadState = viewModel.fetchIterator.state,
                        onLoadNext = { viewModel.fetchIterator.fetchNext() },
                        onBookClicked = ::openBookPage,
                        onBookLongClicked = ::addBookToLibrary,
                        onReload = { viewModel.fetchIterator.reloadFailedLastLoad() },
                        onCopyError = ::copyToClipboard
                    )
                }
            }
        }
    }

    fun openSourceWebPage() {
        WebViewActivity.IntentData(this, viewModel.sourceBaseUrl).let(::startActivity)
    }

    fun openBookPage(book: BookMetadata) {
        ChaptersActivity.IntentData(
            this,
            bookMetadata = book
        ).let(::startActivity)
    }

    fun addBookToLibrary(book: BookMetadata) {
        viewModel.addToLibraryToggle(book)
    }
}
