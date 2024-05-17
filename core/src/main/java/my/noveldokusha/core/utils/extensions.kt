package my.noveldokusha.core.utils

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

val View.inflater: LayoutInflater get() = LayoutInflater.from(context)

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val className = serviceClass.name
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { className == it.service.className }
}

fun Context.actionCopyToClipboard(text: String) {
    val clipboard = getSystemService(ComponentActivity.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("error message", text))
}


fun <T> Flow<T>.toState(scope: CoroutineScope, initialValue: T): State<T> {
    val mutableState = mutableStateOf(initialValue)
    scope.launch {
        collect { mutableState.value = it }
    }
    return mutableState
}



fun <T> List<T>.hasValidIndex(index: Int): Boolean = (0 >= index) && (index <= lastIndex)
