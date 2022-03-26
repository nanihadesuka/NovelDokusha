package my.noveldokusha.ui.sourceCatalog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.MyButton

enum class ToolbarMode { MAIN, SEARCH }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceCatalogView(
    list: List<BookMetadata>,
    error: String?,
    loadState: FetchIteratorState.STATE,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit
) {
    val state = rememberLazyListState()

    val isReadyToLoad by derivedStateOf {
        val lastVisibleIndex = (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
        val isLoadZone = lastVisibleIndex > (state.layoutInfo.totalItemsCount - 3)
        val isIDLE = loadState == FetchIteratorState.STATE.IDLE
        isLoadZone && isIDLE
    }

    if (isReadyToLoad)
        LaunchedEffect(Unit) { onLoadNext() }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 260.dp)
    ) {
        if (error != null) {
            item {
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
            return@LazyColumn
        }

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
                    FetchIteratorState.STATE.LOADING -> CircularProgressIndicator(
                        color = ColorAccent
                    )
                    FetchIteratorState.STATE.CONSUMED -> Text(
                        text = when {
                            list.isEmpty() -> stringResource(R.string.no_results_found)
                            else -> stringResource(R.string.no_more_results)
                        },
                        color = ColorAccent
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun SearchToolbarMode(
    focusRequester: FocusRequester,
    searchText: MutableState<String>,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .background(MaterialTheme.colors.surface)
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        BackHandler { onClose() }

        // Fake button to center text
        IconButton(onClick = { }, enabled = false) {}

        BasicTextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            singleLine = true,
            maxLines = 1,
            textStyle = MaterialTheme.typography.h6.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(ColorAccent),
            keyboardActions = KeyboardActions(
                onDone = { onTextDone(searchText.value) }
            ),
            decorationBox = {
                if (searchText.value.isBlank()) Text(
                    text = "Search by title",
                    style = MaterialTheme.typography.h6.copy(
                        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                ) else it()
            },
            modifier = Modifier
                .focusRequester(focusRequester)
        )

        IconButton(onClick = {
            if (searchText.value.isBlank()) onClose()
            else searchText.value = ""
        }) {
            Icon(
                Icons.Default.Close,
                contentDescription = null
            )
        }
    }
}


@Composable
fun MainToolbarMode(
    title: String,
    subtitle: String,
    toolbarMode: MutableState<ToolbarMode>,
    onOpenSourceWebPage: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .background(MaterialTheme.colors.surface)
    ) {
        IconButton(onClick = { onOpenSourceWebPage() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_public_24),
                contentDescription = stringResource(R.string.open_the_web_view)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.subtitle1
            )
        }

        IconButton(onClick = { toolbarMode.value = ToolbarMode.SEARCH }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(R.string.search_for_title)
            )
        }
    }
}


@Preview
@Composable
fun Preview() {
    InternalTheme {
        SourceCatalogView(
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = FetchIteratorState.STATE.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {}
        )
    }
}