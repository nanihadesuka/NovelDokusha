package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.ui.composeViews.ErrorView

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChaptersScreenBody(
    state: ChapterScreenState,
    lazyListState: LazyListState,
    innerPadding: PaddingValues,
    onChapterClick: (chapter: ChapterWithContext) -> Unit,
    onChapterLongClick: (chapter: ChapterWithContext) -> Unit,
    onChapterDownload: (chapter: ChapterWithContext) -> Unit,
    onPullRefresh: () -> Unit,
) {
    var isRefreshingDelayed by remember { mutableStateOf(state.isRefreshing.value) }
    LaunchedEffect(Unit) {
        snapshotFlow { state.isRefreshing.value }
            .distinctUntilChanged()
            .collectLatest {
                if (it) delay(200)
                isRefreshingDelayed = it
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshingDelayed,
        onRefresh = onPullRefresh
    )

    Box(
        Modifier.pullRefresh(state = pullRefreshState)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 300.dp),
        ) {
            item(
                key = "header",
                contentType = { 0 },
            ) {
                ChaptersScreenHeader(
                    bookState = state.book.value,
                    sourceCatalogName = state.sourceCatalogName.value,
                    numberOfChapters = state.chapters.size,
                    paddingValues = innerPadding,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(
                items = state.chapters,
                key = { "_" + it.chapter.url },
                contentType = { 1 }
            ) {
                ChaptersScreenChapterItem(
                    chapterWithContext = it,
                    selected = state.selectedChaptersUrl.containsKey(it.chapter.url),
                    onClick = { onChapterClick(it) },
                    onLongClick = { onChapterLongClick(it) },
                    onDownload = { onChapterDownload(it) }
                )
            }

            if (state.error.value.isNotBlank()) item(
                key = "error",
                contentType = { 2 }
            ) {
                ErrorView(error = state.error.value)
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshingDelayed,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(innerPadding)
        )
    }
}
