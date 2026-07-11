package my.noveldokusha.features.reader.ui.settingDialogs

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
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import my.noveldoksuha.coreui.theme.clickableWithUnboundedIndicator
import my.noveldoksuha.coreui.theme.ifCase
import my.noveldokusha.features.reader.features.LiveTranslationSettingData
import my.noveldokusha.reader.R

@Composable
internal fun TranslatorSettingDialog(
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
                .padding(vertical = 4.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    rowSize = layoutCoordinates.size.toSize()
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Enable toggle ────────────────────────────────────────────
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    // ── Provider chip (tap to cycle, long-press for dropdown) ──
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                    ) {
                        AssistChip(
                            onClick = { state.onProviderChange() },
                            label = {
                                Text(
                                    text = state.activeProviderName.value,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = "Switch provider",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    // ── Source → Target language pickers ────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowRightAlt, contentDescription = null)
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
        }

        // ── Language dropdown ────────────────────────────────────────────
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
            HorizontalDivider()

            state.listOfAvailableModels.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        if (modelSelectorExpandedForTarget) state.onTargetChange(item)
                        else state.onSourceChange(item)
                        modelSelectorExpanded = false
                    },
                    enabled = true,
                    text = {
                        Text(text = item.locale.displayLanguage)
                    }
                )
            }
        }
    }
}
