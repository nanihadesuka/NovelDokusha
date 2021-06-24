package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageBinding
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageGridviewItemBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiUtils.*

class LibraryPageFragment : BaseFragment
{
	constructor() : super()
	constructor(showCompleted: Boolean) : super()
	{
		this.showCompleted = showCompleted
	}
	
	var showCompleted by Argument_Boolean()
	
	private val viewModel by viewModels<LibraryPageModel>()
	private lateinit var viewHolder: ActivityMainFragmentLibraryPageBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val gridView by lazy { NovelItemAdapter(this@LibraryPageFragment) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentLibraryPageBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewModel.initialization(showCompleted)
		
		viewHolder.gridView.adapter = viewAdapter.gridView
		viewHolder.gridView.itemAnimator = DefaultItemAnimator()
		viewHolder.swipeRefreshLayout.setOnRefreshListener { viewModel.update() }
		
		viewModel.booksWithContextFlow.asLiveData().observe(viewLifecycleOwner) {
			viewAdapter.gridView.setList(it)
		}
		
		viewModel.refreshing.observe(viewLifecycleOwner) { viewHolder.swipeRefreshLayout.isRefreshing = it }
		viewModel.updateNotice.observe(viewLifecycleOwner) {
			if (it.newChapters.isNotEmpty()) notifyUpdated(it.newChapters.joinToString("\n"))
			if (it.failed.isNotEmpty()) notifyUpdatedFails(it.failed.joinToString("\n"))
			if (it.newChapters.isEmpty()) toast("No updates found")
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
		
		NotificationManagerCompat
			.from(requireContext())
			.notify(notificationId, builder.build())
	}
	
}

private class NovelItemAdapter(private val context: BaseFragment) : RecyclerView.Adapter<NovelItemAdapter.ViewBinder>()
{
	private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<BookWithContext>()
	{
		override fun areItemsTheSame(oldItem: BookWithContext, newItem: BookWithContext) = oldItem.book.url == newItem.book.url
		override fun areContentsTheSame(oldItem: BookWithContext, newItem: BookWithContext) = oldItem == newItem
	})
	
	private val list get() = differ.currentList
	
	fun setList(newList: List<BookWithContext>) = differ.submitList(newList)
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBinder
	{
		return ViewBinder(ActivityMainFragmentLibraryPageGridviewItemBinding.inflate(parent.inflater, parent, false))
	}
	
	override fun getItemCount() = list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val viewModel = this.list[position]
		val viewHolder = binder.viewHolder
		
		viewHolder.title.text = viewModel.book.title
		val unreadChaptersCount = viewModel.chaptersCount - viewModel.chaptersReadCount
		viewHolder.unreadChaptersCounter.visibility = if (unreadChaptersCount == 0) View.INVISIBLE else View.VISIBLE
		viewHolder.unreadChaptersCounter.text = unreadChaptersCount.toString()
		
		viewHolder.book.setOnClickListener {
			ChaptersActivity.IntentData(
				context.requireContext(),
				bookMetadata = BookMetadata(url = viewModel.book.url, title = viewModel.book.title)
			).let(context::startActivity)
		}
		viewHolder.book.setOnLongClickListener {
			completedDialog(context, viewModel.book)
			true
		}
	}
	
	inner class ViewBinder(val viewHolder: ActivityMainFragmentLibraryPageGridviewItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}

private fun completedDialog(context: BaseFragment, book: Book) = MaterialDialog(context.requireActivity()).show {
	title(text = book.title)
	checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
	negativeButton(text = "Cancel")
	positiveButton(text = "Ok") {
		val completed = isCheckPromptChecked()
		context.lifecycleScope.launch { bookstore.bookLibrary.update(book.copy(completed = completed)) }
	}
}