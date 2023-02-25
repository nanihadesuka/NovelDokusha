package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.InternalThemeObject
import my.noveldokusha.ui.theme.Themes

@Composable
fun MyOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeHolderText: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeHolderText,
                modifier = Modifier.alpha(0.7f),
                style = MaterialTheme.typography.subtitle2
            )
        },
        modifier = modifier
            .heightIn(min = 42.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.onPrimary
        ),
        interactionSource = MutableInteractionSource()
    )
}

@Preview
@Composable
private fun PreviewView() {
    InternalThemeObject(Themes.LIGHT) {
        MyOutlinedTextField(
            value = "",
            onValueChange = {},
            placeHolderText = "placeholder"
        )
    }
}