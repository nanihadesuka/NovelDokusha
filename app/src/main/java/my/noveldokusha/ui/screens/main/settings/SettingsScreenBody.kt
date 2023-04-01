package my.noveldokusha.ui.screens.main.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.ui.composeViews.ClickableOption
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.ui.composeViews.Section
import my.noveldokusha.ui.composeViews.SettingsTranslationModels
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.PreviewThemes
import my.noveldokusha.ui.theme.Themes

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsTheme(
    currentTheme: Themes,
    currentFollowSystem: Boolean,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
) {
    Section(title = stringResource(id = R.string.theme)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {

                val textColor = when (currentFollowSystem) {
                    true -> Color.White
                    false -> MaterialTheme.colorScheme.onPrimary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(0.dp)
                        .clip(CircleShape)
                        .toggleable(
                            value = currentFollowSystem,
                            onValueChange = { onFollowSystem(!currentFollowSystem) }
                        )
                        .background(
                            if (currentFollowSystem) ColorAccent
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                        )
                        .padding(8.dp)
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.follow_system),
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    AnimatedContent(targetState = currentFollowSystem, label = "") { follow ->
                        Icon(
                            if (follow) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = textColor
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val lines = Themes.pairs.toList().chunked(2)
                for (line in lines) Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((theme, themeName) in line) MyButton(
                        text = themeName,
                        onClick = { onThemeSelected(theme) },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        outerPadding = 0.dp,
                        selected = currentTheme == theme,
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsData(
    databaseSize: String,
    imagesFolderSize: String,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit
) {
    Section(title = stringResource(id = R.string.data)) {
        ClickableOption(
            title = stringResource(id = R.string.clean_database),
            subtitle = stringResource(id = R.string.preserve_only_library_books_data),
            info = stringResource(id = R.string.database_size) + " " + databaseSize,
            onClick = onCleanDatabase
        )
        ClickableOption(
            title = stringResource(id = R.string.clean_images_folder),
            subtitle = stringResource(id = R.string.preserve_only_images_from_library_books),
            info = stringResource(id = R.string.images_total_size) + " " + imagesFolderSize,
            onClick = onCleanImageFolder
        )
    }
}

@Composable
private fun SettingsBackup(
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
) {
    Section(title = stringResource(id = R.string.backup)) {
        ClickableOption(
            title = stringResource(id = R.string.backup_data),
            subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_saving_location),
            onClick = onBackupData
        )
        ClickableOption(
            title = stringResource(id = R.string.restore_data),
            subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_file),
            onClick = onRestoreData
        )
    }
}

@Composable
fun SettingsScreenBody(
    currentFollowSystem: Boolean,
    currentTheme: Themes,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    databaseSize: String,
    imagesFolderSize: String,
    isTranslationSettingsVisible: Boolean,
    translationModelsStates: List<TranslationModelState>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    onDownloadTranslationModel: (lang: String) -> Unit,
    onRemoveTranslationModel: (lang: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(0.dp))
        SettingsTheme(
            currentFollowSystem = currentFollowSystem,
            currentTheme = currentTheme,
            onFollowSystem = onFollowSystem,
            onThemeSelected = onThemeSelected
        )
        SettingsData(
            databaseSize = databaseSize,
            imagesFolderSize = imagesFolderSize,
            onCleanDatabase = onCleanDatabase,
            onCleanImageFolder = onCleanImageFolder
        )
        SettingsBackup(
            onBackupData = onBackupData,
            onRestoreData = onRestoreData
        )
        if (isTranslationSettingsVisible) SettingsTranslationModels(
            translationModelsStates = translationModelsStates,
            onDownloadTranslationModel = onDownloadTranslationModel,
            onRemoveTranslationModel = onRemoveTranslationModel
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

@PreviewThemes
@Composable
private fun Preview() {
    InternalTheme {
        SettingsScreenBody(
            currentFollowSystem = true,
            currentTheme = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT,
            onFollowSystem = { },
            onThemeSelected = { },
            databaseSize = "1 MB",
            imagesFolderSize = "10 MB",
            isTranslationSettingsVisible = true,
            translationModelsStates = listOf(),
            onCleanDatabase = { },
            onCleanImageFolder = { },
            onBackupData = { },
            onRestoreData = { },
            onDownloadTranslationModel = {},
            onRemoveTranslationModel = {},
            scrollState = rememberScrollState()
        )
    }
}