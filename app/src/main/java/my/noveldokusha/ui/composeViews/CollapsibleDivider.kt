package my.noveldokusha.ui.composeViews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleDivider(
    scrollState: TopAppBarState,
    modifier: Modifier = Modifier,
) {
    val isNotOverlappingContent by remember {
        derivedStateOf {
            scrollState.overlappedFraction <= 0.01f
        }
    }
    val alpha by animateColorAsState(
        targetValue = if (isNotOverlappingContent) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "divider opacity",
    )
    val padding by animateDpAsState(
        targetValue = if (isNotOverlappingContent) 10.dp else 0.dp,
        label = "divider fill width fraction",
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Divider(color = alpha, modifier = Modifier.padding(horizontal = padding))
    }
}

@Composable
fun CollapsibleDivider(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val isAtTop by remember {
        derivedStateOf {
            scrollState.value < 10
        }
    }
    val alpha by animateColorAsState(
        targetValue = if (isAtTop) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "divider opacity",
    )
    val padding by animateDpAsState(
        targetValue = if (isAtTop) 10.dp else 0.dp,
        label = "divider fill width fraction",
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Divider(color = alpha, modifier = Modifier.padding(horizontal = padding))
    }
}
