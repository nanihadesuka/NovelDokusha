package my.noveldokusha.ui.screens.main.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.composeViews.BookSettingsDialog
import my.noveldokusha.ui.composeViews.BookSettingsDialogState
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryModel: LibraryViewModel = viewModel()
) {
    val context by rememberUpdatedState(LocalContext.current)
    var showDropDown by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Unspecified,
                    scrolledContainerColor = Color.Unspecified,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { libraryModel.showBottomSheet = !libraryModel.showBottomSheet }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_filter_list_24),
                            contentDescription = stringResource(R.string.options_panel)
                        )
                    }
                    IconButton(onClick = { showDropDown = !showDropDown }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.open_for_more_options)
                        )
                        LibraryDropDown(
                            expanded = showDropDown,
                            onDismiss = { showDropDown = false }
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            LibraryScreenBody(
                tabs = listOf("Default", "Completed"),
                innerPadding = innerPadding,
                topAppBarState = scrollBehavior.state,
                onBookClick = { book ->
                    val intent = ChaptersActivity.IntentData(
                        context,
                        bookMetadata = BookMetadata(title = book.book.title, url = book.book.url)
                    )
                    context.startActivity(intent)
                },
                onBookLongClick = {
                    libraryModel.bookSettingsDialogState = BookSettingsDialogState.Show(it.book)
                }
            )
        }
    )

    // Book selected options dialog
    when (val state = libraryModel.bookSettingsDialogState) {
        is BookSettingsDialogState.Show -> {
            BookSettingsDialog(
                currentBook = state.book,
                onDismiss = { libraryModel.bookSettingsDialogState = BookSettingsDialogState.Hide },
            )
        }

        else -> Unit
    }

    LibraryBottomSheet(
        visible = libraryModel.showBottomSheet,
        onDismiss = { libraryModel.showBottomSheet = false }
    )
}
