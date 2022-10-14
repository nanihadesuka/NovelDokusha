package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.rememberResolvedBookImagePath
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiViews.ErrorView
import my.noveldokusha.utils.ifCase
import my.noveldokusha.utils.mix

enum class ToolbarMode { MAIN, SEARCH }

@Composable
fun HeaderView(
    bookTitle: String,
    sourceName: String,
    numberOfChapters: Int,
    bookCoverUrl: String?,
    bookUrl: String,
    description: String,
    onSearchBookInDatabase: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    val bookCover = bookCoverUrl?.let {
        rememberResolvedBookImagePath(
            bookUrl = bookUrl,
            imagePath = it
        ).value
    } ?: R.drawable.ic_baseline_empty_24

    Box {
        Box {
            ImageView(
                imageModel = bookCover,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .alpha(0.2f)
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colors.surface.copy(alpha = 0f),
                            1f to MaterialTheme.colors.surface,
                        )
                    )
            )
        }
        Column(modifier = Modifier.padding(top = 58.dp)) {
            Row {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(160.dp, 230.dp),
                ) {
                    ImageView(
                        imageModel = bookCover,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(ImageBorderShape)
                    )
                }
                Column(
                    Modifier
                        .heightIn(min = 230.dp)
                        .padding(top = 60.dp, start = 0.dp, end = 8.dp)
                ) {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.chapters) + " " + numberOfChapters.toString(),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(32.dp)
                        ) {
                            IconButton(onClick = onSearchBookInDatabase) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_outline_explore_24),
                                    contentDescription = stringResource(id = R.string.search_in_database_for_more_information)
                                )
                            }
                            IconButton(onClick = onOpenInBrowser) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_outline_website_24),
                                    contentDescription = stringResource(id = R.string.open_in_browser)
                                )
                            }
                        }
                    }
                }
            }
            var maxLines by rememberSaveable { mutableStateOf(4) }
            Text(
                text = description,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(8.dp)
                    .ifCase(description.isNotBlank()) { animateContentSize() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { maxLines = if (maxLines == 4) Int.MAX_VALUE else 4 }
                    ),
            )
            Divider(Modifier.fillMaxWidth())
        }
    }
}

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
        MaterialTheme.colors.onPrimary.mix(MaterialTheme.colors.surface, 0.1f)
    val backgroundColorUnselected =
        MaterialTheme.colors.onPrimary.mix(MaterialTheme.colors.surface, 0.5f)
    val colorTextRead = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
    val colorTextNotRead = MaterialTheme.colors.onPrimary
    val colorIconSeenRead = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
    val colorIconSeenNotRead = MaterialTheme.colors.onPrimary.copy(alpha = 0f)
    val colorIconDownloadedRead = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
    val colorIconDownloadedNotRead = MaterialTheme.colors.onPrimary.copy(alpha = 0f)

    val backgroundColorNotSelectedRead = MaterialTheme.colors.surface.mix(ColorAccent, 0.8f)
    val backgroundColorNotSelectedNotRead = MaterialTheme.colors.surface
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
                }
            )

            val indicatorColor = when (selected) {
                true -> MaterialTheme.colors.surface
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
                    targetValue = if (it.chapter.read) colorTextRead else colorTextNotRead
                )

                val colorIconSeen by animateColorAsState(
                    targetValue = if (it.chapter.read) colorIconSeenRead else colorIconSeenNotRead
                )

                val colorIconDownloaded by animateColorAsState(
                    targetValue = if (it.downloaded) colorIconDownloadedRead else colorIconDownloadedNotRead
                )

                AnimatedContent(
                    targetState = it.chapter.title,
                    transitionSpec = {
                        if (targetState.length == initialState.length)
                            EnterTransition.None with ExitTransition.None
                        else
                            fadeIn() with fadeOut()
                    },
                    modifier = Modifier.weight(1f),
                ) { targetText ->
                    Text(
                        text = targetText,
                        modifier = Modifier.fillMaxWidth(),
                        color = colorText
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
                    HeaderView(
                        bookTitle = "Book title",
                        sourceName = "Novel Web Name",
                        numberOfChapters = 34,
                        bookCoverUrl = null,
                        bookUrl = "",
                        description = "In a very far land, there was a web novel being written.",
                        onSearchBookInDatabase = { },
                        onOpenInBrowser = { }
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