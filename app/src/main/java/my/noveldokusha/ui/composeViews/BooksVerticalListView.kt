package my.noveldokusha.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.composableActions.ListLoadWatcher
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.network.IteratorState
import my.noveldokusha.ui.theme.ColorAccent

@ExperimentalFoundationApi
@Composable
fun BooksVerticalListView(
    list: List<BookMetadata>,
    listState: LazyListState,
    error: String?,
    loadState: IteratorState,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {}
) {
    ListLoadWatcher(
        listState = listState,
        loadState = loadState,
        onLoadNext = onLoadNext
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 260.dp)
    ) {
        items(list) {
            MyButton(
                text = it.title,
                onClick = { onBookClicked(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onBookClicked(it) },
                        onLongClick = { onBookLongClicked(it) }
                    )
            )
        }

        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                when (loadState) {
                    IteratorState.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    IteratorState.CONSUMED -> Text(
                        text = when {
                            list.isEmpty() -> stringResource(R.string.no_results_found)
                            else -> stringResource(R.string.no_more_results)
                        },
                        color = ColorAccent
                    )
                    else -> Unit
                }
            }
        }

        if (error != null) item {
            ErrorView(error = error, onReload = onReload, onCopyError = onCopyError)
        }
    }
}

