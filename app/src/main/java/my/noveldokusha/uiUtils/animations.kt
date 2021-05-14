package my.noveldokusha.uiUtils

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

fun View.fadeInVertical(displacement: Float = 0f, duration: Long = 150) = also {
	it.alpha = 0f
	it.translationY = displacement
	it.visibility = View.VISIBLE
	it.animate().apply {
		alpha(1f)
		translationY(0f)
		this.duration = duration
		interpolator = DecelerateInterpolator()
	}
}

fun View.fadeOutVertical(displacement: Float = 0f, duration: Long = 150) = also {
	it.alpha = 1f
	it.translationY = 0f
	it.animate().apply {
		alpha(0f)
		translationY(displacement)
		this.duration = duration
		interpolator = AccelerateInterpolator()
		withEndAction {
			it.visibility = View.INVISIBLE
		}
	}
}