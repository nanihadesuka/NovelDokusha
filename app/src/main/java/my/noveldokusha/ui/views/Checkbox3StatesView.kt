package my.noveldokusha.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import my.noveldokusha.R
import my.noveldokusha.databinding.ViewCheckboxBinding
import my.noveldokusha.utils.inflater

class Checkbox3StatesView : FrameLayout
{
    private val holder by lazy { ViewCheckboxBinding.inflate(inflater, this, false) }

    constructor(context: Context) : super(context)
    {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    {
        init(context, attrs)
    }

    enum class STATE
    { NONE, POSITIVE, NEGATIVE }

    var state = STATE.NONE
        set(value)
        {
            if (field == value) return
            field = value
            updateButtonState()
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

    @Volatile
    var onStateChangeListener: (newState: STATE) -> Unit = {}

    private fun init(context: Context, attrs: AttributeSet? = null)
    {
        addView(holder.root)
        initAttributes(context, attrs)
        updateButtonState()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet? = null)
    {
        // Dont use "use" kotlin function extension, it does not work (autoclosable not implemented correctly?)
        val it = context.obtainStyledAttributes(attrs, R.styleable.Checkbox3StatesView)
        text = it.getString(R.styleable.Checkbox3StatesView_android_text) ?: ""
        it.recycle()
    }

    private fun updateButtonState(): Unit = when (state)
    {
        STATE.NONE -> Pair(R.drawable.ic_twotone_check_box_outline_blank_24, R.attr.noneStateTint)
        STATE.POSITIVE -> Pair(R.drawable.ic_twotone_check_box_24, R.attr.positiveStateTint)
        STATE.NEGATIVE -> Pair(R.drawable.ic_twotone_indeterminate_check_box_24, R.attr.negativeStateTint)
    }.let { color: Pair<Int, Int> ->
        holder.icon.background = ContextCompat.getDrawable(context, color.first)
        val value = TypedValue()
        context.theme.resolveAttribute(color.second, value, true)
        holder.icon.backgroundTintList = ColorStateList.valueOf(value.data)
    }
}