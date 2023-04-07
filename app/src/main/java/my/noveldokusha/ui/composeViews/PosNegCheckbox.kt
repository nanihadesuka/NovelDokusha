package my.noveldokusha.ui.composeViews

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.Error500
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Success500
import my.noveldokusha.ui.theme.Themes

fun ToggleableState.next() = when (this) {
    ToggleableState.On -> ToggleableState.Indeterminate
    ToggleableState.Indeterminate -> ToggleableState.Off
    ToggleableState.Off -> ToggleableState.On
}

@Composable
fun PosNegCheckbox(
    text: String,
    state: ToggleableState,
    onStateChange: (ToggleableState) -> Unit,
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
        val checkedColor by animateColorAsState(
            targetValue = when (updatedState) {
                ToggleableState.Off -> Success500
                ToggleableState.On -> Success500
                ToggleableState.Indeterminate -> Error500
            },
            animationSpec = tween(250), label = ""
        )
        TriStateCheckbox(
            state = updatedState,
            onClick = null,
            colors = CheckboxDefaults.colors(
                checkmarkColor = MaterialTheme.colorScheme.primary,
                checkedColor = checkedColor,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                disabledCheckedColor = checkedColor.copy(alpha = 0.25f),
                disabledUncheckedColor = MaterialTheme.colorScheme.outlineVariant,
                disabledIndeterminateColor = MaterialTheme.colorScheme.outlineVariant,
            )
        )
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
    InternalThemeObject(theme = if (isSystemInDarkTheme()) Themes.DARK else Themes.LIGHT) {
        Column {
            PosNegCheckbox(text = "Checkbox on", state = ToggleableState.On, onStateChange = { })
            PosNegCheckbox(text = "Checkbox off", state = ToggleableState.Off, onStateChange = { })
            PosNegCheckbox(
                text = "Checkbox indeterminate",
                state = ToggleableState.Indeterminate,
                onStateChange = { }
            )
        }
    }
}