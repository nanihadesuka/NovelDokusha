package my.noveldokusha.features.databaseSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.states.PagedListIteratorState
import my.noveldokusha.R
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.tooling.local_database.BookMetadata
import my.noveldokusha.ui.composeViews.BooksVerticalView
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldoksuha.coreui.components.TopAppBarSearch
import my.noveldoksuha.coreui.theme.ColorNotice
import my.noveldoksuha.coreui.theme.Grey25
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldoksuha.coreui.theme.PreviewThemes
import my.noveldokusha.core.appPreferences.ListLayoutMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSearchScreen(
    state: DatabaseSearchScreenState,
    onSearchCatalogSubmit: () -> Unit,
    onGenresFiltersSubmit: () -> Unit,
    onSearchInputSubmit: () -> Unit,
    onSearchInputChange: (String) -> Unit,
    onSearchModeChange: (SearchMode) -> Unit,
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

    val searchPlaceholderText = when (state.searchMode.value) {
        SearchMode.BookTitle, SearchMode.Catalog -> stringResource(id = R.string.search_by_title)
        SearchMode.BookGenres -> ""
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBarSearch(
                focusRequester = focusRequester,
                searchTextInput = state.searchTextInput.value,
                onSearchTextChange = onSearchInputChange,
                onTextDone = {
                    onSearchModeChange(SearchMode.BookTitle)
                    onSearchInputSubmit()
                },
                onClose = onPressBack,
                placeholderText = searchPlaceholderText,
                scrollBehavior = scrollBehavior,
                inputEnabled = state.searchMode.value != SearchMode.BookGenres,
                labelText = stringResource(id = state.databaseNameStrId.value),
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
                        selected = state.searchMode.value == SearchMode.Catalog,
                        onClick = {
                            onSearchModeChange(SearchMode.Catalog)
                            focusRequester.freeFocus()
                            onSearchCatalogSubmit()
                        },
                        label = { Text(text = stringResource(R.string.filter_catalog)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedLeadingIconColor = Grey25,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            iconColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    )
                    FilterChip(
                        selected = state.searchMode.value == SearchMode.BookTitle,
                        onClick = {
                            onSearchModeChange(SearchMode.BookTitle)
                            focusRequester.requestFocus() // Doesn't work
                        },
                        label = { Text(text = stringResource(R.string.filter_title)) },
                        leadingIcon = { Icon(Icons.Filled.Title, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedLeadingIconColor = Grey25,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            iconColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    )
                    FilterChip(
                        selected = state.searchMode.value == SearchMode.BookGenres,
                        onClick = {
                            onSearchModeChange(SearchMode.BookGenres)
                            showFiltersBottomSheet.value = true
                        },
                        label = {
                            Text(text = stringResource(R.string.filter_genres))
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FilterList, null, tint = ColorNotice)
                        }
                    )
                }
                CollapsibleDivider(scrollBehavior.state)
                BooksVerticalView(
                    list = state.fetchIterator.list,
                    state = rememberLazyGridState(),
                    error = state.fetchIterator.error,
                    loadState = state.fetchIterator.state,
                    layoutMode = state.listLayoutMode.value,
                    onLoadNext = state.fetchIterator::fetchNext,
                    onBookClicked = onBookClicked,
                    onBookLongClicked = onBookLongClicked,
                )
            }
        }
    )

    LaunchedEffect(state.searchMode.value) {
        showFiltersBottomSheet.value = state.searchMode.value == SearchMode.BookGenres
    }

    if (showFiltersBottomSheet.value) {
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showFiltersBottomSheet.value = false },
            sheetState = sheetState,
        ) {
            DatabaseSearchBottomSheet(
                genresList = state.genresList,
                onGenresFiltersSubmit = {
                    onGenresFiltersSubmit()
                    coroutineScope.launch {
                        sheetState.partialExpand()
                    }
                }
            )
        }
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
    val fetchIterator =
        PagedListIteratorState(list = list, coroutineScope = scope) {
            Response.Success(PagedList(listOf(), 0, true))
        }
    val state = remember {
        DatabaseSearchScreenState(
            databaseNameStrId = mutableStateOf(R.string.database_name_baka_updates),
            searchMode = mutableStateOf(SearchMode.BookGenres),
            genresList = genresList,
            searchTextInput = mutableStateOf(""),
            fetchIterator = fetchIterator,
            listLayoutMode = mutableStateOf(ListLayoutMode.verticalGrid),
        )
    }

    InternalTheme {
        DatabaseSearchScreen(
            state = state,
            onSearchModeChange = { },
            onSearchInputChange = { },
            onSearchCatalogSubmit = { },
            onSearchInputSubmit = { },
            onBookClicked = { },
            onBookLongClicked = { },
            onPressBack = { },
            onGenresFiltersSubmit = { }
        )
    }
}