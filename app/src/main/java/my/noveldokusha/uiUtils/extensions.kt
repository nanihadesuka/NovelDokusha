package my.noveldokusha.uiUtils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.App

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
fun @receiver:AttrRes Int.colorIdRes(ctx: Context): Int = ContextCompat.getColor(ctx, this)

fun @receiver:StringRes Int.stringRes(): String = App.instance.getString(this)

fun toast(text: String, duration: Int = Toast.LENGTH_SHORT) = CoroutineScope(Dispatchers.Main).launch {
	Toast.makeText(App.instance, text, duration).show()
}

val View.inflater: LayoutInflater get() = LayoutInflater.from(context)

fun <T, A> Observer<T>.switchLiveData(old: A?, new: A?, owner: LifecycleOwner, liveData: A.() -> LiveData<T>)
{
	old?.let { liveData(it).removeObserver(this) }
	new?.let { liveData(it).observe(owner, this) }
}