package my.noveldokusha.composableActions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import my.noveldokusha.network.IteratorState

@OptIn(FlowPreview::class)
@Composable
fun ListLoadWatcher(
    listState: LazyListState,
    loadState: my.noveldokusha.network.IteratorState,
    onLoadNext: () -> Unit,
    debounceMilliseconds: Long = 100L
) {
    val loadStateUpdated by rememberUpdatedState(newValue = loadState)
    val isReadyToLoad by remember {
        derivedStateOf {
            val lastVisibleIndex =
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val isLoadZone = lastVisibleIndex > (listState.layoutInfo.totalItemsCount - 3)
            val isIDLE = loadStateUpdated == my.noveldokusha.network.IteratorState.IDLE
            val state = isLoadZone && isIDLE
            state
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isReadyToLoad }
            .filter { it }
            .debounce(debounceMilliseconds)
            .collect { onLoadNext() }
    }
}

@OptIn(FlowPreview::class)
@Composable
fun ListGridLoadWatcher(
    listState: LazyGridState,
    loadState: my.noveldokusha.network.IteratorState,
    onLoadNext: () -> Unit,
    debounceMilliseconds: Long = 100L
) {
    val loadStateUpdated by rememberUpdatedState(newValue = loadState)
    val isReadyToLoad by remember {
        derivedStateOf {
            val lastVisibleIndex =
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val isLoadZone = lastVisibleIndex > (listState.layoutInfo.totalItemsCount - 3)
            val isIDLE = loadStateUpdated == my.noveldokusha.network.IteratorState.IDLE
            val state = isLoadZone && isIDLE
            state
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isReadyToLoad }
            .filter { it }
            .debounce(debounceMilliseconds)
            .collect { onLoadNext() }
    }
}