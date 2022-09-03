package my.noveldokusha.utils

import android.view.View
import android.view.ViewGroup
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

fun View.fadeIn(durationMillis: Long = 150) = apply {
    alpha = 0f
    visibility = View.VISIBLE
    animate().also {
        it.alpha(1f)
        it.duration = durationMillis
        it.interpolator = DecelerateInterpolator()
    }
}

fun View.addLeftMargin(marginPx: Int = 1000, condition: () -> Boolean) {
    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
        it.leftMargin = if (condition()) marginPx else 0
    }
}

fun View.addRightMargin(marginPx: Int = 1000, condition: () -> Boolean) {
    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
        it.rightMargin = if (condition()) marginPx else 0
    }
}

fun View.addTopMargin(marginPx: Int = 1000, condition: () -> Boolean) {
    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
        it.topMargin = if (condition()) marginPx else 0
    }
}

fun View.addBottomMargin(marginPX: Int = 1000, condition: () -> Boolean) {
    layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).also {
        it.bottomMargin = if (condition()) marginPX else 0
    }
}