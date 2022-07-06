package my.noveldokusha.ui.adapters

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class MyListAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>()
{
	abstract fun areItemsTheSame(old: T, new: T): Boolean
	abstract fun areContentsTheSame(old: T, new: T): Boolean
	val listDiffer: AsyncListDiffer<T> = AsyncListDiffer(this, object : DiffUtil.ItemCallback<T>()
	{
		override fun areItemsTheSame(oldItem: T, newItem: T) = this@MyListAdapter.areItemsTheSame(oldItem, newItem)
		override fun areContentsTheSame(oldItem: T, newItem: T) = this@MyListAdapter.areContentsTheSame(oldItem, newItem)
	})
	
	var list: List<T>
		get() = listDiffer.currentList
		set(value) = listDiffer.submitList(value)
	
	override fun getItemCount() = list.size
}