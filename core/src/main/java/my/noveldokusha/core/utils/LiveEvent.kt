package my.noveldokusha.core.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

// The implementation might not be perfect but should work
// most of the time and is the simplest one that I can think of
class LiveEvent<T> : MutableLiveData<T>() {
    private var setTime = System.currentTimeMillis()

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