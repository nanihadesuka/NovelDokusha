package my.noveldokusha.ui.composeViews

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@ExperimentalAnimationApi
@Composable
fun <S> AnimatedTransition(
    targetState: S,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentScope<S>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220, delayMillis = 90)) with
                fadeOut(animationSpec = tween(90))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable() AnimatedVisibilityScope.(targetState: S) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        contentAlignment = contentAlignment,
        content = content
    )
}