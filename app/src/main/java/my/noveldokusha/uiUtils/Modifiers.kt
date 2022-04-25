package my.noveldokusha.uiUtils

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun Modifier.ifCase(condition: Boolean, fn: @Composable Modifier.() -> Modifier): Modifier
{
    return if (condition) fn(this) else this
}

@Composable
fun Modifier.drawBottomLine(color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)) =
    drawBehind {
        drawLine(
            color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height)
        )
    }

@Composable
fun Modifier.drawTopLine(color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)) =
    drawBehind {
        drawLine(
            color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f)
        )
    }