package my.noveldokusha.uiViews

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SmoothGraphBars(
    unitaryHeights: List<Float>,
    brush: Brush,
    modifier: Modifier = Modifier
)
{
    val valuesUpdated by rememberUpdatedState(newValue = unitaryHeights)
    val length by remember { derivedStateOf { valuesUpdated.size.toFloat() } }
    val scores by remember { derivedStateOf { valuesUpdated.asSequence().map { 1f - it } } }

    Canvas(modifier = modifier) {
        if (valuesUpdated.isEmpty())
            return@Canvas

        val points = scores.mapIndexed { i, f ->
            Pair(
                first = (i + 0.5f) / length * size.width,
                second = f * size.height
            )
        }
        val quart = 0.5f / length * size.width
        val initPoint = (0 - 0.0f) / length * size.width to size.height
        val path = Path()
        path.moveTo(initPoint.first, initPoint.second)
        var lastHeight = initPoint.second
        for ((w, h) in points)
        {
            path.cubicTo(
                x1 = w - quart,
                y1 = lastHeight,
                x2 = w - quart,
                y2 = h,
                x3 = w,
                y3 = h,
            )
            lastHeight = h
        }
        path.cubicTo(
            x1 = size.width,
            y1 = lastHeight,
            x2 = size.width,
            y2 = size.height,
            x3 = size.width,
            y3 = size.height,
        )
        this.drawPath(path = path, brush = brush)
    }
}

@Preview
@Composable
fun PreviewView()
{
    SmoothGraphBars(
        unitaryHeights = listOf(
            0.1f, 0.1f, 0f, 0.9f, 0.2f
        ),
        brush = Brush.horizontalGradient(
            0.2f to Color.Red,
            0.5f to Color(0xFFFF8800),
            0.8f to Color.Green
        )
    )
}