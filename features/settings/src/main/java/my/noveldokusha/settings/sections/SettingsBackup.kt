package my.noveldokusha.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
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
internal fun SettingsBackup(
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
) {
    Column {
        Text(
            text = stringResource(id = R.string.backup),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.backup_data))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_saving_location))
            },
            leadingContent = {
                Icon(Icons.Outlined.Save, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { onBackupData() }
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.restore_data))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_file))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.SettingsBackupRestore,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.clickable { onRestoreData() }
        )
    }
}