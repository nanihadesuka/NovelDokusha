package my.noveldokusha.ui.chaptersList

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorLong
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.ViewAnimationFactory
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.databinding.ActivityChaptersBinding
import my.noveldokusha.databinding.ActivityChaptersListItemBinding
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.*
import java.util.*

@AndroidEntryPoint
class ChaptersActivity : BaseActivity()
{
    class IntentData : Intent, ChapterStateBundle
    {
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(ctx, ChaptersActivity::class.java)
        {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    val viewModel by viewModels<ChaptersViewModel>()
    val viewBind by lazy { ActivityChaptersBinding.inflate(layoutInflater) }
    private val viewAdapter = object
    {
        val chapters by lazy { ChaptersArrayAdapter(this@ChaptersActivity, viewModel) }
    }

    val appBarTransparent = MutableLiveData(true)
    val offsetListener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        appBarTransparent.postValue(verticalOffset >= -10)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)
        setSupportActionBar(viewBind.toolbar)

        viewBind.recyclerView.adapter = viewAdapter.chapters
        viewBind.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBind.recyclerView.itemAnimator = null
        viewBind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.updateCover()
            viewModel.updateDescription()
            viewModel.updateChaptersList()
        }
        viewModel.onFetching.observe(this) { viewBind.swipeRefreshLayout.isRefreshing = it }
        viewModel.chaptersWithContextLiveData.observe(this) {
            viewAdapter.chapters.list = it
        }
        viewModel.selectionModeVisible.distinctUntilChanged().observe(this) { visible ->
            when (visible)
            {
                true -> viewBind.selectionModeBar.fadeInVertical(displacement = 200f)
                false -> viewBind.selectionModeBar.fadeOutVertical(displacement = 200f)
            }
        }

        setupBookInfo(this, viewModel)
        setupSelectionModeBar()

        viewBind.floatingActionButton.setOnClickListener {
            val bookUrl = viewModel.bookMetadata.url
            lifecycleScope.launch(Dispatchers.IO) {
                val lastReadChapter = viewModel.getLastReadChapter()
                if (lastReadChapter == null)
                {
                    toast(getString(R.string.no_chapters))
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    ReaderActivity
                        .IntentData(this@ChaptersActivity, bookUrl = bookUrl, chapterUrl = lastReadChapter)
                        .let(this@ChaptersActivity::startActivity)
                }
            }
        }

        // Stored inside a weakReference, needs to be explicitly referenced in the activity
        viewBind.appBarLayout.addOnOffsetChangedListener(offsetListener)

        val barBackgroundColor = android.R.attr.colorBackground.colorAttrRes(this)
        val barBackgroundColorTransparent = barBackgroundColor.and(0x00FFFFFF)

        appBarTransparent.distinctUntilChanged().observe(this) { transparent ->
            val background = when (transparent)
            {
                true -> barBackgroundColorTransparent
                false -> barBackgroundColor
            }

            ValueAnimator.ofObject(ArgbEvaluator(), window.statusBarColor, background).apply {
                duration = 150
                addUpdateListener {
                    val color = it.animatedValue as Int
                    window.statusBarColor = color
                    viewBind.toolbar.setBackgroundColor(color)
                }
            }.start()
        }

        window.statusBarColor = barBackgroundColorTransparent
        viewBind.toolbar.setBackgroundColor(barBackgroundColorTransparent)
        supportActionBar!!.let {
            it.title = "Chapters"
            it.setDisplayHomeAsUpEnabled(true)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun setupSelectionModeBar()
    {
        viewBind.selectionSelectAll.setOnClickListener {
            val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
            viewModel.selectedChaptersUrl.addAll(chapters.map { it.chapter.url })
            viewAdapter.chapters.notifyDataSetChanged()
        }

        viewBind.selectionSelectAllUnderSelected.setOnClickListener {
            val chapters = viewModel.chaptersWithContextLiveData.value ?: return@setOnClickListener
            val urls = chapters.dropWhile { !viewModel.selectedChaptersUrl.contains(it.chapter.url) }
                .map { it.chapter.url }
            viewModel.selectedChaptersUrl.addAll(urls)
            viewAdapter.chapters.notifyDataSetChanged()
        }

        viewBind.selectionModeSetAsUnread.setOnClickListener {
            viewModel.setSelectedAsUnread()
        }

        viewBind.selectionModeSetAsRead.setOnClickListener {
            viewModel.setSelectedAsRead()
        }

        viewBind.selectionModeDownload.setOnClickListener {
            viewModel.downloadSelected()
        }

        viewBind.selectionModeDeleteDownload.setOnClickListener {
            viewModel.deleteDownloadSelected()
        }

        viewBind.selectionClose.setOnClickListener {
            viewModel.closeSelectionMode()
            viewAdapter.chapters.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() = when
    {
        viewBind.selectionModeBar.isVisible ->
        {
            viewModel.closeSelectionMode()
            viewAdapter.chapters.notifyDataSetChanged()
        }
        else -> super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.chapters_list_menu__appbar, menu)

        menu.findItem(R.id.action_library_bookmark)!!.also { bookmarkItem ->
            viewModel.isInLibrary.observe(this) {
                setMenuIconLibraryState(it, bookmarkItem)
            }
        }

        menu.findItem(R.id.action_search_by_title)!!.also { menuItem ->

            val searchView = menuItem.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
            {
                override fun onQueryTextSubmit(query: String?): Boolean = true

                override fun onQueryTextChange(newText: String?): Boolean
                {
                    lifecycleScope.launch(Dispatchers.Default) {
                        viewModel.chaptersFilterFlow.emit(newText ?: "")
                    }
                    return true
                }
            })
        }

        return true
    }

    private fun setMenuIconLibraryState(isInLibrary: Boolean, item: MenuItem)
    {
        item.icon.setTint(if (isInLibrary) R.color.dark_orange_red.colorIdRes(this) else Color.GRAY)
        item.isChecked = isInLibrary
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
    {
        R.id.action_library_bookmark ->
        {
            val msg = if (!item.isChecked) R.string.added_to_library else R.string.removed_from_library
            toast(getString(msg))
            viewModel.toggleBookmark()
            true
        }
        R.id.action_filter ->
        {
            appPreferences.CHAPTERS_SORT_ASCENDING = when (appPreferences.CHAPTERS_SORT_ASCENDING)
            {
                AppPreferences.TERNARY_STATE.active -> AppPreferences.TERNARY_STATE.inverse
                AppPreferences.TERNARY_STATE.inverse -> AppPreferences.TERNARY_STATE.active
                AppPreferences.TERNARY_STATE.inactive -> AppPreferences.TERNARY_STATE.active
            }
            true
        }
        android.R.id.home -> this.onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }
}

private class ChaptersArrayAdapter(
    private val context: BaseActivity,
    private val viewModel: ChaptersViewModel
) : MyListAdapter<ChapterWithContext, ChaptersArrayAdapter.ViewHolder>()
{
    override fun areItemsTheSame(old: ChapterWithContext, new: ChapterWithContext) =
        old.chapter.url == new.chapter.url

    override fun areContentsTheSame(old: ChapterWithContext, new: ChapterWithContext) = old == new

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ActivityChaptersListItemBinding.inflate(parent.inflater, parent, false))

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
    {
        val itemData = list[position]
        val itemBind = viewHolder.viewBind

        itemBind.title.text = itemData.chapter.title
        itemBind.title.alpha = if (itemData.chapter.read) 0.5f else 1.0f
        itemBind.downloaded.visibility = if (itemData.downloaded) View.VISIBLE else View.INVISIBLE
        itemBind.currentlyReading.visibility = if (itemData.lastReadChapter) View.VISIBLE else View.INVISIBLE
        itemBind.selected.visibility = if (viewModel.selectedChaptersUrl.contains(itemData.chapter.url)) View.VISIBLE else View.INVISIBLE

        itemBind.root.setOnClickListener {
            if (viewModel.selectedChaptersUrl.isNotEmpty())
                toggleItemSelection(itemData, itemBind.selected)
            else
            {
                ReaderActivity
                    .IntentData(context, bookUrl = viewModel.bookMetadata.url, chapterUrl = itemData.chapter.url)
                    .let(context::startActivity)
            }
        }

        itemBind.root.setOnLongClickListener {
            toggleItemSelection(itemData, itemBind.selected)
            true
        }
    }

    fun toggleItemSelection(itemData: ChapterWithContext, view: View)
    {
        fun <T> MutableSet<T>.removeOrAdd(value: T) =
            contains(value).also { if (it) remove(value) else add(value) }

        val isRemoved = viewModel.selectedChaptersUrl.removeOrAdd(itemData.chapter.url)
        view.visibility = if (isRemoved) View.INVISIBLE else View.VISIBLE
        viewModel.updateSelectionModeBarState()
    }

    inner class ViewHolder(val viewBind: ActivityChaptersListItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}

private fun setupBookInfo(activity: ChaptersActivity, viewModel: ChaptersViewModel)
{
    val viewBind = activity.viewBind.header

    viewBind.bookTitle.text = viewModel.bookMetadata.title
    viewBind.sourceName.text = scraper.getCompatibleSource(viewModel.bookMetadata.url)?.name ?: ""

    viewModel.book.observe(activity) {
        val isVisible = it != null && it.description.isNotBlank()
        viewBind.bookDescription.visibility = if (isVisible) View.VISIBLE else View.GONE
        viewBind.bookDescription.text = it?.description ?: ""

        it?.coverImageUrl.let { imgUrl ->
            Glide.with(activity)
                .load(imgUrl)
                .error(R.drawable.md_transparent)
                .transform(RoundedCorners(32))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(viewBind.coverImageBackground)
        }
    }

    viewModel.chaptersWithContextLiveData.observe(activity) { list ->
        viewBind.numberOfChapters.text = list.size.toString()
    }
    viewModel.onError.observe(activity) {
        viewBind.errorMessage.text = it
    }
    viewModel.onErrorVisibility.observe(activity) {
        viewBind.errorMessage.visibility = it
    }
    viewBind.errorMessage.setOnLongClickListener(object : View.OnLongClickListener
    {
        private var expand: Boolean = false
        override fun onLongClick(v: View?): Boolean
        {
            expand = !expand
            viewBind.errorMessage.maxLines = if (expand) 100 else 10
            return true
        }
    })
    viewBind.databaseSearchButton.setOnClickListener {
        DatabaseSearchResultsActivity
            .IntentData(activity, "https://www.novelupdates.com/", DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookMetadata.title))
            .let(activity::startActivity)
    }
    viewBind.webpageOpenButton.setOnClickListener {
        Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse(viewModel.bookMetadata.url)
        }.let(activity::startActivity)
    }
}

