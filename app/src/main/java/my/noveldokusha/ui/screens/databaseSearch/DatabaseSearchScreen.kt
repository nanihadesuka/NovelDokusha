package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.PagedListIteratorState
import my.noveldokusha.ui.composeViews.BooksVerticalView
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.composeViews.TopAppBarSearch
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.PreviewThemes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSearchScreen(
    databaseName: String,
    searchMode: SearchMode,
    searchInput: String,
    genresList: SnapshotStateList<GenreItem>,
    onSearchCatalogSubmit: () -> Unit,
    onGenresFiltersSubmit: () -> Unit,
    onSearchInputSubmit: () -> Unit,
    onSearchInputChange: (String) -> Unit,
    onSearchModeChange: (SearchMode) -> Unit,
    fetchIterator: PagedListIteratorState<BookMetadata>,
    listLayoutMode: AppPreferences.LIST_LAYOUT_MODE,
    onBookClicked: (BookMetadata) -> Unit,
    onBookLongClicked: (BookMetadata) -> Unit,
    onPressBack: () -> Unit,
) {
    val showFiltersBottomSheet = rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    val searchPlaceholderText = when (searchMode) {
        SearchMode.BookTitle, SearchMode.Catalog -> stringResource(id = R.string.search_by_title)
        SearchMode.BookGenres -> ""
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBarSearch(
                focusRequester = focusRequester,
                searchTextInput = searchInput,
                onSearchTextChange = onSearchInputChange,
                onTextDone = {
                    onSearchModeChange(SearchMode.BookTitle)
                    onSearchInputSubmit()
                },
                onClose = onPressBack,
                placeholderText = searchPlaceholderText,
                scrollBehavior = scrollBehavior,
                inputEnabled = searchMode != SearchMode.BookGenres,
                labelText = databaseName
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    FilterChip(
                        selected = searchMode == SearchMode.Catalog,
                        onClick = {
                            onSearchModeChange(SearchMode.Catalog)
                            focusRequester.freeFocus()
                            onSearchCatalogSubmit()
                        },
                        label = {
                            Text(text = stringResource(R.string.filter_catalog))
                        }
                    )
                    FilterChip(
                        selected = searchMode == SearchMode.BookTitle,
                        onClick = {
                            onSearchModeChange(SearchMode.BookTitle)
                            focusRequester.requestFocus() // Doesn't work
                        },
                        label = {
                            Text(text = stringResource(R.string.filter_title))
                        }
                    )
                    FilterChip(
                        selected = searchMode == SearchMode.BookGenres,
                        onClick = {
                            onSearchModeChange(SearchMode.BookGenres)
                            showFiltersBottomSheet.value = true
                        },
                        label = {
                            Text(text = stringResource(R.string.filter_genres))
                        }
                    )
                }
                CollapsibleDivider(scrollBehavior.state)
                BooksVerticalView(
                    list = fetchIterator.list,
                    state = rememberLazyGridState(),
                    error = fetchIterator.error,
                    loadState = fetchIterator.state,
                    layoutMode = listLayoutMode,
                    onLoadNext = { fetchIterator.fetchNext() },
                    onBookClicked = onBookClicked,
                    onBookLongClicked = onBookLongClicked,
                )
            }
        }
    )

    LaunchedEffect(searchMode) {
        showFiltersBottomSheet.value = searchMode == SearchMode.BookGenres
    }

    if (showFiltersBottomSheet.value) ModalBottomSheet(
        onDismissRequest = { showFiltersBottomSheet.value = false }
    ) {
        DatabaseSearchBottomSheet(
            genresList = genresList,
            onGenresFiltersSubmit = onGenresFiltersSubmit
        )
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {

    val genresList = remember { mutableStateListOf<GenreItem>() }
    val list = remember {
        mutableStateListOf<BookMetadata>(
            BookMetadata(title = "title1", url = "url1"),
            BookMetadata(title = "title2", url = "url2"),
            BookMetadata(title = "title3", url = "url3"),
        )
    }
    val scope = rememberCoroutineScope()
    val fetchIterator = PagedListIteratorState(list = list, coroutineScope = scope) {
        Response.Success(PagedList(listOf(), 0, true))
    }

    InternalTheme {
        DatabaseSearchScreen(
            databaseName = "Database",
            searchMode = SearchMode.BookTitle,
            genresList = genresList,
            searchInput = "",
            onSearchModeChange = { },
            onSearchInputChange = { },
            onSearchCatalogSubmit = { },
            onSearchInputSubmit = { },
            fetchIterator = fetchIterator,
            listLayoutMode = AppPreferences.LIST_LAYOUT_MODE.verticalGrid,
            onBookClicked = { },
            onBookLongClicked = { },
            onPressBack = { },
            onGenresFiltersSubmit = { }
        )
    }
}