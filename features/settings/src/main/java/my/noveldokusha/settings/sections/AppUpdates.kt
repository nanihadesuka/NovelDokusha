package my.noveldokusha.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.DoubleArrow
import androidx.compose.material3.CircularProgressIndicator
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
import my.noveldoksuha.coreui.theme.debouncedClickable
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState
import my.noveldokusha.settings.views.NewAppUpdateDialog

@Composable
internal fun AppUpdates(
    state: SettingsScreenState.UpdateApp,
    onCheckForUpdatesManual: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.app_updates) + " | " + state.currentAppVersion,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            modifier = Modifier.clickable {
                state.appUpdateCheckerEnabled.value = !state.appUpdateCheckerEnabled.value
            },
            headlineContent = {
                Text(text = stringResource(R.string.automatically_check_for_app_updates))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.AutoMode,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.appUpdateCheckerEnabled.value,
                    onCheckedChange = {
                        state.appUpdateCheckerEnabled.value = !state.appUpdateCheckerEnabled.value
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorAccent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.check_for_app_updates_now))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.DoubleArrow,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            trailingContent = {
                AnimatedVisibility(visible = state.checkingForNewVersion.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            modifier = Modifier.debouncedClickable { onCheckForUpdatesManual() }
        )
        NewAppUpdateDialog(
            updateApp = state
        )
    }
}