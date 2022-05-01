package my.noveldokusha.ui.main.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.composableActions.onDoBackup
import my.noveldokusha.composableActions.onDoRestore

@Composable
fun SettingsView(context: Context)
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
            onCleanDatabase = viewModel::cleanDatabase,
            onCleanImageFolder = viewModel::cleanImagesFolder,
            onBackupData = onDoBackup(),
            onRestoreData = onDoRestore(),
        )
    }
}

