package my.noveldokusha.ui.databaseSearch

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import my.noveldokusha.R
import my.noveldokusha.databinding.ViewCheckboxBinding

class Checkbox3StatesView : FrameLayout
{
	private val holder by lazy { ViewCheckboxBinding.inflate(LayoutInflater.from(context), this, false) }
	
	constructor(context: Context) : super(context)
	{
		addView(holder.root)
		updateButton()
	}
	
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	{
		addView(holder.root)
		updateButton()
	}
	
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
	{
		addView(holder.root)
		updateButton()
	}
	
	enum class STATE
	{ NONE, POSITIVE, NEGATIVE }
	
	var state = STATE.NONE
		set(value)
		{
			if (field == value) return
			field = value
			updateButton()
			onStateChangeListener(field)
		}
	
	var text: String
		get() = holder.text.text.toString()
		set(value)
		{
			holder.text.text = value
		}
	
	init
	{
		setOnClickListener {
			state = when (state)
			{
				STATE.NONE -> STATE.POSITIVE
				STATE.POSITIVE -> STATE.NEGATIVE
				STATE.NEGATIVE -> STATE.NONE
			}
		}
	}
	
	var onStateChangeListener: (newState: STATE) -> Unit = {}
	
	private fun updateButton(): Unit = when (state)
	{
		STATE.NONE -> Pair(R.drawable.ic_twotone_check_box_outline_blank_24, null)
		STATE.POSITIVE -> Pair(R.drawable.ic_twotone_check_box_24, android.R.color.holo_green_dark)
		STATE.NEGATIVE -> Pair(R.drawable.ic_twotone_indeterminate_check_box_24, android.R.color.holo_red_dark)
	}.let { color: Pair<Int, Int?> ->
		holder.icon.background = ContextCompat.getDrawable(context, color.first)
		holder.icon.backgroundTintList = color.second?.let { AppCompatResources.getColorStateList(context, it) }
	}
}