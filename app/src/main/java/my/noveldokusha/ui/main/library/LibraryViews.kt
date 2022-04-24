package my.noveldokusha.ui.main.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.uiViews.BookImageButtonView

@OptIn(ExperimentalPagerApi::class)
@Composable
fun LibraryBody(
    tabs: List<String>,
    onBookClick: (BookWithContext) -> Unit,
    onBookLongClick: (BookWithContext) -> Unit
)
{
    val viewModel = viewModel<LibraryPageViewModel>()
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val pageIndex by remember {
        derivedStateOf { pagerState.targetPage }
    }
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
                    when (showCompleted)
                    {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPageBody(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onLongClick: (BookWithContext) -> Unit,
)
{
    LazyVerticalGrid(
        cells = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 4.dp, bottom = 400.dp, start = 4.dp, end = 4.dp)
    ) {
        items(list) {
            Box {
                BookImageButtonView(
                    title = it.book.title,
                    coverImageUrl = it.book.coverImageUrl,
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