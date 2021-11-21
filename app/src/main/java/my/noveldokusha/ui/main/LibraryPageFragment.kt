package my.noveldokusha.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageBinding
import my.noveldokusha.databinding.ActivityMainFragmentLibraryPageGridviewItemBinding
import my.noveldokusha.services.LibraryUpdateService
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.*

class LibraryPageFragment : BaseFragment
{
	constructor() : super()
	constructor(showCompleted: Boolean) : super()
	{
		this.showCompleted = showCompleted
	}
	
	var showCompleted by Argument_Boolean()
	
	private val viewModel by viewModelsFactory { LibraryPageModel(showCompleted) }
	private lateinit var viewBind: ActivityMainFragmentLibraryPageBinding
	private lateinit var viewAdapter: Adapter
	private lateinit var viewLayout: Layout
	
	private inner class Adapter
	{
		val gridView by lazy { NovelItemAdapter(requireContext()) }
	}
	
	private inner class Layout
	{
		val gridView = GridLayoutManager(requireContext(), 2).also {
			it.spanCount = if (this@LibraryPageFragment.isOnPortraitMode()) 2 else 4
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewBind = ActivityMainFragmentLibraryPageBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		viewLayout = Layout()
		
		viewBind.gridView.adapter = viewAdapter.gridView
		viewBind.gridView.updatePadding(bottom = if (isOnPortraitMode()) spToPx(300f) else spToPx(50f))
		viewBind.gridView.layoutManager = viewLayout.gridView
		viewBind.gridView.itemAnimator = DefaultItemAnimator()
		viewBind.swipeRefreshLayout.setOnRefreshListener {
			LibraryUpdateService.start(requireActivity(), viewModel.showCompleted)
			viewBind.swipeRefreshLayout.isRefreshing = false
		}
		
		viewModel.booksWithContextFlow.asLiveData().observe(viewLifecycleOwner) {
			viewAdapter.gridView.list = it
		}
		
		return viewBind.root
	}
}

private class NovelItemAdapter(private val ctx: Context) : MyListAdapter<BookWithContext, NovelItemAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: BookWithContext, new: BookWithContext) = old.book.url == new.book.url
	override fun areContentsTheSame(old: BookWithContext, new: BookWithContext) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(ActivityMainFragmentLibraryPageGridviewItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val viewData = list[position]
		val viewBind = viewHolder.viewBind
		
		viewBind.title.text = viewData.book.title
		val unreadChaptersCount = viewData.chaptersCount - viewData.chaptersReadCount
		viewBind.unreadChaptersCounter.visibility = if (unreadChaptersCount == 0) View.INVISIBLE else View.VISIBLE
		viewBind.unreadChaptersCounter.text = unreadChaptersCount.toString()
		
		viewBind.book.setOnClickListener {
			ChaptersActivity.IntentData(
				ctx,
				bookMetadata = BookMetadata(url = viewData.book.url, title = viewData.book.title)
			).let(ctx::startActivity)
		}
		viewBind.book.setOnLongClickListener {
			completedDialog(ctx, viewData.book)
			true
		}
	}
	
	inner class ViewHolder(val viewBind: ActivityMainFragmentLibraryPageGridviewItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}

private fun completedDialog(ctx: Context, book: Book) = MaterialDialog(ctx).show {
	title(text = book.title)
	checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
	negativeButton(text = "Cancel")
	positiveButton(text = "Ok") {
		val completed = isCheckPromptChecked()
		CoroutineScope(Dispatchers.IO).launch { bookstore.bookLibrary.update(book.copy(completed = completed)) }
	}
}