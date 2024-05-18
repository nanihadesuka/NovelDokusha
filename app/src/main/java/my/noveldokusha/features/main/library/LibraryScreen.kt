package my.noveldokusha.features.main.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldoksuha.coreui.components.BookSettingsDialog
import my.noveldoksuha.coreui.components.BookSettingsDialogState
import my.noveldoksuha.coreui.theme.ColorNotice
import my.noveldokusha.R
import my.noveldokusha.features.chapterslist.ChaptersActivity
import my.noveldokusha.tooling.local_database.BookMetadata

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
                    IconButton(
                        onClick = { libraryModel.showBottomSheet = !libraryModel.showBottomSheet }
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            stringResource(R.string.filter),
                            tint = ColorNotice
                        )
                    }
                    IconButton(
                        onClick = { showDropDown = !showDropDown }
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            stringResource(R.string.options_panel)
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
                        bookMetadata = BookMetadata(
                            title = book.book.title,
                            url = book.book.url
                        )
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
                book = libraryModel.getBook(state.book.url)
                    .collectAsState(initial = state.book)
                    .value,
                onDismiss = { libraryModel.bookSettingsDialogState = BookSettingsDialogState.Hide },
                onToggleCompleted = { libraryModel.bookCompletedToggle(state.book.url) }
            )
        }

        else -> Unit
    }

    LibraryBottomSheet(
        visible = libraryModel.showBottomSheet,
        onDismiss = { libraryModel.showBottomSheet = false }
    )
}
