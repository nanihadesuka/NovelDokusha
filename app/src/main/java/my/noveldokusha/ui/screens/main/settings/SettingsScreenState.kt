package my.noveldokusha.ui.screens.main.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.ui.theme.Themes

data class SettingsScreenState(
    val databaseSize: MutableState<String>,
    val imageFolderSize: MutableState<String>,
    val followsSystemTheme: State<Boolean>,
    val currentTheme: State<Themes>,
    val isTranslationSettingsVisible: State<Boolean>,
    val translationModelsStates: SnapshotStateList<TranslationModelState>,
)