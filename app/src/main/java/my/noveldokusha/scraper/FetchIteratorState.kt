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

    val state = mutableStateOf(STATE.IDLE)
    val error = mutableStateOf<String?>(null)

    fun reset() {
        job?.cancel()
        state.value = STATE.IDLE
        index = 0
        error.value = null
        list.clear()
    }

    fun fetchNext() {
        if (state.value != STATE.IDLE) return
        state.value = STATE.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            val res = withContext(Dispatchers.IO) { fn(index) }
            if (!isActive) return@launch
            state.value = when (res) {
                is Response.Success -> {
                    list.addAll(res.data)
                    if (res.data.isEmpty()) STATE.CONSUMED else STATE.IDLE
                }
                is Response.Error -> {
                    error.value = res.message
                    STATE.CONSUMED
                }
            }
            Log.e("NEW STATE", state.value.name)
            index += 1
        }
    }
}