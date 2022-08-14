package my.noveldokusha.ui.screens.main.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.rememberResolvedBookImagePath
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.uiViews.BookImageButtonView

@OptIn(ExperimentalPagerApi::class)
@Composable
fun LibraryBody(
    tabs: List<String>,
    onBookClick: (BookWithContext) -> Unit,
    onBookLongClick: (BookWithContext) -> Unit
) {
    val viewModel = viewModel<LibraryPageViewModel>()
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val pageIndex by remember {
        derivedStateOf { pagerState.currentPage }
    }
    val updateCompleted = rememberUpdatedState(newValue = pageIndex)
    SwipeRefresh(
        state = viewModel.refreshState,
        onRefresh = {
            viewModel.onLibraryCategoryRefresh(updateCompleted.value == 1)
        }
    ) {
        Column {
            TabRow(selectedTabIndex = pageIndex) {
                tabs.forEachIndexed { index, title ->
                    val selected by remember {
                        derivedStateOf { pageIndex == index }
                    }
                    Tab(
                        text = { Text(title) },
                        selected = selected,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }
            HorizontalPager(
                count = tabs.size,
                state = pagerState
            ) { page ->
                val showCompleted by remember {
                    derivedStateOf {
                        tabs[page] == "Completed"
                    }
                }
                val list: List<BookWithContext> by remember {
                    derivedStateOf {
                        when (showCompleted) {
                            true -> viewModel.listCompleted
                            else -> viewModel.listReading
                        }
                    }
                }
                LibraryPageBody(
                    list = list,
                    onClick = onBookClick,
                    onLongClick = onBookLongClick
                )
            }
        }
    }
}

@Composable
fun LibraryPageBody(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onLongClick: (BookWithContext) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 4.dp, bottom = 400.dp, start = 4.dp, end = 4.dp)
    ) {
        items(list) {
            val coverImageUrl by rememberResolvedBookImagePath(
                bookUrl = it.book.url,
                imagePath = it.book.coverImageUrl
            )
            Box {
                BookImageButtonView(
                    title = it.book.title,
                    coverImageUrl = coverImageUrl,
                    onClick = { onClick(it) },
                    onLongClick = { onLongClick(it) }
                )
                val notReadCount = it.chaptersCount - it.chaptersReadCount
                AnimatedVisibility(visible = notReadCount != 0) {
                    Text(
                        text = notReadCount.toString(),
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(ColorAccent, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarMain(
    title: String,
    onOpenBottomSheetOptionsPress: () -> Unit,
    onOptionsDropDownPress: () -> Unit,
    onOptionsDropDownView: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenBottomSheetOptionsPress) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_filter_list_24),
                contentDescription = stringResource(R.string.options_panel)
            )
        }

        IconButton(onClick = onOptionsDropDownPress) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.open_for_more_options)
            )
            onOptionsDropDownView()
        }
    }
}
