package my.noveldokusha.ui.screens.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )
    val context by rememberUpdatedState(LocalContext.current)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                CollapsibleDivider(scrollBehavior.state)
            }
        },
        content = { innerPadding ->
            SettingsScreenBody(
                state = viewModel.state,
                onFollowSystem = viewModel::onFollowSystemChange,
                onThemeSelected = viewModel::onThemeChange,
                onCleanDatabase = viewModel::cleanDatabase,
                onCleanImageFolder = viewModel::cleanImagesFolder,
                onBackupData = onDoBackup(),
                onRestoreData = onDoRestore(),
                onDownloadTranslationModel = viewModel::downloadTranslationModel,
                onRemoveTranslationModel = viewModel::removeTranslationModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    )
}

