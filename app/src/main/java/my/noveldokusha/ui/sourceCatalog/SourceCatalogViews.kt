package my.noveldokusha.ui.sourceCatalog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.FetchIteratorState
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderRadius
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiUtils.ifCase
import my.noveldokusha.uiViews.ImageView
import my.noveldokusha.uiViews.MyButton

enum class ToolbarMode
{ MAIN, SEARCH }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceCatalogListView(
    list: List<BookMetadata>,
    state: LazyListState,
    error: String?,
    loadState: FetchIteratorState.STATE,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit
)
{
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
        if (error != null)
        {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceCatalogGridView(
    list: List<BookMetadata>,
    state: LazyListState,
    error: String?,
    loadState: FetchIteratorState.STATE,
    cells: GridCells,
    onLoadNext: () -> Unit,
    onBookClicked: (book: BookMetadata) -> Unit,
    onBookLongClicked: (bookItem: BookMetadata) -> Unit
)
{
    val isReadyToLoad by derivedStateOf {
        val lastVisibleIndex = (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
        val isLoadZone = lastVisibleIndex > (state.layoutInfo.totalItemsCount - 3)
        val isIDLE = loadState == FetchIteratorState.STATE.IDLE
        isLoadZone && isIDLE
    }

    if (isReadyToLoad)
        LaunchedEffect(Unit) { onLoadNext() }

    LazyVerticalGrid(
        cells = cells,
        state = state,
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

@Composable
fun ToolbarMain(
    title: String,
    subtitle: String,
    toolbarMode: MutableState<ToolbarMode>,
    onOpenSourceWebPage: () -> Unit,
    onPressMoreOptions: () -> Unit,
    optionsDropDownView: @Composable () -> Unit
)
{

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .background(MaterialTheme.colors.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
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
        IconButton(onClick = { onOpenSourceWebPage() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_public_24),
                contentDescription = stringResource(R.string.open_the_web_view)
            )
        }
        IconButton(onClick = { toolbarMode.value = ToolbarMode.SEARCH }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(R.string.search_for_title)
            )
        }

        IconButton(onClick = onPressMoreOptions) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.open_for_more_options)
            )
            optionsDropDownView()
        }
    }
}

@Composable
fun OptionsDropDown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    listLayoutMode: AppPreferences.LIST_LAYOUT_MODE,
    onSelectListLayout: (mode: AppPreferences.LIST_LAYOUT_MODE) -> Unit
)
{
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Books layout",
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(
                Modifier
                    .padding(4.dp)
                    .border(
                        Dp.Hairline,
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                @Composable
                fun colorBackground(state: AppPreferences.LIST_LAYOUT_MODE) =
                    if (listLayoutMode == state) ColorAccent else MaterialTheme.colors.surface

                @Composable
                fun colorText(state: AppPreferences.LIST_LAYOUT_MODE) =
                    if (listLayoutMode == state) Color.White else MaterialTheme.colors.onPrimary
                Box(
                    Modifier
                        .weight(1f)
                        .background(colorBackground(AppPreferences.LIST_LAYOUT_MODE.verticalList))
                        .clickable { onSelectListLayout(AppPreferences.LIST_LAYOUT_MODE.verticalList) }
                ) {
                    Text(
                        text = stringResource(R.string.list),
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = colorText(AppPreferences.LIST_LAYOUT_MODE.verticalList)
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(colorBackground(AppPreferences.LIST_LAYOUT_MODE.verticalGrid))
                        .clickable { onSelectListLayout(AppPreferences.LIST_LAYOUT_MODE.verticalGrid) }
                ) {
                    Text(
                        text = stringResource(R.string.grid),
                        Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = colorText(AppPreferences.LIST_LAYOUT_MODE.verticalGrid)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewList()
{
    val state = rememberLazyListState()
    InternalTheme {
        SourceCatalogListView(
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = FetchIteratorState.STATE.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            state = state
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun PreviewGrid()
{
    val state = rememberLazyListState()
    InternalTheme {
        SourceCatalogGridView(
            cells = GridCells.Fixed(2),
            list = (1..10).map { BookMetadata("Book $it", "url") },
            error = null,
            loadState = FetchIteratorState.STATE.LOADING,
            onLoadNext = {},
            onBookClicked = {},
            onBookLongClicked = {},
            state = state
        )
    }
}