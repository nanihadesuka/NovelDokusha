package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import my.noveldokusha.ui.composeViews.RoundedContentLayout
import my.noveldokusha.ui.screens.reader.tools.FontsLoader
import my.noveldokusha.ui.theme.selectableMinHeight

@Composable
fun TextFontSetting(
    textFont: String,
    onTextFontChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rowSize by remember { mutableStateOf(Size.Zero) }
    var expanded by remember { mutableStateOf(false) }
    val fontLoader = FontsLoader()
    RoundedContentLayout(
        modifier
            .clickable { expanded = true }
            .height(selectableMinHeight)
            .onGloballyPositioned { layoutCoordinates ->
                rowSize = layoutCoordinates.size.toSize()
            }
    ) {
        Icon(
            imageVector = Icons.Default.FontDownload,
            contentDescription = null,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = textFont,
            style = MaterialTheme.typography.h6,
            fontFamily = fontLoader.getFontFamily(textFont),
            modifier = Modifier
                .padding(end = 50.dp)
                .weight(1f),
            textAlign = TextAlign.Center
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 10.dp),
            modifier = Modifier
                .heightIn(max = 300.dp)
                .width(with(LocalDensity.current) { rowSize.width.toDp() })
        ) {
            FontsLoader.availableFonts.forEach { item ->
                DropdownMenuItem(
                    onClick = { onTextFontChanged(item) }
                ) {
                    Text(
                        text = item,
                        fontFamily = fontLoader.getFontFamily(item),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}