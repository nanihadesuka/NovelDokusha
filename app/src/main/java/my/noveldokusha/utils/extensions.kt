package my.noveldokusha.utils

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.core.content.res.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences


@ColorInt
fun @receiver:AttrRes Int.colorAttrRes(ctx: Context): Int =
    ctx.theme.obtainStyledAttributes(intArrayOf(this)).use {
        it.getColor(0, Color.MAGENTA)
    }


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

fun androidx.compose.ui.graphics.Color.mix(
    color: androidx.compose.ui.graphics.Color,
    fraction: Float,
) = androidx.compose.ui.graphics.Color(
    red = red * fraction + color.red * (1f - fraction),
    green = green * fraction + color.green * (1f - fraction),
    blue = blue * fraction + color.blue * (1f - fraction),
    alpha = alpha * fraction + color.alpha * (1f - fraction),
)

fun <T> List<T>.hasValidIndex(index: Int): Boolean = (0 >= index) && (index <= lastIndex)

fun AppPreferences.TERNARY_STATE.toToggleableState() = when (this) {
    AppPreferences.TERNARY_STATE.active -> ToggleableState.On
    AppPreferences.TERNARY_STATE.inverse -> ToggleableState.Indeterminate
    AppPreferences.TERNARY_STATE.inactive -> ToggleableState.Off
}