package my.noveldokusha.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.settings.R

@Composable
internal fun SettingsData(
    databaseSize: String,
    imagesFolderSize: String,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.data),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.clean_database))
            },
            supportingContent = {
                Column {
                    Text(text = stringResource(id = R.string.size) + " " + databaseSize)
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.DataArray, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onCleanDatabase() }
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.clean_images_folder))
            },
            supportingContent = {
                Column {
                    Text(text = stringResource(id = R.string.preserve_only_images_from_library_books))
                    Text(text = stringResource(id = R.string.size) + " " + imagesFolderSize)
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onCleanImageFolder() }
        )
    }
}