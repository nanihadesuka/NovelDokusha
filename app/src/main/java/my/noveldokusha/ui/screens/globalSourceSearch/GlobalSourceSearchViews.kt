package my.noveldokusha.ui.screens.globalSourceSearch

import android.net.Uri
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.IteratorState
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderRadius
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.ImageView
import my.noveldokusha.composableActions.ListLoadWatcher
import my.noveldokusha.utils.capitalize
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.*

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
                text = entry.source.name.capitalize(Locale.ROOT),
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

@Composable
fun SourceListView(
    list: List<BookMetadata>,
    loadState: IteratorState,
    error: String?,
    onBookClick: (book: BookMetadata) -> Unit,
    onLoadNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    ListLoadWatcher(listState = state, loadState = loadState, onLoadNext = onLoadNext)

    val noResults by remember(loadState, list.isEmpty()) {
        derivedStateOf {
            (loadState == IteratorState.CONSUMED) && list.isEmpty()
        }
    }

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
        items(list) {
            Column(
                Modifier
                    .width(130.dp)
                    .padding(end = 8.dp)
            ) {
                ImageView(
                    imageModel = it.coverImageUrl,
                    modifier = Modifier
                        .clickable { onBookClick(it) }
                        .fillMaxWidth()
                        .aspectRatio(1 / 1.45f)
                        .border(
                            0.dp,
                            MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                            RoundedCornerShape(ImageBorderRadius)
                        )
                        .clip(RoundedCornerShape(ImageBorderRadius)),
                )
                Text(
                    text = it.title,
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
                    IteratorState.LOADING -> CircularProgressIndicator(
                        color = ColorAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                    IteratorState.CONSUMED -> when {
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
                    IteratorState.IDLE -> {}
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {

    val scraper = Scraper(object : NetworkClient {
        override val client by lazy { OkHttpClient() }
        override val clientWithRedirects by lazy { OkHttpClient() }
        override suspend fun call(request: Request.Builder) = Response.Builder().build()
        override suspend fun get(url: String) = Response.Builder().build()
        override suspend fun get(url: Uri.Builder) = Response.Builder().build()
    })

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
