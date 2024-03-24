package my.noveldokusha.utils

import android.view.View
import android.view.animation.DecelerateInterpolator

fun View.fadeIn(durationMillis: Long = 150) = apply {
    alpha = 0f
    visibility = View.VISIBLE
    animate().also {
        it.alpha(1f)
        it.duration = durationMillis
        it.interpolator = DecelerateInterpolator()
    }
}
