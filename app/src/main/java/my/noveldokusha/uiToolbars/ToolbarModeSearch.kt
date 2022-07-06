package my.noveldokusha.uiToolbars

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.uiUtils.ifCase

@Composable
fun ToolbarModeSearch(
    focusRequester: FocusRequester,
    searchText: MutableState<String>,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
    color: Color = MaterialTheme.colors.surface,
    borderColor: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f),
    placeholderText: String = stringResource(R.string.search_here),
    showUnderline: Boolean = false,
    topPadding: Dp = 8.dp,
    height: Dp = 56.dp
)
{
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .background(color)
            .fillMaxWidth()
            .ifCase(showUnderline) {
                drawBehind {
                    drawLine(
                        borderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height)
                    )
                }
            }
            .padding(top = topPadding, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(height)
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        BackHandler { onClose() }

        // Fake button to center text
        IconButton(onClick = { }, enabled = false) {}

        BasicTextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            singleLine = true,
            maxLines = 1,
            textStyle = MaterialTheme.typography.h6.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(ColorAccent),
            keyboardActions = KeyboardActions(
                onDone = { onTextDone(searchText.value) }
            ),
            decorationBox = {
                if (searchText.value.isBlank()) Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.h6.copy(
                        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                ) else it()
            },
            modifier = Modifier
                .focusRequester(focusRequester)
        )

        IconButton(onClick = {
            if (searchText.value.isBlank()) onClose()
            else searchText.value = ""
        }) {
            Icon(
                Icons.Default.Close,
                contentDescription = null
            )
        }
    }
}