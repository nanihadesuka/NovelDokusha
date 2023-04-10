package my.noveldokusha.ui.screens.reader.settingDialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import my.noveldokusha.R
import my.noveldokusha.ui.screens.reader.features.LiveTranslationSettingData
import my.noveldokusha.utils.clickableWithUnboundedIndicator
import my.noveldokusha.utils.ifCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorSettingDialog(
    state: LiveTranslationSettingData
) {
    var modelSelectorExpanded by rememberSaveable { mutableStateOf(false) }
    var modelSelectorExpandedForTarget by rememberSaveable { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(Size.Zero) }
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    rowSize = layoutCoordinates.size.toSize()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.enable.value,
                    label = {
                        Text(text = stringResource(R.string.translate))
                    },
                    onClick = { state.onEnable(!state.enable.value) },
                )
                AnimatedVisibility(visible = state.enable.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clickableWithUnboundedIndicator {
                                    modelSelectorExpanded = !modelSelectorExpanded
                                    modelSelectorExpandedForTarget = false
                                }
                        ) {
                            Text(
                                text = state.source.value?.locale?.displayLanguage
                                    ?: stringResource(R.string.language_source_empty_text),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .ifCase(state.source.value == null) { alpha(0.5f) },
                            )
                        }
                        Icon(Icons.Default.ArrowRightAlt, contentDescription = null)
                        Box(
                            modifier = Modifier
                                .clickableWithUnboundedIndicator {
                                    modelSelectorExpanded = !modelSelectorExpanded
                                    modelSelectorExpandedForTarget = true
                                }
                        ) {
                            Text(
                                text = state.target.value?.locale?.displayLanguage
                                    ?: stringResource(R.string.language_target_empty_text),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .ifCase(state.target.value == null) { alpha(0.5f) },
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = modelSelectorExpanded,
                onDismissRequest = { modelSelectorExpanded = false },
                offset = DpOffset(0.dp, 10.dp),
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .width(with(LocalDensity.current) { rowSize.width.toDp() })
            ) {

                DropdownMenuItem(
                    onClick = {
                        if (modelSelectorExpandedForTarget) state.onTargetChange(null)
                        else state.onSourceChange(null)
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.language_clear_selection),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )

                Divider()

                state.listOfAvailableModels.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            if (modelSelectorExpandedForTarget) state.onTargetChange(item)
                            else state.onSourceChange(item)
                            modelSelectorExpanded = false
                        },
                        enabled = item.available,
                        trailingIcon = {
                            when {
                                item.downloadingFailed -> IconButton(
                                    onClick = { state.onDownloadTranslationModel(item.language) },
                                ) {
                                    Icon(
                                        Icons.Outlined.CloudDownload,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                item.downloading -> IconButton(
                                    onClick = { },
                                    enabled = false
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                item.available -> IconButton(
                                    onClick = { state.onDownloadTranslationModel(item.language) },
                                ) {
                                    Icon(Icons.Filled.CloudDownload, null)
                                }
                                else -> IconButton(
                                    onClick = { state.onDownloadTranslationModel(item.language) },
                                ) {
                                    Icon(Icons.Outlined.CloudDownload, null)
                                }
                            }
                        },
                        text = {
                            Text(text = item.locale.displayLanguage)
                        }
                    )
                }
            }
        }
    }
}