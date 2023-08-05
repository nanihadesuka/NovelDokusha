package my.noveldokusha.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.l4digital.fastscroll.FastScroller
import my.noveldokusha.R

class myFastScroll(context: Context, attrs: AttributeSet? = null) : FastScroller(context, attrs) {
    private val myRecyclerViewId: Int

    init {
        val sa = context.obtainStyledAttributes(attrs, R.styleable.myFastScroll)
        myRecyclerViewId =
            sa.getResourceId(R.styleable.myFastScroll_attachedRecyclerView, ResourcesCompat.ID_NULL)
        sa.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val accentColor = TypedValue().let {
            context.theme.resolveAttribute(R.attr.colorAccent, it, true)
            it.data
        }

        setBubbleColor(accentColor)
        setHandleColor(accentColor)

        if (myRecyclerViewId != ResourcesCompat.ID_NULL) {
            val viewGroup = parent as ViewGroup
            val recyclerView = viewGroup.findViewById<RecyclerView>(myRecyclerViewId)
            attachRecyclerView(recyclerView)
        }
    }
}