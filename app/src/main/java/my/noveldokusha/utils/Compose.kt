package my.noveldokusha.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun <T> rememberMutableStateOf(value: T) = remember { mutableStateOf(value) }


@Composable
fun LazyListState.isAtTop(threshold: Dp) = run {

    val density by rememberUpdatedState(LocalDensity.current)

    return@run remember {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) return@derivedStateOf false
            val item = layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf true
            with(density) { item.offset.toDp() } > -threshold
        }
    }
}

