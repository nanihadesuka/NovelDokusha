package my.noveldokusha.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.composableActions.debouncedAction

@Composable
fun Modifier.ifCase(condition: Boolean, fn: @Composable Modifier.() -> Modifier): Modifier {
    return if (condition) fn(this) else this
}

@Composable
fun Modifier.drawBottomLine(
    color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f),
    thickness: Dp = Dp.Hairline
) = drawBehind {
    val thicknessPx = thickness.toPx()
    drawLine(
        color,
        start = Offset(0f, size.height - thicknessPx),
        end = Offset(size.width, size.height - thicknessPx),
        strokeWidth = thicknessPx
    )
}

@Composable
fun Modifier.drawTopLine(
    color: Color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f),
    thickness: Dp = Dp.Hairline
) = drawBehind {
    drawLine(
        color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = thickness.toPx()
    )
}


/**
 * Blocks any input from passing this composable down the event tree.
 * Effectively acts as a surface.
 */
fun Modifier.blockInteraction() = this.pointerInput(Unit) {}


fun Modifier.clickableWithUnboundedIndicator(onClick: () -> Unit) = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false),
        onClick = onClick
    )
}

fun Modifier.debouncedClickable(waitMillis: Long = 250, action: () -> Unit) = composed {
    clickable(
        onClick = debouncedAction(waitMillis = waitMillis, action = action)
    )
}

fun Modifier.outlineCircle(width: Dp = 1.dp): Modifier = composed {
    border(
        width = width,
        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
        shape = CircleShape
    ).clip(CircleShape)
}

fun Modifier.outlineRounded(width: Dp = 1.dp): Modifier = composed {
    border(
        width = width,
        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ).clip(MaterialTheme.shapes.medium)
}

fun Modifier.backgroundCircle(): Modifier = composed {
    background(
        color = MaterialTheme.colors.primary,
        shape = CircleShape
    )
}

fun Modifier.backgroundRounded(): Modifier = composed {
    background(
        color = MaterialTheme.colors.primary,
        shape = MaterialTheme.shapes.medium
    )
}