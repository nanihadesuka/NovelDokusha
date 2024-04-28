package my.noveldokusha.ui.composeViews

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.parcelize.Parcelize
import my.noveldokusha.R
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.features.main.library.LibraryViewModel
import my.noveldokusha.ui.theme.ImageBorderShape
import my.noveldokusha.ui.theme.colorApp


sealed interface BookSettingsDialogState : Parcelable {
    @Parcelize
    object Hide : BookSettingsDialogState

    @Parcelize
    data class Show(val book: my.noveldokusha.feature.local_database.tables.Book) : BookSettingsDialogState
}

@Composable
fun BookSettingsDialog(
    currentBook: my.noveldokusha.feature.local_database.tables.Book,
    onDismiss: () -> Unit,
    model: LibraryViewModel = viewModel()
) {
    val book by model.getBook(currentBook.url).collectAsState(initial = currentBook)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ImageView(
                imageModel = rememberResolvedBookImagePath(
                    bookUrl = book.url,
                    imagePath = book.coverImageUrl
                ),
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
                        checkedColor = MaterialTheme.colorApp.checkboxPositive,
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