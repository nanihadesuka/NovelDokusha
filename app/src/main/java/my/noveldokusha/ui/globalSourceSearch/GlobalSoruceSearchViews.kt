package my.noveldokusha.ui.globalSourceSearch

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.MyButton

@Composable
fun GlobalSourceSearchView(
    listSources: List<SourceResults>,
    onBookClick: (book: BookMetadata) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 260.dp)
    ) {
        items(listSources) { entry ->
            Text(
                text = entry.source.name.capitalize(),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(8.dp),
            )
            SourceListView(
                list = entry.fetchIterator.list,
                loadState = entry.fetchIterator.state,
                error = entry.fetchIterator.error,
                onBookClick = onBookClick,
                onLoadNext = { entry.fetchIterator.fetchNext() },
            )
        }
    }
}

@Composable
fun SourceListView(
    list: List<BookMetadata>,
    loadState: FetchIteratorState.STATE,
    error: String?,
    onBookClick: (book: BookMetadata) -> Unit,
    onLoadNext: () -> Unit,
) {
    val state = rememberLazyListState()

    val isReadyToLoad by derivedStateOf {
        val lastVisibleIndex = (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
        val isLoadZone = lastVisibleIndex > (state.layoutInfo.totalItemsCount - 3)
        val isIDLE = loadState == FetchIteratorState.STATE.IDLE
        isLoadZone && isIDLE
    }

    val noResults by derivedStateOf {
        (loadState == FetchIteratorState.STATE.CONSUMED) && list.isEmpty()
    }

    LaunchedEffect(isReadyToLoad) { if (isReadyToLoad) onLoadNext() }

    LazyRow(
        state = state,
        contentPadding = PaddingValues(
            start = 6.dp,
            end = 30.dp,
        ),
        modifier = Modifier
            .animateContentSize()
            .height(if (noResults) 40.dp else 160.dp)
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {

        items(list) { book ->
            MyButton(
                text = book.title,
                onClick = { onBookClick(book) },
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(3 / 4f)
            )
        }

        item {
            Box(
                contentAlignment = if (noResults) Alignment.CenterStart else Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(160.dp)
                    .padding(start = if (noResults) 2.dp else 0.dp)
            ) {
                when (loadState) {
                    FetchIteratorState.STATE.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    FetchIteratorState.STATE.CONSUMED -> when {
                        error != null -> Text(
                            text = stringResource(R.string.error_loading),
                            color = MaterialTheme.colors.onError,
                        )
                        list.isEmpty() -> Text(
                            text = stringResource(R.string.no_results_found),
                            color = ColorAccent
                        )
                        else -> Text(
                            text = stringResource(R.string.no_more_results),
                            color = ColorAccent
                        )
                    }
                    FetchIteratorState.STATE.IDLE -> {}
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {

    val list = scraper.sourcesListCatalog.map { source ->
        source to (0..5).map { BookMetadata(title = "Book $it", url = "") }
    }.map { (source, books) ->
        val sr = SourceResults(source, "", rememberCoroutineScope())
        sr.fetchIterator.list.addAll(books)
        sr
    }

    InternalTheme {
        GlobalSourceSearchView(listSources = list, onBookClick = {})
    }
}
