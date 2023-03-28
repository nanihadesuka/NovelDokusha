package my.noveldokusha.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.rememberResolvedBookImagePath
import my.noveldokusha.ui.composeViews.ImageView
import my.noveldokusha.ui.screens.main.library.LibraryViewModel
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.Success500

sealed interface BookSettingsDialogState {
    object Hide : BookSettingsDialogState
    data class Show(val book: Book) : BookSettingsDialogState
}

@Composable
fun BookSettingsDialog(
    currentBook: Book,
    onDismiss: () -> Unit,
    model: LibraryViewModel = viewModel()
) {
    val book by model.getBook(currentBook.url).collectAsState(initial = currentBook)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val image by rememberResolvedBookImagePath(
                bookUrl = book.url,
                imagePath = book.coverImageUrl
            )
            ImageView(
                imageModel = image,
                error = R.drawable.default_book_cover,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(1 / 1.45f)
                    .clip(ImageBorderShape)
            )
        },
        title = {
            Text(text = book.title)
        },
        confirmButton = {},
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { model.bookCompletedToggle(book.url) }
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Checkbox(
                    checked = book.completed,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Success500,
                        checkmarkColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                )
                Text(
                    text = stringResource(R.string.completed),
                )
            }
        }
    )
}