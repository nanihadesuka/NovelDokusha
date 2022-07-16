package my.noveldokusha.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.ifCase(condition: Boolean, fn: @Composable Modifier.() -> Modifier): Modifier {
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


/**
 * Blocks any input from passing this composable down the event tree.
 * Effectively acts as a surface.
 */
fun Modifier.blockInteraction() = this.pointerInput(Unit) {}


fun Modifier.clickableWithUnboundedIndicator(onClick: () -> Unit) = composed { clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = rememberRipple(bounded = false),
    onClick = onClick
) }