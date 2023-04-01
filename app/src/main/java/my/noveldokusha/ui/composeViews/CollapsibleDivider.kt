package my.noveldokusha.ui.composeViews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CollapsibleDivider(
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val isAtTop by remember {
        derivedStateOf {
            if (lazyGridState.firstVisibleItemIndex > 0) return@derivedStateOf false
            val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf true
            item.offset.y > -10
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

@Composable
fun CollapsibleDivider(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val isAtTop by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) return@derivedStateOf false
            val item = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf true
            item.offset > -10
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