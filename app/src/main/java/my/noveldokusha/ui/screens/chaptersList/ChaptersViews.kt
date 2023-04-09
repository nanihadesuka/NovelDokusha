package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.composeViews.ErrorView
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.mix


@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun ChaptersListView(
    header: @Composable () -> Unit,
    list: SnapshotStateList<ChapterWithContext>,
    selectedChapters: SnapshotStateMap<String, Unit>,
    listState: LazyListState,
    error: String,
    onClick: (ChapterWithContext) -> Unit,
    onLongClick: (ChapterWithContext) -> Unit
) {

    val backgroundColorSelected =
        MaterialTheme.colorScheme.onPrimary.mix(MaterialTheme.colorScheme.primary, 0.1f)
    val backgroundColorUnselected =
        MaterialTheme.colorScheme.onPrimary.mix(MaterialTheme.colorScheme.primary, 0.5f)
    val colorTextRead = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
    val colorTextNotRead = MaterialTheme.colorScheme.onPrimary
    val colorIconSeenRead = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
    val colorIconSeenNotRead = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0f)
    val colorIconDownloadedRead = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
    val colorIconDownloadedNotRead = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0f)

    val backgroundColorNotSelectedRead = MaterialTheme.colorScheme.primary.mix(ColorAccent, 0.8f)
    val backgroundColorNotSelectedNotRead = MaterialTheme.colorScheme.primary
    val backgroundColorSelectedRead = backgroundColorSelected.mix(ColorAccent, 0.8f)
    val backgroundColorSelectedNotRead = backgroundColorSelected

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 240.dp)
    ) {
        item(key = "header") { header() }
        items(
            items = list,
            key = { "_" + it.chapter.url }
        ) {
            val selected by remember {
                derivedStateOf { selectedChapters.containsKey(it.chapter.url) }
            }

            val backgroundColor by animateColorAsState(
                targetValue = if (selected) {
                    if (it.lastReadChapter) backgroundColorSelectedRead
                    else backgroundColorSelectedNotRead
                } else {
                    if (it.lastReadChapter) backgroundColorNotSelectedRead
                    else backgroundColorNotSelectedNotRead
                }, label = ""
            )

            val indicatorColor = when (selected) {
                true -> MaterialTheme.colorScheme.primary
                false -> backgroundColorUnselected
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .height(68.dp)
                    .background(backgroundColor)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(color = indicatorColor),
                        onClick = { onClick(it) },
                        onLongClick = { onLongClick(it) }
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            ) {

                val colorText by animateColorAsState(
                    targetValue = if (it.chapter.read) colorTextRead else colorTextNotRead,
                    label = ""
                )

                val colorIconSeen by animateColorAsState(
                    targetValue = if (it.chapter.read) colorIconSeenRead else colorIconSeenNotRead,
                    label = ""
                )

                val colorIconDownloaded by animateColorAsState(
                    targetValue = if (it.downloaded) colorIconDownloadedRead else colorIconDownloadedNotRead,
                    label = ""
                )

                AnimatedContent(
                    targetState = it.chapter.title,
                    transitionSpec = {
                        if (targetState.length == initialState.length)
                            EnterTransition.None with ExitTransition.None
                        else
                            fadeIn() with fadeOut()
                    },
                    modifier = Modifier.weight(1f), label = "",
                ) { targetText ->
                    Text(
                        text = targetText,
                        modifier = Modifier.fillMaxWidth(),
                        color = colorText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_looking_24),
                    contentDescription = null,
                    tint = colorIconSeen
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_cloud_download_24),
                    contentDescription = null,
                    tint = colorIconDownloaded
                )
            }
        }
        if (error.isNotBlank()) item {
            ErrorView(error = error)
        }
    }
}

private class SampleProvider : PreviewParameterProvider<Themes> {
    override val values = Themes.list.asSequence()
}

@Preview
@Composable
private fun Preview(@PreviewParameter(SampleProvider::class) theme: Themes) {
    val list = (0..10).map {
        ChapterWithContext(
            chapter = Chapter(
                title = "Title chapter $it",
                url = "chapterUrl_$it",
                bookUrl = "bookUrl",
                position = it,
                read = it % 4 == 0,
                lastReadOffset = 0,
                lastReadPosition = 0
            ),
            downloaded = it % 2 == 0,
            lastReadChapter = it == 0
        )
    }.let { mutableStateListOf<ChapterWithContext>().apply { addAll(it) } }

    val selectedChapters = list
        .filterIndexed { index, _ -> (index % 3) == 0 }
        .associate { it.chapter.url to Unit }
        .let { SnapshotStateMap<String, Unit>().apply { putAll(it) } }

    InternalTheme(theme) {
        Box {
            ChaptersListView(
                header = {
                    ChaptersScreenHeader(
                        bookState = ChapterScreenState.BookState(
                            title = "Book title",
                            coverImageUrl = null,
                            url = "",
                            description = "In a very far land, there was a web novel being written."
                        ),
                        sourceCatalogName = "Novel Web Name",
                        numberOfChapters = 34,
                        paddingValues = PaddingValues()
                    )
                },
                list = list,
                selectedChapters = selectedChapters,
                listState = rememberLazyListState(),
                error = "Error",
                onClick = {},
                onLongClick = {}
            )
            MainToolbar(
                bookTitle = "Book title",
                isBookmarked = true,
                alpha = 0.5f,
                onClickBookmark = {},
                onClickChapterTitleSearch = {},
                onClickSortChapters = {},
                topPadding = 38.dp,
                height = 56.dp
            )
        }
    }
}