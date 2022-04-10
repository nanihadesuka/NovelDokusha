package my.noveldokusha.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksVerticalView(
    layoutMode: AppPreferences.LIST_LAYOUT_MODE,
    list: List<BookMetadata>,
    listState: LazyListState,
    error: String?,
    loadState: FetchIteratorState.STATE,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit,
    onCopyError: (String) -> Unit
)
{
    when (layoutMode)
    {
        AppPreferences.LIST_LAYOUT_MODE.verticalGrid -> BooksVerticalGridView(
            cells = GridCells.Fixed(2),
            list = list,
            listState = listState,
            error = error,
            loadState = loadState,
            onLoadNext = onLoadNext,
            onBookClicked = onBookClicked,
            onBookLongClicked = onBookLongClicked,
            onReload = onReload,
            onCopyError = onCopyError,
        )
        AppPreferences.LIST_LAYOUT_MODE.verticalList -> BooksVerticalListView(
            list = list,
            listState = listState,
            error = error,
            loadState = loadState,
            onLoadNext = onLoadNext,
            onBookClicked = onBookClicked,
            onBookLongClicked = onBookLongClicked,
            onReload = onReload,
            onCopyError = onCopyError,
        )
    }
}