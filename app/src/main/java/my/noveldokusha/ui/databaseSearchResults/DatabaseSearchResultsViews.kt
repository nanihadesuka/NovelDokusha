package my.noveldokusha.ui.databaseSearchResults

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIterator
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.MyButton

@Composable
fun DatabaseSearchResultsView(
    title: String,
    subtitle: String,
    list: List<BookMetadata>,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    loadState: FetchIteratorState.STATE
) {
    val state = rememberLazyListState()

    val loadMore by derivedStateOf {
        val total = state.layoutInfo.totalItemsCount
        val lastVisibleItemIndex =
            (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

        val buffer = 3
        val readyToLoadMore = loadState == FetchIteratorState.STATE.IDLE
        val isLastVisible = lastVisibleItemIndex > (total - buffer)
        val doit = readyToLoadMore && isLastVisible
        Log.e("--------", "-----------")
        Log.e("readyToLoadMore", readyToLoadMore.toString())
        Log.e("isLastVisible", isLastVisible.toString())
        Log.e("loadState", loadState.name)
        Log.e("--------", "-----------")
        doit
    }

    LaunchedEffect(loadState) {
        Log.e("loadState", loadState.name)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { loadMore }
            .filter { it }
            .debounce(100)
            .collect {
                onLoadNext()
            }
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 260.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = subtitle, style = MaterialTheme.typography.subtitle1)
            }
        }
        items(list) {
            MyButton(
                text = it.title,
                onClick = { onBookClicked(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (loadState == FetchIteratorState.STATE.LOADING) item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(50.dp)
                    .fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(25.dp),
                    color = ColorAccent
                )
            }
        }
    }
}


@Preview
@Composable
fun Preview() {
    InternalTheme {
        DatabaseSearchResultsView(
            title = "Database: Baka-Updates",
            subtitle = "Search by genre",
            list = (1..10).map { BookMetadata("Book $it", "url") },
            onLoadNext = {},
            onBookClicked = {},
            loadState = FetchIteratorState.STATE.LOADING
        )
    }
}