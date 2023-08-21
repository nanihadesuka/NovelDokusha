package my.noveldokusha.ui.screens.main.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.DoubleArrow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.domain.AppVersion
import my.noveldokusha.domain.RemoteAppVersion
import my.noveldokusha.ui.composeViews.SettingsTranslationModels
import my.noveldokusha.ui.screens.main.settings.views.NewAppUpdateDialog
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.PreviewThemes
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.debouncedClickable
import my.noveldokusha.utils.textPadding

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTheme(
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
                    Themes.list.forEach {
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

@Composable
private fun SettingsData(
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

@Composable
private fun SettingsBackup(
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

@Composable
fun SettingsScreenBody(
    state: SettingsScreenState,
    modifier: Modifier = Modifier,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    onDownloadTranslationModel: (lang: String) -> Unit,
    onRemoveTranslationModel: (lang: String) -> Unit,
    onCheckForUpdatesManual: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        SettingsTheme(
            currentFollowSystem = state.followsSystemTheme.value,
            currentTheme = state.currentTheme.value,
            onFollowSystemChange = onFollowSystem,
            onCurrentThemeChange = onThemeSelected
        )
        Divider()
        SettingsData(
            databaseSize = state.databaseSize.value,
            imagesFolderSize = state.imageFolderSize.value,
            onCleanDatabase = onCleanDatabase,
            onCleanImageFolder = onCleanImageFolder
        )
        Divider()
        SettingsBackup(
            onBackupData = onBackupData,
            onRestoreData = onRestoreData
        )
        if (state.isTranslationSettingsVisible.value) {
            Divider()
            SettingsTranslationModels(
                translationModelsStates = state.translationModelsStates,
                onDownloadTranslationModel = onDownloadTranslationModel,
                onRemoveTranslationModel = onRemoveTranslationModel
            )
        }
        Divider()
        LibraryAutoUpdate(state = state.libraryAutoUpdate)
        Divider()
        AppUpdates(
            state = state.updateAppSetting,
            onCheckForUpdatesManual = onCheckForUpdatesManual
        )
        Spacer(modifier = Modifier.height(500.dp))
        Text(
            text = "(°.°)",
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun AppUpdates(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

private data class UpdateIntervalTimes(
    val intervalHours: Int,
    @StringRes val nameRes: Int
)

private val libraryUpdateTimes = listOf(
    UpdateIntervalTimes(intervalHours = 6, nameRes = R.string.library_update_interval_6_h),
    UpdateIntervalTimes(intervalHours = 12, nameRes = R.string.library_update_interval_12_h),
    UpdateIntervalTimes(intervalHours = 24, nameRes = R.string.library_update_interval_1_day),
    UpdateIntervalTimes(intervalHours = 24 * 2, nameRes = R.string.library_update_interval_2_days),
)


@PreviewThemes
@Composable
private fun Preview() {
    val isDark = isSystemInDarkTheme()
    val theme = remember { mutableStateOf(if (isDark) Themes.DARK else Themes.LIGHT) }
    InternalTheme(theme.value) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreenBody(
                state = SettingsScreenState(
                    followsSystemTheme = remember { derivedStateOf { true } },
                    currentTheme = theme,
                    databaseSize = remember { mutableStateOf("1 MB") },
                    imageFolderSize = remember { mutableStateOf("10 MB") },
                    isTranslationSettingsVisible = remember { mutableStateOf(true) },
                    translationModelsStates = remember { mutableStateListOf() },
                    updateAppSetting = SettingsScreenState.UpdateApp(
                        currentAppVersion = "1.0.0",
                        appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                        showNewVersionDialog = remember {
                            mutableStateOf(
                                if (true) null else RemoteAppVersion(
                                    version = AppVersion(1, 1, 1),
                                    sourceUrl = "url"
                                )
                            )
                        },
                        checkingForNewVersion = remember { mutableStateOf(true) },
                    ),
                    libraryAutoUpdate = SettingsScreenState.LibraryAutoUpdate(
                        autoUpdateEnabled = remember { mutableStateOf(true) },
                        autoUpdateIntervalHours = remember { mutableStateOf(24) },
                    )
                ),
                onFollowSystem = { },
                onThemeSelected = { },
                onCleanDatabase = { },
                onCleanImageFolder = { },
                onBackupData = { },
                onRestoreData = { },
                onDownloadTranslationModel = { },
                onRemoveTranslationModel = { },
                onCheckForUpdatesManual = { },
            )
        }
    }
}
