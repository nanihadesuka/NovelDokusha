package my.noveldokusha.features.sourceCatalog

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.composeViews.AnimatedTransition
import my.noveldokusha.ui.composeViews.BooksVerticalView
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.composeViews.ToolbarMode
import my.noveldokusha.ui.composeViews.TopAppBarSearch
import my.noveldokusha.utils.actionCopyToClipboard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SourceCatalogScreen(
    state: SourceCatalogScreenState,
    onSearchTextInputChange: (String) -> Unit,
    onSearchTextInputSubmit: (String) -> Unit,
    onSearchCatalogSubmit: () -> Unit,
    onListLayoutModeChange: (AppPreferences.LIST_LAYOUT_MODE) -> Unit,
    onToolbarModeChange: (ToolbarMode) -> Unit,
    onOpenSourceWebPage: () -> Unit,
    onBookClicked: (BookMetadata) -> Unit,
    onBookLongClicked: (BookMetadata) -> Unit,
    onPressBack: () -> Unit,
) {
    val context by rememberUpdatedState(newValue = LocalContext.current)
    val focusRequester = remember { FocusRequester() }
    val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
    var optionsExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                AnimatedTransition(targetState = state.toolbarMode.value) { target ->
                    when (target) {
                        ToolbarMode.MAIN -> TopAppBar(
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Unspecified,
                                scrolledContainerColor = Color.Unspecified,
                            ),
                            title = {
                                Column {
                                    Text(
                                        text = stringResource(id = state.sourceCatalogNameStrId.value),
                                        style = MaterialTheme.typography.headlineSmall,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = stringResource(R.string.catalog),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onPressBack
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { onOpenSourceWebPage() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_baseline_public_24),
                                        contentDescription = stringResource(R.string.open_the_web_view)
                                    )
                                }
                                IconButton(onClick = { onToolbarModeChange(ToolbarMode.SEARCH) }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_baseline_search_24),
                                        contentDescription = stringResource(R.string.search_for_title)
                                    )
                                }
                                IconButton(onClick = { optionsExpanded = !optionsExpanded }) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.open_for_more_options)
                                    )
                                    SourceCatalogDropDown(
                                        expanded = optionsExpanded,
                                        onDismiss = { optionsExpanded = !optionsExpanded },
                                        listLayoutMode = state.listLayoutMode.value,
                                        onListLayoutModeChange = onListLayoutModeChange
                                    )
                                }
                            }
                        )
                        ToolbarMode.SEARCH -> TopAppBarSearch(
                            scrollBehavior = scrollBehavior,
                            focusRequester = focusRequester,
                            searchTextInput = state.searchTextInput.value,
                            onClose = {
                                focusManager.clearFocus()
                                onToolbarModeChange(ToolbarMode.MAIN)
                                onSearchCatalogSubmit()
                            },
                            onTextDone = {
                                onSearchTextInputChange(it)
                                onSearchTextInputSubmit(it)
                            },
                            placeholderText = stringResource(R.string.search_by_title),
                            onSearchTextChange = onSearchTextInputChange
                        )
                    }
                }
                CollapsibleDivider(scrollBehavior.state)
            }
        },
        content = { innerPadding ->
            BooksVerticalView(
                layoutMode = state.listLayoutMode.value,
                list = state.fetchIterator.list,
                state = rememberLazyGridState(),
                error = state.fetchIterator.error,
                loadState = state.fetchIterator.state,
                onLoadNext = state.fetchIterator::fetchNext,
                onBookClicked = onBookClicked,
                onBookLongClicked = onBookLongClicked,
                onReload = state.fetchIterator::reloadFailedLastLoad,
                onCopyError = context::actionCopyToClipboard,
                innerPadding = innerPadding
            )
        }
    )
}