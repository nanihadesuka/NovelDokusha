package my.noveldokusha.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

fun Modifier.bounceOnPressed(
    interactionSource: InteractionSource,
    bounceScale: Float = 0.98f
) = composed {
    val isBeingPressed by interactionSource.collectIsPressedAsState()
    val animateScale by animateFloatAsState(
        targetValue = if (isBeingPressed) bounceScale else 1f,
        label = "buttonScaledAnimation",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    graphicsLayer {
        scaleX = animateScale
        scaleY = animateScale
    }
}

@Preview
@Composable
private fun PreviewView() {
    Column {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .bounceOnPressed(interactionSource)
                .size(120.dp, 40.dp)
                .background(Color.Red)
                .clickable(interactionSource = interactionSource, onClick = {}, indication = null)
        )
    }
}