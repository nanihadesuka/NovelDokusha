package my.noveldokusha.ui.screens.main.finder

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.composeViews.ToolbarModeSearch
import my.noveldokusha.ui.screens.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.screens.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.screens.sourceCatalog.SourceCatalogActivity
import my.noveldokusha.ui.screens.sourceCatalog.ToolbarMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinderScreen(
    viewModel: FinderViewModel = viewModel()
) {
    val context by rememberUpdatedState(newValue = LocalContext.current)
    var searchText by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val toolbarMode = rememberSaveable { mutableStateOf(ToolbarMode.MAIN) }
    var languagesOptionsExpanded by remember { mutableStateOf(false) }
    val focusManager by rememberUpdatedState(newValue = LocalFocusManager.current)
    val lazyListState = rememberLazyListState()
    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(appBarState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                when (toolbarMode.value) {
                    ToolbarMode.MAIN -> TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Unspecified,
                            scrolledContainerColor = Color.Unspecified,
                        ),
                        title = {
                            Text(
                                text = stringResource(id = R.string.title_finder),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        },
                        actions = {
                            IconButton(onClick = { toolbarMode.value = ToolbarMode.SEARCH }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_search_24),
                                    contentDescription = stringResource(R.string.search_for_title)
                                )
                            }
                            IconButton(onClick = {
                                languagesOptionsExpanded = !languagesOptionsExpanded
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_languages_24),
                                    contentDescription = stringResource(R.string.open_for_more_options)
                                )
                                LanguagesDropDown(
                                    expanded = languagesOptionsExpanded,
                                    list = viewModel.languagesList,
                                    onDismiss = { languagesOptionsExpanded = false },
                                    onToggleLanguage = { viewModel.toggleSourceLanguage(it.language) }
                                )
                            }
                        }
                    )

                    ToolbarMode.SEARCH -> ToolbarModeSearch(
                        focusRequester = focusRequester,
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                        },
                        onClose = {
                            focusManager.clearFocus()
                            toolbarMode.value = ToolbarMode.MAIN
                        },
                        onTextDone = { context.goToGlobalSearch(searchText) },
                        placeholderText = stringResource(R.string.global_search),
                    )
                }
                CollapsibleDivider(lazyListState)
            }
        },
        content = { innerPadding ->
            FinderScreenBody(
                lazyListState = lazyListState,
                innerPadding = innerPadding,
                databasesList = viewModel.databaseList,
                sourcesList = viewModel.sourcesList,
                onDatabaseClick = context::goToDatabaseSearch,
                onSourceClick = context::goToSourceCatalog,
                onSourceSetPinned = viewModel::onSourceSetPinned
            )
        }
    )
}

private fun Context.goToSourceCatalog(source: SourceInterface.Catalog) {
    SourceCatalogActivity
        .IntentData(this, sourceBaseUrl = source.baseUrl)
        .let(::startActivity)
}

private fun Context.goToDatabaseSearch(database: DatabaseInterface) {
    DatabaseSearchActivity
        .IntentData(this, databaseBaseUrl = database.baseUrl)
        .let(::startActivity)
}

private fun Context.goToGlobalSearch(text: String) {
    GlobalSourceSearchActivity
        .IntentData(this, text)
        .let(::startActivity)
}
