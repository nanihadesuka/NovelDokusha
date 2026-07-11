package my.noveldokusha.settings.sections

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.core.domain.AppVersion
import my.noveldokusha.core.domain.RemoteAppVersion
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState
import my.noveldokusha.settings.views.NewAppUpdateDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAutoUpdate(
    state: SettingsScreenState.LibraryAutoUpdate,
) {
    Column {
        Text(
            text = "Library updates",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            modifier = Modifier.clickable {
                state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value
            },
            headlineContent = {
                Text(text = stringResource(R.string.automatically_check_for_library_updates))
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
                    checked = state.autoUpdateEnabled.value,
                    onCheckedChange = {
                        state.autoUpdateEnabled.value = !state.autoUpdateEnabled.value
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorAccent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Library update interval options
        ListItem(
            headlineContent = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    libraryUpdateTimes.forEach {
                        FilterChip(
                            selected = it.intervalHours == state.autoUpdateIntervalHours.value,
                            onClick = { state.autoUpdateIntervalHours.value = it.intervalHours },
                            label = { Text(text = stringResource(id = it.nameRes)) }
                        )
                    }
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        )
    }
}

internal val libraryUpdateTimes = listOf(
    UpdateIntervalTimes(intervalHours = 6, nameRes = R.string.library_update_interval_6_h),
    UpdateIntervalTimes(intervalHours = 12, nameRes = R.string.library_update_interval_12_h),
    UpdateIntervalTimes(intervalHours = 24, nameRes = R.string.library_update_interval_1_day),
    UpdateIntervalTimes(intervalHours = 24 * 2, nameRes = R.string.library_update_interval_2_days),
)

internal data class UpdateIntervalTimes(
    val intervalHours: Int,
    @StringRes val nameRes: Int
)

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        NewAppUpdateDialog(
            updateApp = SettingsScreenState.UpdateApp(
                currentAppVersion = "1.2.3",
                appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                showNewVersionDialog = remember {
                    mutableStateOf(
                        RemoteAppVersion(
                            sourceUrl = "url",
                            version = AppVersion(1, 4, 5)
                        )
                    )
                },
                checkingForNewVersion = remember { mutableStateOf(true) }
            )
        )
    }
}
