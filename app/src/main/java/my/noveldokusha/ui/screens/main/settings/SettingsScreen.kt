package my.noveldokusha.ui.screens.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.composableActions.onDoBackup
import my.noveldokusha.composableActions.onDoRestore
import my.noveldokusha.ui.composeViews.CollapsibleDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(snapAnimationSpec = null)
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Unspecified,
                        scrolledContainerColor = Color.Unspecified,
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_settings),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                )
                CollapsibleDivider(scrollState)
            }
        },
        content = { innerPadding ->
            SettingsScreenBody(
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
                onRemoveTranslationModel = viewModel.translationManager::removeModel,
                modifier = Modifier.padding(innerPadding),
                scrollState = scrollState
            )
        }
    )
}

