package my.noveldokusha.utils

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

fun View.fadeInVertical(displacement: Float = 0f, duration: Long = 150) = apply {
	alpha = 0f
	translationY = displacement
	visibility = View.VISIBLE
	animate().also {
		it.alpha(1f)
		it.translationY(0f)
		it.duration = duration
		it.interpolator = DecelerateInterpolator()
	}
}

fun View.fadeOutVertical(displacement: Float = 0f, duration: Long = 150) = apply {
	alpha = 1f
	translationY = 0f
	animate().also {
		it.alpha(0f)
		it.translationY(displacement)
		it.duration = duration
		it.interpolator = AccelerateInterpolator()
		it.withEndAction {
			visibility = View.INVISIBLE
		}
	}
}

fun View.fadeIn(duration: Long = 150) = apply {
	alpha = 0f
	visibility = View.VISIBLE
	animate().also {
		it.alpha(1f)
		it.duration = duration
		it.interpolator = DecelerateInterpolator()
	}
}