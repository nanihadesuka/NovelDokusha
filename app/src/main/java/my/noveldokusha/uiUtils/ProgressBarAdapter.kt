package my.noveldokusha.uiUtils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.databinding.ProgressBarBinding
import kotlin.properties.Delegates

class ProgressBarAdapter : RecyclerView.Adapter<ProgressBarAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder =
		ViewBinder(ProgressBarBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	
	var visible: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
		if (oldValue != newValue) this.notifyDataSetChanged()
	}
	
	override fun getItemCount() = if (visible) 1 else 0
	
	override fun onBindViewHolder(holder: ViewBinder, position: Int) = Unit
	
	inner class ViewBinder(val viewHolder: ProgressBarBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
