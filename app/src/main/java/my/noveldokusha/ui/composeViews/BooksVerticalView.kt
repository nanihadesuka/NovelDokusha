package my.noveldokusha.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.IteratorState
import my.noveldokusha.ui.composeViews.BooksVerticalGridView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksVerticalView(
    layoutMode: AppPreferences.LIST_LAYOUT_MODE,
    list: List<BookMetadata>,
    listState: ScrollableState,
    error: String?,
    loadState: IteratorState,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit,
    onCopyError: (String) -> Unit
)
{
    // TODO() Have to find a way to sync LazyGridState and LazyListState, currently not working
    when (layoutMode)
    {
        AppPreferences.LIST_LAYOUT_MODE.verticalGrid -> BooksVerticalGridView(
            cells = GridCells.Fixed(2),
            list = list,
            listState = (listState as? LazyGridState) ?: rememberLazyGridState(),
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
            listState = (listState as? LazyListState) ?: rememberLazyListState(),
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