package my.noveldokusha.ui

import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel()
{
	private var initialized = AtomicBoolean(false)
	
	// Used so that if the activity/fragment is recreated but the the model persists, its initializer won't be called again
	protected fun callOneTime(fn: () -> Unit)
	{
		if (initialized.compareAndSet(false, true))
			fn()
	}
}