package my.noveldokusha.features.chaptersList

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PublishedWithChanges
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.ColorLike
import my.noveldoksuha.coreui.theme.ColorNotice
import my.noveldoksuha.coreui.theme.colorApp
import my.noveldoksuha.coreui.theme.isAtTop
import my.noveldokusha.R
import my.noveldokusha.core.isLocalUri
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.ui.goToWebBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    state: ChaptersScreenState,
    onLibraryToggle: () -> Unit,
    onSearchBookInDatabase: () -> Unit,
    onResumeReading: () -> Unit,
    onPressBack: () -> Unit,
    onSelectedDeleteDownloads: () -> Unit,
    onSelectedDownload: () -> Unit,
    onSelectedSetRead: () -> Unit,
    onSelectedSetUnread: () -> Unit,
    onSelectedInvertSelection: () -> Unit,
    onSelectAllChapters: () -> Unit,
    onCloseSelectionBar: () -> Unit,
    onChapterClick: (chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) -> Unit,
    onChapterLongClick: (chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) -> Unit,
    onSelectionModeChapterClick: (chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) -> Unit,
    onSelectionModeChapterLongClick: (chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) -> Unit,
    onChapterDownload: (chapter: my.noveldokusha.tooling.local_database.ChapterWithContext) -> Unit,
    onPullRefresh: () -> Unit,
    onCoverLongClick: () -> Unit,
    onChangeCover: () -> Unit,
) {
    val context by rememberUpdatedState(newValue = LocalContext.current)
    var showDropDown by rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()

    if (state.isInSelectionMode.value) BackHandler {
        onCloseSelectionBar()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            val isAtTop by lazyListState.isAtTop(threshold = 40.dp)
            val alpha by animateFloatAsState(targetValue = if (isAtTop) 0f else 1f, label = "")
            val backgroundColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.background.copy(alpha = alpha),
                label = ""
            )
            val titleColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha),
                label = ""
            )
            Surface(color = backgroundColor) {
                Column {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Unspecified,
                            scrolledContainerColor = Color.Unspecified,
                        ),
                        title = {
                            Text(
                                text = state.book.value.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = titleColor
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onPressBack
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onLibraryToggle
                            ) {
                                Icon(
                                    if (state.book.value.inLibrary) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                    stringResource(R.string.open_the_web_view),
                                    tint = ColorLike
                                )
                            }
                            IconButton(
                                onClick = { showBottomSheet = !showBottomSheet }
                            ) {
                                Icon(
                                    Icons.Filled.FilterList,
                                    stringResource(R.string.filter),
                                    tint = ColorNotice
                                )
                            }
                            IconButton(onClick = { showDropDown = !showDropDown }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    stringResource(R.string.options_panel)
                                )
                                DropdownMenu(
                                    expanded = showDropDown,
                                    onDismissRequest = { showDropDown = false }) {
                                    ChaptersDropDown(
                                        isLocalSource = state.isLocalSource.value,
                                        openInBrowser = {
                                            if (!state.book.value.url.isLocalUri) {
                                                context.goToWebBrowser(url = state.book.value.url)
                                            }
                                        },
                                        onSearchBookInDatabase = onSearchBookInDatabase,
                                        onResumeReading = onResumeReading,
                                        onChangeCover = onChangeCover,
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(Modifier.alpha(alpha))
                }
            }
            AnimatedVisibility(
                visible = state.isInSelectionMode.value,
                enter = expandVertically(initialHeight = { it / 2 }, expandFrom = Alignment.Top)
                        + fadeIn(),
                exit = shrinkVertically(targetHeight = { it / 2 }, shrinkTowards = Alignment.Top)
                        + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorApp.tintedSurface) {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Unspecified,
                            scrolledContainerColor = Color.Unspecified,
                        ),
                        title = {
                            Text(
                                text = state.selectedChaptersUrl.size.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onCloseSelectionBar) {
                                Icon(
                                    Icons.Outlined.Close,
                                    stringResource(id = R.string.close_selection_bar)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSelectAllChapters) {
                                Icon(
                                    Icons.Outlined.SelectAll,
                                    stringResource(id = R.string.select_all_chapters)
                                )
                            }
                            IconButton(onClick = onSelectedInvertSelection) {
                                Icon(
                                    Icons.Outlined.PublishedWithChanges,
                                    stringResource(id = R.string.close_selection_bar)
                                )
                            }
                        }
                    )
                }
            }
        },
        content = { innerPadding ->
            ChaptersScreenBody(
                state = state,
                innerPadding = innerPadding,
                lazyListState = lazyListState,
                onChapterClick = if (state.isInSelectionMode.value) onSelectionModeChapterClick else onChapterClick,
                onChapterLongClick = if (state.isInSelectionMode.value) onSelectionModeChapterLongClick else onChapterLongClick,
                onChapterDownload = onChapterDownload,
                onPullRefresh = onPullRefresh,
                onCoverLongClick = onCoverLongClick,
            )
            Box(Modifier.padding(innerPadding)) {
                InternalLazyColumnScrollbar(state = lazyListState)
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isInSelectionMode.value,
                enter = expandVertically(initialHeight = { it / 2 }) + fadeIn(),
                exit = shrinkVertically(targetHeight = { it / 2 }) + fadeOut(),
            ) {
                BottomAppBar(
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    containerColor = MaterialTheme.colorApp.tintedSurface,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        if (!state.isLocalSource.value) {
                            IconButton(onClick = onSelectedDeleteDownloads) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    stringResource(id = R.string.remove_selected_chapters_dowloads)
                                )
                            }
                            IconButton(onClick = onSelectedDownload) {
                                Icon(
                                    Icons.Outlined.CloudDownload,
                                    stringResource(id = R.string.download_selected_chapters)
                                )
                            }
                        }
                        IconButton(onClick = onSelectedSetRead) {
                            Icon(
                                Icons.Filled.Done,
                                stringResource(id = R.string.set_as_read_selected_chapters)
                            )
                        }
                        IconButton(onClick = onSelectedSetUnread) {
                            Icon(
                                Icons.Filled.RemoveDone,
                                stringResource(id = R.string.set_as_not_read_selected_chapters)
                            )
                        }
                    }
                }

            }
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = ColorAccent,
                onClick = onResumeReading
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.textPadding()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.open_last_read_chapter),
                        tint = Color.White
                    )
                    AnimatedVisibility(visible = lazyListState.isAtTop(threshold = 100.dp).value) {
                        Text(
                            text = stringResource(id = R.string.read),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    )

    ChaptersBottomSheet(
        visible = showBottomSheet,
        onDismiss = { showBottomSheet = false },
        state = state
    )
}