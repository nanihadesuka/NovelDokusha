package my.noveldokusha.ui.globalSourceSearch

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.scraper.downloadBookCoverImageUrl
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.GlideImageFadeIn

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
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
            )
            SourceListView(
                list = entry.fetchIterator.list,
                loadState = entry.fetchIterator.state,
                error = entry.fetchIterator.error,
                onBookClick = onBookClick,
                onLoadNext = { entry.fetchIterator.fetchNext() },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SourceListView(
    list: List<BookMetadata>,
    loadState: FetchIteratorState.STATE,
    error: String?,
    onBookClick: (book: BookMetadata) -> Unit,
    onLoadNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    val isReadyToLoad by remember(loadState, state) {
        derivedStateOf {
            val lastVisibleIndex = (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val isLoadZone = lastVisibleIndex > (state.layoutInfo.totalItemsCount - 3)
            val isIDLE = loadState == FetchIteratorState.STATE.IDLE
            isLoadZone && isIDLE
        }
    }

    val noResults by remember(loadState, list.isEmpty()) {
        derivedStateOf {
            (loadState == FetchIteratorState.STATE.CONSUMED) && list.isEmpty()
        }
    }

    LaunchedEffect(isReadyToLoad) { if (isReadyToLoad) onLoadNext() }

    LazyRow(
        state = state,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 30.dp,
        ),
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {

        items(list) { book ->
            Column(
                Modifier
                    .width(130.dp)
                    .padding(end = 8.dp)
            ) {
                GlideImageFadeIn(
                    imageModel = book.coverImageUrl,
                    modifier = Modifier
                        .clickable { onBookClick(book) }
                        .fillMaxWidth()
                        .aspectRatio(1/1.5f)
                        .border(
                            0.dp,
                            MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = book.title,
                    maxLines = 2,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .height(40.dp)
                        .padding(4.dp)
                        .fillMaxWidth()
                )
            }
        }

        item {

            Box(
                contentAlignment = if (noResults) Alignment.CenterStart else Alignment.Center,
                modifier = Modifier.width(160.dp)
            ) {
                when (loadState) {
                    FetchIteratorState.STATE.LOADING -> CircularProgressIndicator(
                        color = ColorAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                    FetchIteratorState.STATE.CONSUMED -> when {
                        error != null -> Text(
                            text = stringResource(R.string.error_loading),
                            color = MaterialTheme.colors.onError,
                        )
                        list.isEmpty() -> Text(
                            text = stringResource(R.string.no_results_found),
                            color = ColorAccent,
                        )
                        else -> Text(
                            text = stringResource(R.string.no_more_results),
                            color = ColorAccent,
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
