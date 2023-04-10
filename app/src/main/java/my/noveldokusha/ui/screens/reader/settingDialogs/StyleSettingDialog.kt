package my.noveldokusha.ui.screens.reader.settingDialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import my.noveldokusha.R
import my.noveldokusha.ui.composeViews.MySlider
import my.noveldokusha.ui.screens.reader.ReaderScreenState
import my.noveldokusha.ui.screens.reader.tools.FontsLoader
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.Themes

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingDialog(
    state: ReaderScreenState.Settings.StyleSettingsData,
    onTextSizeChange: (Float) -> Unit,
    onTextFontChange: (String) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    onThemeSelectedChange: (Themes) -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        // Text size
        var currentTextSize by remember { mutableStateOf(state.textSize.value) }
        MySlider(
            value = currentTextSize,
            valueRange = 8f..24f,
            onValueChange = {
                currentTextSize = it
                onTextSizeChange(currentTextSize)
            },
            text = stringResource(R.string.text_size) + ": %.2f".format(currentTextSize),
            modifier = Modifier.padding(16.dp)
        )
        // Text font
        Box {
            var showFontsDropdown by rememberSaveable { mutableStateOf(false) }
            val fontLoader = remember { FontsLoader() }
            var rowSize by remember { mutableStateOf(Size.Zero) }
            ListItem(
                modifier = Modifier
                    .clickable { showFontsDropdown = !showFontsDropdown }
                    .onGloballyPositioned { rowSize = it.size.toSize() },
                headlineContent = {
                    Text(
                        text = state.textFont.value,
                        fontFamily = fontLoader.getFontFamily(state.textFont.value),
                    )
                },
                leadingContent = { Icon(Icons.Default.TextFields, null) }
            )
            DropdownMenu(
                expanded = showFontsDropdown,
                onDismissRequest = { showFontsDropdown = false },
                offset = DpOffset(0.dp, 10.dp),
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .width(with(LocalDensity.current) { rowSize.width.toDp() })
            ) {
                FontsLoader.availableFonts.forEach { item ->
                    DropdownMenuItem(
                        onClick = { onTextFontChange(item) },
                        text = {
                            Text(
                                text = item,
                                fontFamily = fontLoader.getFontFamily(item),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
            }
        }
        // Follow system theme
        ListItem(
            modifier = Modifier
                .clickable { onFollowSystemChange(!state.followSystem.value) },
            headlineContent = {
                Text(text = stringResource(id = R.string.follow_system))
            },
            leadingContent = {
                Switch(
                    checked = state.followSystem.value,
                    onCheckedChange = onFollowSystemChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorAccent,
                        checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
            }
        )
        // Themes
        FlowRow(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Themes.list.forEach {
                FilterChip(
                    selected = it == state.currentTheme.value,
                    onClick = { onThemeSelectedChange(it) },
                    label = { Text(text = stringResource(id = it.nameId)) }
                )
            }
        }
    }
}
