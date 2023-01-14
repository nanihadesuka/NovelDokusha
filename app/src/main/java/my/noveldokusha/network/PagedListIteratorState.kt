package my.noveldokusha.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*
import my.noveldokusha.data.Response

/**
 * Used to fetch data that needs multiple request for jetpack compose.
 * e.g: Asking for a list of results that has multiple pages
 *
 * The fetch iteration will be consider finished if a results returns positive hasNoNextPage.
 */
class PagedListIteratorState<T>(
    private val coroutineScope: CoroutineScope,
    val list: SnapshotStateList<T> = mutableStateListOf(),
    private var fn: (suspend (index: Int) -> Response<PagedList<T>>)
) {
    private var index = 0
    private var job: Job? = null

    var state by mutableStateOf(IteratorState.IDLE)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun reset() {
        job?.cancel()
        list.clear()
        index = 0
        state = IteratorState.IDLE
        error = null
    }

    fun reloadFailedLastLoad() {
        if (error == null)
            return
        job?.cancel()
        index = (index - 1).coerceAtLeast(0)
        state = IteratorState.IDLE
        error = null
        fetchNext()
    }

    fun fetchNext() {
        if (state != IteratorState.IDLE) return
        state = IteratorState.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            val res = withContext(Dispatchers.IO) { fn(index) }
            if (!isActive) return@launch
            state = when (res) {
                is Response.Success -> {
                    list.addAll(res.data.list)
                    if (res.data.hasNoNextPage) IteratorState.CONSUMED else IteratorState.IDLE
                }
                is Response.Error -> {
                    error = res.message
                    IteratorState.CONSUMED
                }
            }
            index += 1
        }
    }

    fun setFunction(fn: (suspend (index: Int) -> Response<PagedList<T>>)) {
        this.fn = fn
    }
}
