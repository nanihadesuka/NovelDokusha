package my.noveldokusha.ui.screens.main.library

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.composableActions.onDoImportEPUB
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.main.OptionsDialogBookLibrary

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryView() {
    val context by rememberUpdatedState(LocalContext.current)
    val title = stringResource(id = R.string.app_name)
    val downDownExpanded = remember { mutableStateOf(false) }
    val libraryModel = viewModel<LibraryViewModel>()
    val coroutine = rememberCoroutineScope()

    Column {
        ToolbarMain(
            title = title,
            onOpenBottomSheetOptionsPress = {
                coroutine.launch { libraryModel.bottomSheetState.show() }
            },
            onOptionsDropDownPress = { downDownExpanded.value = !downDownExpanded.value },
            onOptionsDropDownView = { OptionsDropDownView(downDownExpanded) }
        )
        LibraryBody(
            tabs = listOf("Default", "Completed"),
            onBookClick = context::goToBookChaptersPage,
            onBookLongClick = {
                libraryModel.optionsBookDialogState = OptionsBookDialogState.Show(it.book)
            }
        )
    }

    // Book selected options dialog
    when (val state = libraryModel.optionsBookDialogState) {
        is OptionsBookDialogState.Show -> Dialog(
            onDismissRequest = {
                libraryModel.optionsBookDialogState = OptionsBookDialogState.Hide
            },
            content = {
                var book by remember { mutableStateOf(state.book) }
                LaunchedEffect(Unit) {
                    libraryModel
                        .getBook(state.book.url)
                        .filterNotNull()
                        .collect { book = it }
                }
                OptionsDialogBookLibrary(book = book)
            }
        )
        else -> Unit
    }
}

@Composable
fun OptionsDropDownView(expanded: MutableState<Boolean>) {
    DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
        Text(
            text = stringResource(id = R.string.import_epub),
            modifier = Modifier
                .clickable(onClick = onDoImportEPUB())
                .padding(16.dp)
        )
    }
}

private fun Context.goToBookChaptersPage(book: BookWithContext) {
    ChaptersActivity.IntentData(
        this,
        bookMetadata = BookMetadata(title = book.book.title, url = book.book.url)
    ).let(::startActivity)
}


