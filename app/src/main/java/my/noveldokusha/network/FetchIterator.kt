package my.noveldokusha.network

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

/**
 * Used to fetch data that needs multiple request.
 * e.g: Asking for a list of results that has multiple pages
 *
 * The fetch iteration will be consider finished if a results returns an empty list.
 */
class FetchIterator<T>(
    private val coroutineScope: CoroutineScope,
    private val list: ArrayList<T> = ArrayList(listOf()),
    private var fn: (suspend (index: Int) -> Response<List<T>>)
) {
    enum class STATE { IDLE, LOADING, CONSUMED }

    private var state = STATE.IDLE
    private var index = 0
    private var job: Job? = null

    val onSuccess = MutableLiveData<List<T>>()
    val onCompleted = MutableLiveData<Unit>()
    val onCompletedEmpty = MutableLiveData<Unit>()
    val onError = MutableLiveData<Response.Error>()
    val onFetching = MutableLiveData<Boolean>()
    val onReset = MutableLiveData<Unit>()

    fun setFunction(fn: (suspend (index: Int) -> Response<List<T>>)) {
        this.fn = fn
    }

    fun reset() {
        job?.cancel()
        state = STATE.IDLE
        index = 0
        list.clear()
        onSuccess.value = list.toList()
        onReset.value = Unit
    }

    fun fetchTrigger(trigger: () -> Boolean) {
        if (state == STATE.IDLE && trigger())
            fetchNext()
    }

    private fun fetchNext() {
        if (state != STATE.IDLE) return
        state = STATE.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            onFetching.value = true
            val res = withContext(Dispatchers.IO) { fn(index) }
            onFetching.value = false
            if (!isActive) return@launch
            when (res) {
                is Response.Success -> {
                    if (res.data.isEmpty()) {
                        state = STATE.CONSUMED
                        if (list.isEmpty())
                            onCompletedEmpty.value = Unit
                        else
                            onCompleted.value = Unit
                    } else {
                        state = STATE.IDLE
                        list.addAll(res.data)
                        onSuccess.value = list.toList()
                    }
                }
                is Response.Error -> {
                    state = STATE.CONSUMED
                    onError.value = res
                    if (list.isEmpty())
                        onCompletedEmpty.value = Unit
                    else
                        onCompleted.value = Unit
                }
            }
            index += 1
        }
    }
}