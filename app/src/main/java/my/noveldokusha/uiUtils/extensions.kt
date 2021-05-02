package my.noveldokusha.uiUtils

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.ViewHolder.addBottomMargin(margin: Int = 600, condition: () -> Boolean): Unit
{
	itemView.layoutParams = (itemView.layoutParams as ViewGroup.MarginLayoutParams).also {
		it.bottomMargin = if (condition()) margin else 0
	}
}

fun RecyclerView.ViewHolder.addRightMargin(margin: Int = 300, condition: () -> Boolean): Unit
{
	itemView.layoutParams = (itemView.layoutParams as ViewGroup.MarginLayoutParams).also {
		it.rightMargin = if (condition()) margin else 0
	}
}