package my.noveldokusha.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

@Composable
fun LazyListState.OnTopReached(
    buffer: Int = 0,
    call: () -> Unit
) {
    val callUpdated by rememberUpdatedState(newValue = call)

    val shouldCall by remember(buffer) {
        derivedStateOf {
            val item = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf false
            item.index <= buffer.coerceAtLeast(0)
        }
    }
    LaunchedEffect(shouldCall) {
        snapshotFlow { shouldCall }.collect {
            if (it) callUpdated()
        }
    }
}

@Composable
fun LazyListState.OnBottonReached(
    buffer: Int = 0,
    call: () -> Unit
) {
    val callUpdated by rememberUpdatedState(newValue = call)

    val shouldCall by remember(buffer) {
        derivedStateOf {
            val item = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            item.index >= (layoutInfo.totalItemsCount - 1 - buffer.coerceAtLeast(0))
        }
    }
    LaunchedEffect(shouldCall) {
        snapshotFlow { shouldCall }.collect {
            if (it) callUpdated()
        }
    }
}
