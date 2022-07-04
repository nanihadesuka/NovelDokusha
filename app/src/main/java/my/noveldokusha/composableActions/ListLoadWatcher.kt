package my.noveldokusha.uiViews

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import my.noveldokusha.scraper.IteratorState

@Composable
fun ListLoadWatcher(
    listState: LazyListState,
    loadState: IteratorState,
    onLoadNext: () -> Unit,
    debounceMilliseconds: Long = 100L
)
{
    // Probably a bug loadState doesn't get udpates inside derivedStateOf, use this workaround
    val loadStateUpdated by rememberUpdatedState(newValue = loadState)
    val isReadyToLoad by remember {
        derivedStateOf {
            val lastVisibleIndex =
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val isLoadZone = lastVisibleIndex > (listState.layoutInfo.totalItemsCount - 3)
            val isIDLE = loadStateUpdated == IteratorState.IDLE
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