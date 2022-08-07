package my.noveldokusha.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// The implementation might not be perfect but should work
// most of the time and is the simplest one that I can think of
class LiveEvent<T> : MutableLiveData<T>() {
    var setTime = System.currentTimeMillis()

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val observerTime = System.currentTimeMillis()
        super.observe(owner) {
            if (synchronized(this) { observerTime <= setTime })
                observer.onChanged(it)
        }
    }

    @MainThread
    override fun setValue(value: T) {
        setTime = System.currentTimeMillis()
        super.setValue(value)
    }

    override fun postValue(value: T) {
        synchronized(this) { setTime = System.currentTimeMillis() }
        super.postValue(value)
    }
}

fun <T> Flow<T>.asLiveEvent(): LiveEvent<T> {
    val liveEvent = LiveEvent<T>()
    CoroutineScope(Dispatchers.IO).launch {
        collectLatest {
            liveEvent.postValue(it)
        }
    }
    return liveEvent
}