package my.noveldokusha.ui.composeViews

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.utils.blockInteraction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarModeSearch(
    focusRequester: FocusRequester,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
    color: Color = MaterialTheme.colorScheme.surface,
    placeholderText: String = stringResource(R.string.search_here),
    topPadding: Dp = 8.dp,
    height: Dp = 56.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .blockInteraction()
            .background(color)
            .fillMaxWidth()
            .padding(top = topPadding, start = 12.dp, end = 12.dp)
            .height(height)
            .padding(bottom = 4.dp)
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        BackHandler { onClose() }
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            colors = TextFieldDefaults.textFieldColors(
                cursorColor = ColorAccent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onTertiary,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onTertiary,
            ),
            keyboardActions = KeyboardActions(onDone = { onTextDone(searchText) }),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholderText,
                    style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            leadingIcon = {
                Box(modifier = Modifier.size(24.dp))
            },
            trailingIcon = {
                IconButton(onClick = {
                    if (searchText.isBlank()) onClose()
                    else onSearchTextChange("")
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null
                    )
                }
            },
            shape = CircleShape
        )
    }
}
