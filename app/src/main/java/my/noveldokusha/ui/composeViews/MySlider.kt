package my.noveldokusha.ui.composeViews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.slider.ColorfulIconSlider
import com.smarttoolfactory.slider.MaterialSliderDefaults
import com.smarttoolfactory.slider.SliderBrushColor
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.selectableMinHeight
import my.noveldokusha.utils.mix

@Composable
fun MySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    MySlider(
        value = value,
        valueRange = valueRange,
        onValueChange = onValueChange,
        modifier = modifier,
    ) {
        Text(text = text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun MySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        ColorfulIconSlider(
            value = value,
            valueRange = valueRange,
            onValueChange = { value -> onValueChange(value) },
            coerceThumbInTrack = true,
            colors = MaterialSliderDefaults.defaultColors(
                activeTrackColor = SliderBrushColor(ColorAccent),
                thumbColor = SliderBrushColor(ColorAccent),
                inactiveTrackColor = SliderBrushColor(
                    MaterialTheme.colors.primaryVariant.mix(
                        ColorAccent,
                        0.5f
                    )
                )
            ),
            trackHeight = selectableMinHeight,
            thumb = {
                Surface(
                    color = ColorAccent,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {}
            }
        )
        overlayContent()
    }
}