package my.noveldokusha.ui.screens.reader.settingsViews

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import my.noveldokusha.R
import my.noveldokusha.ui.composeViews.MySlider

@Composable
fun TextSizeSetting(
    textSize: Float,
    onTextSizeChanged: (Float) -> Unit,
) {
    var pos by remember { mutableStateOf(textSize) }
    MySlider(
        value = pos,
        valueRange = 8f..24f,
        onValueChange = {
            pos = it
            onTextSizeChanged(pos)
        },
        text = stringResource(R.string.text_size) + ": %.2f".format(pos)
    )
}