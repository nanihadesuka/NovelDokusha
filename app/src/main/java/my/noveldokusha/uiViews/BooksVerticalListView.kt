package my.noveldokusha.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.ui.theme.ColorAccent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksVerticalListView(
    list: List<BookMetadata>,
    listState: LazyListState,
    error: String?,
    loadState: FetchIteratorState.STATE,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit,
    onReload: () -> Unit = {},
    onCopyError: (String) -> Unit = {}
)
{
    val isReadyToLoad by derivedStateOf {
        val lastVisibleIndex = (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
        val isLoadZone = lastVisibleIndex > (listState.layoutInfo.totalItemsCount - 3)
        val isIDLE = loadState == FetchIteratorState.STATE.IDLE
        isLoadZone && isIDLE
    }

    if (isReadyToLoad)
        LaunchedEffect(Unit) { onLoadNext() }

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
                when (loadState)
                {
                    FetchIteratorState.STATE.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    FetchIteratorState.STATE.CONSUMED -> Text(
                        text = when
                        {
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

