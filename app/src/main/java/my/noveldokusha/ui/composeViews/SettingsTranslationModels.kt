package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.textPadding

@Composable
fun SettingsTranslationModels(
    translationModelsStates: List<TranslationModelState>,
    onDownloadTranslationModel: (lang: String) -> Unit,
    onRemoveTranslationModel: (lang: String) -> Unit,
) {
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(R.string.settings_title_translation_models),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.open_translation_models_manager))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.settings_translations_models_main_description))
            },
            leadingContent = {
                Icon(Icons.Outlined.Translate, null, tint = MaterialTheme.colorScheme.onPrimary)
            },
            modifier = Modifier.clickable { isDialogVisible = true }
        )
    }
    SettingsTranslationModelsDialog(
        translationModelsStates = translationModelsStates,
        onDownloadTranslationModel = onDownloadTranslationModel,
        onRemoveTranslationModel = onRemoveTranslationModel,
        visible = isDialogVisible,
        setVisible = { isDialogVisible = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTranslationModelsDialog(
    translationModelsStates: List<TranslationModelState>,
    onDownloadTranslationModel: (lang: String) -> Unit,
    onRemoveTranslationModel: (lang: String) -> Unit,
    visible: Boolean,
    setVisible: (Boolean) -> Unit,
) {
    val state = rememberLazyListState()
    if (visible) BasicAlertDialog(onDismissRequest = { setVisible(false) }
    ) {
        Card {
            LazyColumn(
                state = state,
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(translationModelsStates) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = it.locale.displayLanguage,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .widthIn(min = 22.dp)
                                .height(22.dp)
                        ) {
                            when {
                                it.available -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Done,
                                            contentDescription = null
                                        )
                                        IconButton(
                                            onClick = { onRemoveTranslationModel(it.language) },
                                            enabled = it.language != "en"
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }

                                it.downloading -> IconButton(onClick = { }, enabled = false) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                else -> IconButton(
                                    onClick = { onDownloadTranslationModel(it.language) }) {
                                    Icon(
                                        Icons.Filled.CloudDownload,
                                        contentDescription = null,
                                        tint = if (it.downloadingFailed) Color.Red
                                        else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
