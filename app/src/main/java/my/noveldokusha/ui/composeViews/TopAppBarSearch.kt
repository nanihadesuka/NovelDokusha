package my.noveldokusha.ui.composeViews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSearch(
    focusRequester: FocusRequester,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    placeholderText: String = stringResource(R.string.search_here),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    inputEnabled: Boolean = true,
    labelText: String? = null,
) {
    // Many hacks going on here to make it scrollBehavior compatible
    Box {
        Box(
            Modifier
                .padding(8.dp)
                .systemBarsPadding()
                .background(containerColor, CircleShape)
                .matchParentSize()
        )
        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,

                ),
            navigationIcon = {
                IconButton(onClick = onClose, modifier = Modifier.padding(start = 2.dp)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            title = {
                LaunchedEffect(Unit) {
                    if (searchText.isEmpty() && inputEnabled) {
                        focusRequester.requestFocus()
                    }
                }
                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    maxLines = 1,
                    enabled = inputEnabled,
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = ColorAccent,
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.onTertiary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onTertiary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onTertiary,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                    label = labelText?.let {
                        { Text(text = it) }
                    },
                    keyboardActions = KeyboardActions(onDone = {
                        if (searchText.isNotBlank()) {
                            onTextDone(searchText)
                        }
                    }),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = placeholderText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchText.isNotEmpty(),
                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
                        ) {
                            IconButton(onClick = { onSearchTextChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            }
        )
    }
}
