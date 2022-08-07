package my.noveldokusha.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface Toasty {
    fun show(text: String, shortDuration: Boolean = true)
    fun show(@StringRes id: Int, shortDuration: Boolean = true)
}

class ToastyToast(private val applicationContext: Context) : Toasty {

    override fun show(text: String, shortDuration: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(applicationContext, text, durationMapper(shortDuration))
                .show()
        }
    }

    override fun show(@StringRes id: Int, shortDuration: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(applicationContext, id, durationMapper(shortDuration))
                .show()
        }
    }

    private fun durationMapper(shortDuration: Boolean) = when (shortDuration) {
        true -> Toast.LENGTH_SHORT
        false -> Toast.LENGTH_LONG
    }
}

