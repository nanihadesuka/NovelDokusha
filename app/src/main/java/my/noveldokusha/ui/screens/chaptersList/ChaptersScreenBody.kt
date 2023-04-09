package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.ui.composeViews.ErrorView

@Composable
fun ChaptersScreenBody(
    state: ChapterScreenState,
    lazyListState: LazyListState,
    innerPadding: PaddingValues,
    onChapterClick: (chapter: ChapterWithContext) -> Unit,
    onChapterLongClick: (chapter: ChapterWithContext) -> Unit,
    onChapterDownload: (chapter: ChapterWithContext) -> Unit,
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
                paddingValues = innerPadding
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
}