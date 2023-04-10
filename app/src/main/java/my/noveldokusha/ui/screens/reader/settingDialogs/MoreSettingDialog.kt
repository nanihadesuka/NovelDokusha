package my.noveldokusha.ui.screens.reader.settingDialogs

import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent

@Composable
fun MoreSettingDialog(
    allowTextSelection: Boolean,
    onAllowTextSelectionChange: (Boolean) -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        // Allow text selection
        ListItem(
            modifier = Modifier
                .clickable { onAllowTextSelectionChange(!allowTextSelection) },
            headlineContent = {
                Text(text = stringResource(id = R.string.allow_text_selection))
            },
            trailingContent = {
                Switch(
                    checked = allowTextSelection,
                    onCheckedChange = onAllowTextSelectionChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorAccent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
    }
}