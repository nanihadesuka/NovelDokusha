package my.noveldokusha.uiViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Needs parent to have Modifier.height(IntrinsicSize.Min)
@Composable
fun DividerVertical(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface,
    thickness: Dp = 1.dp,
    topIndent: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .padding(top = topIndent)
            .background(color)
            .fillMaxHeight()
            .width(thickness)
    )
}