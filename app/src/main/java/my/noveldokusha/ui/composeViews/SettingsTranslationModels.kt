package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.tools.TranslationModelState

@Composable
fun SettingsTranslationModels(
    translationModelsStates: List<TranslationModelState>,
    onDownloadTranslationModel: (lang: String) -> Unit,
    onRemoveTranslationModel: (lang: String) -> Unit,
) {
    Section(title = stringResource(R.string.settings_title_translation_models)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = stringResource(R.string.settings_translations_models_main_description),
                modifier = Modifier
                    .width(260.dp)
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            translationModelsStates.forEach {
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
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                            it.downloading -> IconButton(onClick = { }, enabled = false) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colors.onPrimary
                                )
                            }
                            else -> IconButton(
                                onClick = { onDownloadTranslationModel(it.language) }) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (it.downloadingFailed) Color.Red
                                    else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}