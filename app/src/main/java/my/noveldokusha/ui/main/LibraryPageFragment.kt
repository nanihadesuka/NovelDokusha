package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageBinding
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageGridviewItemBinding
import my.noveldokusha.ui.chaptersList.ChaptersActivity

class LibraryPageFragment : Fragment()
{
	companion object
	{
		fun createInstance(showCompleted: Boolean) = LibraryPageFragment().apply {
			arguments = Bundle().also {
				it.putBoolean("showCompleted", showCompleted)
			}
		}
	}
	
	private val extras = object
	{
		fun showCompleted() = arguments!!.getBoolean("showCompleted")
	}
	
	private val viewModel by viewModels<LibraryPageModel>()
	private lateinit var viewHolder: ActivityMainFragmentLibraryPageBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val gridView by lazy { NovelItemAdapter(viewModel.books) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentLibraryPageBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewModel.initialization(extras.showCompleted())
		
		viewHolder.gridView.adapter = viewAdapter.gridView
		viewHolder.gridView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.update() }
		
		viewModel.booksLiveData.observe(viewLifecycleOwner) { viewAdapter.gridView.setList(it) }
		
		viewModel.refreshing.observe(viewLifecycleOwner) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		viewModel.updateNotice.observe(viewLifecycleOwner) {
			if (it.newChapters.isNotEmpty()) notifyUpdated(it.newChapters.joinToString("\n"))
			if (it.failed.isNotEmpty()) notifyUpdatedFails(it.failed.joinToString("\n"))
		}
		
		return viewHolder.root
	}
	
	private fun completedDialog(book: bookstore.Book) = MaterialDialog(requireActivity()).show {
		title(text = book.title)
		checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
		negativeButton(text = "Cancel")
		positiveButton(text = "Ok") {
			val completed = isCheckPromptChecked()
			lifecycleScope.launch { bookstore.bookLibrary.update(book.copy(completed = completed)) }
		}
	}
	
	private fun notifyUpdated(text: String)
	{
		notify("New chapters found", text)
	}
	
	private fun notifyUpdatedFails(text: String)
	{
		notify("Update error", text)
	}
	
	private fun notify(title: String, text: String, channel_ID: String = title)
	{
		val notificationId = channel_ID.hashCode()
		
		val builder = NotificationCompat.Builder(requireContext(), channel_ID)
			.setSmallIcon(R.drawable.ic_baseline_update_24)
			.setContentTitle(title)
			.setContentText(text)
			.setStyle(NotificationCompat.BigTextStyle().bigText(text))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		
		with(NotificationManagerCompat.from(requireContext())) {
			notify(notificationId, builder.build())
		}
	}
	
	inner class NovelItemAdapter(private val list: MutableList<LibraryPageModel.BookItem>) :
		RecyclerView.Adapter<NovelItemAdapter.ViewBinder>()
	{
		private inner class Diff(private val new: List<LibraryPageModel.BookItem>) : DiffUtil.Callback()
		{
			override fun getOldListSize(): Int = list.size
			override fun getNewListSize(): Int = new.size
			override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].data.url == new[newPos].data.url
			override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean = list[oldPos].data == new[newPos].data
		}
		
		fun setList(newList: List<LibraryPageModel.BookItem>) = DiffUtil.calculateDiff(Diff(newList)).let {
			val isEmpty = list.isEmpty()
			list.clear()
			list.addAll(newList)
			if (isEmpty) notifyDataSetChanged() else it.dispatchUpdatesTo(this)
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
		{
			return ViewBinder(ActivityMainFragmentLibraryPageGridviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		}
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemView = binder.viewHolder
			itemView.title.text = itemData.data.title
			itemView.unreadChaptersCounter.visibility = View.INVISIBLE
			
			itemData.numberOfUnreadChapters.removeObservers(viewLifecycleOwner)
			itemData.numberOfUnreadChapters.observe(viewLifecycleOwner) {
				itemView.unreadChaptersCounter.text = it.toString()
				itemView.unreadChaptersCounter.visibility = if (it == 0) View.INVISIBLE else View.VISIBLE
			}
			
			itemView.book.setOnClickListener {
				val intent = ChaptersActivity.Extras(bookUrl = itemData.data.url, bookTitle = itemData.data.title).intent(requireActivity())
				this@LibraryPageFragment.startActivity(intent)
			}
			
			itemView.book.setOnLongClickListener {
				completedDialog(itemData.data)
				true
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityMainFragmentLibraryPageGridviewItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}