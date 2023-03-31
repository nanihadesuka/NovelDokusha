package my.noveldokusha.ui.screens.main.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.rememberResolvedBookImagePath
import my.noveldokusha.ui.composeViews.BookImageButtonView
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderShape

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreenBody(
    tabs: List<String>,
    innerPadding: PaddingValues,
    onBookClick: (BookWithContext) -> Unit,
    onBookLongClick: (BookWithContext) -> Unit,
    viewModel: LibraryPageViewModel = viewModel()
) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val updateCompleted = rememberUpdatedState(newValue = pagerState.currentPage)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isPullRefreshing,
        onRefresh = {
            viewModel.onLibraryCategoryRefresh(isCompletedCategory = updateCompleted.value == 1)
        }
    )
    val lazyGridState = rememberLazyGridState()

    Box(
        modifier = Modifier
            .pullRefresh(state = pullRefreshState)
            .padding(innerPadding),
    ) {
        Column {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = {
                    val tabPos = it[pagerState.currentPage]
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPos)
                            .fillMaxSize()
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .zIndex(-1f)
                    )
                },
                tabs = {
                    tabs.forEachIndexed { index, title ->
                        val selected by remember { derivedStateOf { pagerState.currentPage == index } }
                        Tab(
                            selected = selected,
                            text = { Text(title, color = MaterialTheme.colorScheme.onPrimary) },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                },
                divider = {
                    CollapsibleDivider(lazyGridState)
                }
            )
            HorizontalPager(
                pageCount = tabs.size,
                state = pagerState
            ) { page ->

                // TODO: improve + make more generic (use database table?)
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
                    lazyGridState = lazyGridState,
                    onClick = onBookClick,
                    onLongClick = onBookLongClick
                )
            }
        }
        PullRefreshIndicator(
            refreshing = viewModel.isPullRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LibraryPageBody(
    list: List<BookWithContext>,
    lazyGridState: LazyGridState,
    onClick: (BookWithContext) -> Unit,
    onLongClick: (BookWithContext) -> Unit,
) {
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 4.dp, bottom = 400.dp, start = 4.dp, end = 4.dp)
    ) {
        items(
            items = list,
            key = { it.book.url }
        ) {
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
                AnimatedVisibility(
                    visible = notReadCount != 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = notReadCount.toString(),
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(ColorAccent, ImageBorderShape)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
