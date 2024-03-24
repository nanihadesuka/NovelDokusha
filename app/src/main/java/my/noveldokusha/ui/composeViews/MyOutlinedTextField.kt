package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.InternalTheme

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
                style = MaterialTheme.typography.titleSmall
            )
        },
        modifier = modifier
            .heightIn(min = 42.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
        ),
        interactionSource = remember { MutableInteractionSource() }
    )
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        MyOutlinedTextField(
            value = "",
            onValueChange = {},
            placeHolderText = "placeholder"
        )
    }
}