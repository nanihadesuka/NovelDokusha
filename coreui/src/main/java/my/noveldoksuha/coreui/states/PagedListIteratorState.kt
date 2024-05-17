package my.noveldoksuha.coreui.states

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response

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
    var error by mutableStateOf<String?>(null)

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

    val hasFinished by derivedStateOf {
        state == IteratorState.CONSUMED ||
                (state == IteratorState.IDLE && list.size != 0) ||
                error != null
    }

    fun fetchNext() {
        if (state != IteratorState.IDLE) return
        state = IteratorState.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            val res = withContext(Dispatchers.Default) { fn(index) }
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
