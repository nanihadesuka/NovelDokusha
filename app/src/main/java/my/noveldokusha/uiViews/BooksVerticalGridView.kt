package my.noveldokusha.uiViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderRadius

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksVerticalGridView(
    list: List<BookMetadata>,
    listState: LazyListState,
    error: String?,
    loadState: FetchIteratorState.STATE,
    cells: GridCells,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit
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

    LazyVerticalGrid(
        cells = cells,
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 260.dp)
    ) {
        if (error != null)
        {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                SelectionContainer {
                    Text(
                        text = error,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                        color = MaterialTheme.colors.onError,
                        modifier = Modifier
                            .padding(10.dp)
                            .border(0.5.dp, Color.Red, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    )
                }
            }
            return@LazyVerticalGrid
        }

        items(list) {
            MyButton(
                text = it.title,
                onClick = { onBookClicked(it) },
                radius = ImageBorderRadius,
                borderWidth = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onBookClicked(it) },
                        onLongClick = { onBookLongClicked(it) }
                    )
            ) { _, radius, _ ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1 / 1.45f)
                        .clip(RoundedCornerShape(radius))
                ) {
                    ImageView(
                        imageModel = it.coverImageUrl,
                        contentDescription = it.title,
                        modifier = Modifier.fillMaxSize(),
                        error = R.drawable.default_book_cover,
                    )
                    Icons.Default.Book
                    Text(
                        text = it.title,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    0f to MaterialTheme.colors.primary.copy(alpha = 0f),
                                    0.44f to MaterialTheme.colors.primary.copy(alpha = 0.5f),
                                    1f to MaterialTheme.colors.primary.copy(alpha = 0.85f),
                                )
                            )
                            .padding(top = 30.dp, bottom = 8.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
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
                    else ->
                    {
                    }
                }
            }
        }
    }
}