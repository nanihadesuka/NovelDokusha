package my.noveldokusha.ui.screens.sourceCatalog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.webView.WebViewActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.ui.composeViews.ToolbarModeSearch
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.copyToClipboard
import my.noveldokusha.uiViews.AnimatedTransition
import my.noveldokusha.uiViews.BooksVerticalView
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SourceCatalogActivity : ComponentActivity() {
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

    @Inject
    lateinit var appPreferences: AppPreferences

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val searchText = rememberSaveable { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
            var toolbarMode by rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
            val state = rememberLazyListState()
            var optionsExpanded by remember { mutableStateOf(false) }

            Theme(appPreferences = appPreferences) {
                Column {
                    AnimatedTransition(targetState = toolbarMode) { target ->
                        when (target) {
                            ToolbarMode.MAIN -> ToolbarMain(
                                title = stringResource(R.string.catalog),
                                subtitle = viewModel.source.name.capitalize(Locale.ROOT),
                                onOpenSourceWebPage = ::openSourceWebPage,
                                onPressMoreOptions = { optionsExpanded = !optionsExpanded },
                                onPressSearchForTitle = { toolbarMode = ToolbarMode.SEARCH },
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
                            ToolbarMode.SEARCH -> ToolbarModeSearch(
                                focusRequester = focusRequester,
                                searchText = searchText,
                                onClose = {
                                    focusManager.clearFocus()
                                    toolbarMode = ToolbarMode.MAIN
                                    viewModel.startCatalogListMode()
                                },
                                onTextDone = { viewModel.startCatalogSearchMode(searchText.value) },
                                placeholderText = stringResource(R.string.search_by_title)
                            )
                        }
                    }
                    BooksVerticalView(
                        layoutMode = viewModel.listLayout,
                        list = viewModel.fetchIterator.list,
                        listState = state,
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
