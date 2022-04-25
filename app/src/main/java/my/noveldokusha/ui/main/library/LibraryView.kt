package my.noveldokusha.ui.main.library

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.services.EpubImportService
import my.noveldokusha.services.LibraryUpdateService
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiUtils.toast
import my.noveldokusha.uiViews.MyButton

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryView(context: Context) = with(context)
{
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
            onBookClick = ::goToBookChaptersPage,
            onBookLongClick = ::openCompletedDialog
        )
    }
}


@Composable
fun OptionsDropDownView(expanded: MutableState<Boolean>)
{
    DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
        ImportEUPB {
            Text(
                text = stringResource(id = R.string.import_epub),
                modifier = Modifier
                    .clickable { it() }
                    .padding(16.dp)
            )
        }
    }
}

private fun Context.goToBookChaptersPage(book: BookWithContext)
{
    ChaptersActivity.IntentData(
        this,
        bookMetadata = BookMetadata(title = book.book.title, url = book.book.url)
    ).let(::startActivity)
}

private fun Context.openCompletedDialog(book: BookWithContext)
{
    completedDialog(this, book.book)
}

private fun completedDialog(ctx: Context, book: Book) = MaterialDialog(ctx).show {
    title(text = book.title)
    checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
    negativeButton(text = "Cancel")
    positiveButton(text = "Ok") {
        val completed = isCheckPromptChecked()
//        TODO()
//        viewModel.setBookAsCompleted(book, completed)
    }
}

@Composable
fun ImportEUPB(content: @Composable (onImport: () -> Unit) -> Unit)
{
    val context = LocalContext.current

    val launcherSelectEPUBFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { result ->
        val uri = result ?: return@rememberLauncherForActivityResult
        EpubImportService.start(context, uri)
    }

    val launcherPermisionToRead = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted)
            launcherSelectEPUBFile.launch("application/epub+zip")
    }

    content(
        onImport = {
            launcherPermisionToRead.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )
}
