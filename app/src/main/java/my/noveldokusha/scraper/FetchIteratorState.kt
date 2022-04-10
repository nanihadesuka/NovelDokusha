package my.noveldokusha.scraper

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*

/**
 * Used to fetch data that needs multiple request for jetpack compose.
 * e.g: Asking for a list of results that has multiple pages
 *
 * The fetch iteration will be consided finished if a results returns an empty list.
 */
class FetchIteratorState<T>(
    private val coroutineScope: CoroutineScope,
    val list: SnapshotStateList<T> = mutableStateListOf(),
    private var fn: (suspend (index: Int) -> Response<List<T>>)
) {
    enum class STATE { IDLE, LOADING, CONSUMED }

    private var index = 0
    private var job: Job? = null

    var state by mutableStateOf(STATE.IDLE)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun reset() {
        job?.cancel()
        list.clear()
        index = 0
        state = STATE.IDLE
        error = null
    }

    fun reloadFailedLastLoad() {
        if(error == null)
            return
        job?.cancel()
        index = (index-1).coerceAtLeast(0)
        state = STATE.IDLE
        error = null
        fetchNext()
    }

    fun fetchNext() {
        if (state != STATE.IDLE) return
        state = STATE.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            val res = withContext(Dispatchers.IO) { fn(index) }
            if (!isActive) return@launch
            state = when (res) {
                is Response.Success -> {
                    list.addAll(res.data)
                    if (res.data.isEmpty()) STATE.CONSUMED else STATE.IDLE
                }
                is Response.Error -> {
                    error = res.message
                    STATE.CONSUMED
                }
            }
            index += 1
        }
    }

    fun setFunction(fn: (suspend (index: Int) -> Response<List<T>>)) {
            this.fn = fn
    }
}