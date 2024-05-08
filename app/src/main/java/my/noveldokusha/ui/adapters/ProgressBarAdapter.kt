package my.noveldokusha.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.databinding.ProgressBarBinding
import my.noveldokusha.databinding.ProgressBarHorizontalBinding
import my.noveldokusha.core.utils.inflater
import kotlin.properties.Delegates

class ProgressBarAdapter : RecyclerView.Adapter<ProgressBarAdapter.ViewBinder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
        ViewBinder(ProgressBarBinding.inflate(parent.inflater, parent, false))

    var visible: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) this.notifyDataSetChanged()
    }

    override fun getItemCount() = if (visible) 1 else 0

    override fun onBindViewHolder(holder: ViewBinder, position: Int) = Unit

    class ViewBinder(val viewHolder: ProgressBarBinding) : RecyclerView.ViewHolder(viewHolder.root)
}

class ProgressBarHorizontalAdapter :
    RecyclerView.Adapter<ProgressBarHorizontalAdapter.ViewBinder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
        ViewBinder(ProgressBarHorizontalBinding.inflate(parent.inflater, parent, false))

    var visible: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) this.notifyDataSetChanged()
    }

    override fun getItemCount() = if (visible) 1 else 0

    override fun onBindViewHolder(holder: ViewBinder, position: Int) = Unit

    class ViewBinder(val viewHolder: ProgressBarHorizontalBinding) :
        RecyclerView.ViewHolder(viewHolder.root)
}