package my.noveldokusha.settings.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import my.noveldokusha.text_translator.domain.TranslationModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTranslationModelsDialog(
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
                                        else LocalContentColor.current
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