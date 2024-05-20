package my.noveldokusha.libraryexplorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import my.noveldoksuha.coreui.components.CollapsibleDivider
import my.noveldoksuha.coreui.theme.colorApp
import my.noveldokusha.core.domain.LibraryCategory

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
@Composable
internal fun LibraryScreenBody(
    tabs: List<String>,
    innerPadding: PaddingValues,
    topAppBarState: TopAppBarState,
    onBookClick: (my.noveldokusha.tooling.local_database.BookWithContext) -> Unit,
    onBookLongClick: (my.noveldokusha.tooling.local_database.BookWithContext) -> Unit,
    viewModel: LibraryPageViewModel = viewModel()
) {
    val tabsSizeUpdated = rememberUpdatedState(newValue = tabs.size)

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f ,
        pageCount = { tabsSizeUpdated.value }
    )
    val scope = rememberCoroutineScope()
    val updateCompleted = rememberUpdatedState(newValue = pagerState.currentPage)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isPullRefreshing,
        onRefresh = {
            viewModel.onLibraryCategoryRefresh(
                libraryCategory = when (updateCompleted.value) {
                    0 -> LibraryCategory.DEFAULT
                    else -> LibraryCategory.COMPLETED
                }
            )
        }
    )

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
                            .background(MaterialTheme.colorApp.tabSurface, CircleShape)
                            .zIndex(-1f)
                    )
                },
                tabs = {
                    tabs.forEachIndexed { index, text ->
                        val selected by remember { derivedStateOf { pagerState.currentPage == index } }
                        val title by remember { derivedStateOf { text } }
                        Tab(
                            selected = selected,
                            text = { Text(title, color = MaterialTheme.colorScheme.onPrimary) },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                },
                divider = {
                    CollapsibleDivider(topAppBarState)
                }
            )
            HorizontalPager(
                state = pagerState
            ) { page ->
                // TODO: improve + make more generic (use database table?)
                val showCompleted by remember {
                    derivedStateOf {
                        tabs[page] == "Completed"
                    }
                }
                val list: List<my.noveldokusha.tooling.local_database.BookWithContext> by remember {
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
        PullRefreshIndicator(
            refreshing = viewModel.isPullRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
