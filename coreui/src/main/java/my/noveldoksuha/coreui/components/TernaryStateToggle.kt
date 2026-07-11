package my.noveldoksuha.coreui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.InternalTheme
import my.noveldokusha.core.appPreferences.TernaryState

@Composable
fun TernaryStateToggle(
    text: String,
    state: TernaryState,
    onStateChange: (TernaryState) -> Unit,
    activeIcon: @Composable (() -> Unit),
    inverseIcon: @Composable (() -> Unit),
    inactiveIcon: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val updatedState by rememberUpdatedState(newValue = state)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(
                value = true,
                onValueChange = { onStateChange(updatedState) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp)
    ) {
        AnimatedContent(targetState = updatedState, label = "") {
            when (it) {
                TernaryState.Active -> activeIcon()
                TernaryState.Inverse -> inverseIcon()
                TernaryState.Inactive -> inactiveIcon()
            }
        }
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewView() {

    @Composable
    fun draw(text: String, state: TernaryState) {
        TernaryStateToggle(
            text = text,
            state = state,
            onStateChange = { },
            modifier = Modifier.fillMaxWidth(),
            activeIcon = { Icon(imageVector = Icons.Filled.ArrowUpward, null) },
            inverseIcon = { Icon(imageVector = Icons.Filled.ArrowDownward, null) },
            inactiveIcon = { Icon(imageVector = Icons.Filled.SwapVert, null) },
        )
    }

    InternalTheme {
        Column {
            draw(text = "Up", state = TernaryState.Active)
            draw(text = "Down", state = TernaryState.Inverse)
            draw(text = "Inactive", state = TernaryState.Inactive)
        }
    }
}
