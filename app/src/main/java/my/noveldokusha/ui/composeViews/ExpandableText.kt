package my.noveldokusha.ui.composeViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.unit.Constraints
import my.noveldokusha.utils.ifCase

@Composable
fun ExpandableText(
    text: String,
    linesForExpand: Int,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<String>.() -> ContentTransform = { fadeIn() togetherWith fadeOut() }
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = transitionSpec,
        label = "ExpandableText",
    ) { target ->
        BoxWithConstraints {
            var expanded by rememberSaveable { mutableStateOf(false) }
            val maxLines by remember {
                derivedStateOf { if (expanded) Int.MAX_VALUE else linesForExpand }
            }
            val textStyle = MaterialTheme.typography.bodyMedium
            val paragraph = Paragraph(
                text = target,
                style = textStyle,
                constraints = Constraints(maxWidth = constraints.maxWidth),
                density = LocalDensity.current,
                fontFamilyResolver = LocalFontFamilyResolver.current
            )
            val textColor = LocalContentColor.current
            val textColorEnd by animateColorAsState(
                targetValue = when {
                    target.isBlank() -> textColor.copy(alpha = 0.25f)
                    paragraph.lineCount <= linesForExpand -> textColor
                    expanded -> textColor
                    else -> textColor.copy(alpha = 0.25f)
                },
                label = "",
            )
            Text(
                text = target,
                maxLines = maxLines,
                style = textStyle.copy(
                    brush = Brush.verticalGradient(
                        0.50f to textColor,
                        1.00f to textColorEnd
                    )
                ),
                modifier = modifier
                    .ifCase(target.isNotBlank()) { animateContentSize() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = !expanded }
                    )
            )
        }
    }
}