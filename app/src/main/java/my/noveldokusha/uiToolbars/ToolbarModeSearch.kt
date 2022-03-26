package my.noveldokusha.uiToolbars

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent

@Composable
fun ToolbarModeSearch(
    focusRequester: FocusRequester,
    searchText: MutableState<String>,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
    placeholderText: String = stringResource(R.string.search_here)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .background(MaterialTheme.colors.surface)
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