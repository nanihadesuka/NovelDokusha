package my.noveldokusha.features.main.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.domain.RemoteAppVersion
import my.noveldokusha.tooling.text_translator.domain.TranslationModelState
import my.noveldokusha.ui.theme.Themes

data class SettingsScreenState(
    val databaseSize: MutableState<String>,
    val imageFolderSize: MutableState<String>,
    val followsSystemTheme: State<Boolean>,
    val currentTheme: State<Themes>,
    val isTranslationSettingsVisible: State<Boolean>,
    val translationModelsStates: SnapshotStateList<my.noveldokusha.tooling.text_translator.domain.TranslationModelState>,
    val updateAppSetting: UpdateApp,
    val libraryAutoUpdate: LibraryAutoUpdate,
) {
    data class UpdateApp(
        val currentAppVersion: String,
        val appUpdateCheckerEnabled: MutableState<Boolean>,
        val showNewVersionDialog: MutableState<RemoteAppVersion?>,
        val checkingForNewVersion: MutableState<Boolean>,
    )

    data class LibraryAutoUpdate(
        val autoUpdateEnabled: MutableState<Boolean>,
        val autoUpdateIntervalHours: MutableState<Int>,
    )
}