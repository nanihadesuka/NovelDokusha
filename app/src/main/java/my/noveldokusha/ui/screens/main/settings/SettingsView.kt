package my.noveldokusha.ui.screens.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.composableActions.onDoBackup
import my.noveldokusha.composableActions.onDoRestore

@Composable
fun SettingsView()
{
    val viewModel = viewModel<SettingsViewModel>()
    Column {
        ToolbarMain(title = stringResource(id = R.string.app_name))
        SettingsBody(
            currentFollowSystem = viewModel.followsSystem,
            currentTheme = viewModel.theme,
            onFollowSystem = viewModel::onFollowSystem,
            onThemeSelected = viewModel::onThemeSelected,
            databaseSize = viewModel.databaseSize,
            imagesFolderSize = viewModel.imageFolderSize,
            translationModelsStates = viewModel.translationManager.models,
            isTranslationSettingsVisible = viewModel.translationManager.available,
            onCleanDatabase = viewModel::cleanDatabase,
            onCleanImageFolder = viewModel::cleanImagesFolder,
            onBackupData = onDoBackup(),
            onRestoreData = onDoRestore(),
            onDownloadTranslationModel = viewModel.translationManager::downloadModel,
            onRemoveTranslationModel = viewModel.translationManager::removeModel
        )
    }
}

