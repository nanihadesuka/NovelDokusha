package my.noveldokusha.uiUtils

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.*
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import my.noveldokusha.App
import my.noveldokusha.ui.BaseFragment

fun RecyclerView.ViewHolder.addBottomMargin(margin: Int = 1000, condition: () -> Boolean) = itemView.addBottomMargin(margin, condition)
fun RecyclerView.ViewHolder.addTopMargin(margin: Int = 1000, condition: () -> Boolean) = itemView.addTopMargin(margin, condition)
fun RecyclerView.ViewHolder.addRightMargin(margin: Int = 1000, condition: () -> Boolean) = itemView.addRightMargin(margin, condition)
fun RecyclerView.ViewHolder.addLeftMargin(margin: Int = 1000, condition: () -> Boolean) = itemView.addLeftMargin(margin, condition)

fun View.addLeftMargin(margin: Int = 1000, condition: () -> Boolean): Unit
{
	layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
		it.leftMargin = if (condition()) margin else 0
	}
}

fun View.addRightMargin(margin: Int = 1000, condition: () -> Boolean): Unit
{
	layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
		it.rightMargin = if (condition()) margin else 0
	}
}

fun View.addTopMargin(margin: Int = 1000, condition: () -> Boolean): Unit
{
	layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
		it.topMargin = if (condition()) margin else 0
	}
}

fun View.addBottomMargin(margin: Int = 1000, condition: () -> Boolean): Unit
{
	layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
		it.bottomMargin = if (condition()) margin else 0
	}
}

@ColorInt
fun @receiver:AttrRes Int.colorAttrRes(ctx: Context): Int = ctx.theme.obtainStyledAttributes(intArrayOf(this)).use {
	it.getColor(0, Color.MAGENTA)
}

@ColorInt
fun @receiver:ColorRes Int.colorIdRes(ctx: Context): Int = ContextCompat.getColor(ctx, this)

fun toast(text: String, duration: Int = Toast.LENGTH_SHORT) = CoroutineScope(Dispatchers.Main).launch {
	Toast.makeText(App.instance, text, duration).show()
}

val View.inflater: LayoutInflater get() = LayoutInflater.from(context)

fun <T, A> Observer<T>.switchLiveData(old: A?, new: A?, owner: LifecycleOwner, liveData: A.() -> LiveData<T>)
{
	old?.let { liveData(it).removeObserver(this) }
	new?.let { liveData(it).observe(owner, this) }
}

fun Context.isOnPortraitMode() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
fun BaseFragment.isOnPortraitMode() = requireActivity().isOnPortraitMode()

fun Context.spToPx(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics).toInt()
fun BaseFragment.spToPx(value: Float) = requireActivity().spToPx(value)

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
	val className = serviceClass.name
	val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
	return manager.getRunningServices(Integer.MAX_VALUE)
		.any { className == it.service.className }
}

fun Context.copyToClipboard(text: String)
{
    val clipboard = getSystemService(ComponentActivity.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("error message", text))
}


fun <T> Flow<T>.toState(scope: CoroutineScope, initialValue: T): State<T>
{
	val mutableState = mutableStateOf(initialValue)
	scope.launch {
        collect { mutableState.value = it }
    }
	return mutableState
}
