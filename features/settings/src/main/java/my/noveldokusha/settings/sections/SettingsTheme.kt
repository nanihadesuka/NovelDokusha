package my.noveldokusha.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.Themes
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.settings.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsTheme(
    currentTheme: Themes,
    currentFollowSystem: Boolean,
    onFollowSystemChange: (Boolean) -> Unit,
    onCurrentThemeChange: (Themes) -> Unit,
) {
    Column {
        Text(
            text = stringResource(id = R.string.theme),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        // Follow system theme
        ListItem(
            modifier = Modifier
                .clickable { onFollowSystemChange(!currentFollowSystem) },
            headlineContent = {
                Text(text = stringResource(id = R.string.follow_system))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = currentFollowSystem,
                    onCheckedChange = onFollowSystemChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorAccent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Themes
        ListItem(
            headlineContent = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Themes.entries.forEach {
                        FilterChip(
                            selected = it == currentTheme,
                            onClick = { onCurrentThemeChange(it) },
                            label = { Text(text = stringResource(id = it.nameId)) }
                        )
                    }
                }

            },
            leadingContent = {
                Icon(Icons.Outlined.ColorLens, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        )
    }
}