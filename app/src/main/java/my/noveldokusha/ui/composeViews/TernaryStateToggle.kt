package my.noveldokusha.ui.composeViews

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
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
import my.noveldokusha.AppPreferences.TERNARY_STATE
import my.noveldokusha.ui.theme.InternalTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TernaryStateToggle(
    text: String,
    state: TERNARY_STATE,
    onStateChange: (TERNARY_STATE) -> Unit,
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
                TERNARY_STATE.active -> activeIcon()
                TERNARY_STATE.inverse -> inverseIcon()
                TERNARY_STATE.inactive -> inactiveIcon()
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
    fun draw(text: String, state: TERNARY_STATE) {
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

    InternalTheme{
        Column {
            draw(text = "Up", state = TERNARY_STATE.active)
            draw(text = "Down", state = TERNARY_STATE.inverse)
            draw(text = "Inactive", state = TERNARY_STATE.inactive)
        }
    }
}