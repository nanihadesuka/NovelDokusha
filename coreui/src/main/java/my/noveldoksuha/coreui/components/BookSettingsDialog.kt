package my.noveldoksuha.coreui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import my.noveldoksuha.coreui.R
import my.noveldoksuha.coreui.theme.ImageBorderShape
import my.noveldoksuha.coreui.theme.colorApp
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.tooling.local_database.tables.Book


sealed interface BookSettingsDialogState : Parcelable {
    @Parcelize
    data object Hide : BookSettingsDialogState

    @Parcelize
    data class Show(val book: Book) :
        BookSettingsDialogState
}

@Composable
fun BookSettingsDialog(
    book: Book,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
) {
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
                    .clickable(onClick = onToggleCompleted)
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