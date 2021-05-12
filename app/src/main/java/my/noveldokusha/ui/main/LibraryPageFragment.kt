package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import kotlinx.coroutines.launch
import my.noveldokusha.BookMetadata
import my.noveldokusha.R
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageBinding
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageGridviewItemBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.ui.chaptersList.ChaptersActivity

class LibraryPageFragment : BaseFragment()
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
		val gridView by lazy { NovelItemAdapter(this@LibraryPageFragment, viewModel.books) }
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
	
}

private class NovelItemAdapter(
	private val context: BaseFragment,
	private val list: MutableList<LibraryPageModel.BookItem>
) : RecyclerView.Adapter<NovelItemAdapter.ViewBinder>()
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
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		binder.setObservers(viewModel)
		
		viewHolder.title.text = viewModel.data.title
		viewHolder.unreadChaptersCounter.visibility = View.INVISIBLE
		viewHolder.book.setOnClickListener {
			ChaptersActivity
				.IntentData(context.requireActivity(), bookMetadata = BookMetadata(url = viewModel.data.url, title = viewModel.data.title))
				.let(context.requireActivity()::startActivity)
		}
		viewHolder.book.setOnLongClickListener {
			completedDialog(context, viewModel.data)
			true
		}
	}
	
	inner class ViewBinder(val viewHolder: ActivityMainFragmentLibraryPageGridviewItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	{
		var viewModelLast: LibraryPageModel.BookItem? = null
		val numberOfUnreadChaptersObserver = Observer<Int> {
			viewHolder.unreadChaptersCounter.text = it.toString()
			viewHolder.unreadChaptersCounter.visibility = if (it == 0) View.INVISIBLE else View.VISIBLE
		}
		
		fun setObservers(viewModel: LibraryPageModel.BookItem)
		{
			viewModelLast?.numberOfUnreadChapters?.removeObserver(numberOfUnreadChaptersObserver)
			viewModel.numberOfUnreadChapters.observe(context.viewLifecycleOwner, numberOfUnreadChaptersObserver)
			viewModelLast = viewModel
		}
		
	}
}

private fun completedDialog(context: BaseFragment, book: bookstore.Book) = MaterialDialog(context.requireActivity()).show {
	title(text = book.title)
	checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
	negativeButton(text = "Cancel")
	positiveButton(text = "Ok") {
		val completed = isCheckPromptChecked()
		context.lifecycleScope.launch { bookstore.bookLibrary.update(book.copy(completed = completed)) }
	}
}