package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.screens.reader.RoundedContentLayout

@Composable
fun TextSizeSetting(
    textSize: Float,
    onTextSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    RoundedContentLayout(modifier) {
        Icon(
            imageVector = Icons.TwoTone.FormatSize,
            contentDescription = null,
            modifier = Modifier
                .width(50.dp)
        )
        var pos by remember { mutableStateOf(textSize) }
        Slider(
            value = pos,
            onValueChange = {
                pos = it
                onTextSizeChanged(pos)
            },
            valueRange = 8f..24f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.onPrimary,
                activeTrackColor = MaterialTheme.colors.onPrimary,
                activeTickColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                inactiveTickColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
            ),
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}