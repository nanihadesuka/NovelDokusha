package my.noveldokusha.ui.chaptersList

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterWithContext
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.reader.ReaderActivity
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.*
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@AndroidEntryPoint
class ChaptersActivity : ComponentActivity()
{
    class IntentData : Intent, ChapterStateBundle
    {
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, bookMetadata: BookMetadata) : super(
            ctx,
            ChaptersActivity::class.java
        )
        {
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    lateinit var appPreferences: AppPreferences
    val viewModel by viewModels<ChaptersViewModel>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val listState = rememberLazyListState()

            Theme(appPreferences = appPreferences) {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colors.isLight
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }
                val sourceName = remember(viewModel.book.url) {
                    scraper.getCompatibleSource(viewModel.bookUrl)?.name ?: ""
                }

                Box {
                    ChaptersListView(
                        header = {
                            HeaderView(
                                bookTitle = viewModel.bookTitle,
                                sourceName = sourceName,
                                numberOfChapters = 34,
                                bookCover = viewModel.book.coverImageUrl,
                                description = viewModel.book.description,
                                onSearchBookInDatabase = ::searchBookInDatabase,
                                onOpenInBrowser = ::openInBrowser,
                            )
                        },
                        list = viewModel.chaptersWithContext,
                        selectedChapters = viewModel.selectedChaptersUrl,
                        listState = listState,
                        onClick = ::onClickChapter,
                        onLongClick = ::onLongClickChapter
                    )
                    ToolBarMode(
                        bookTitle = viewModel.book.title,
                        isBookmarked = viewModel.book.inLibrary,
                        listState = listState,
                        onClickBookmark = ::bookmarkToggle,
                        onClickSortChapters = viewModel::toggleChapterSort,
                        onClickMoreSettings = { },
                        MoreSettingsView = {},
                    )
                    AnimatedVisibility(
                        visible = viewModel.selectedChaptersUrl.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 125.dp),
                    ) {
                        SelectionToolsBar(
                            onDeleteDownload = viewModel::deleteDownloadSelected,
                            onDownload = viewModel::downloadSelected,
                            onSetRead = viewModel::setSelectedAsRead,
                            onSetUnread = viewModel::setSelectedAsUnread,
                            onSelectAllChapters = viewModel::selectAll,
                            onSelectAllChaptersAfterSelectedOnes = viewModel::selectAllAfterSelectedOnes,
                            onCloseSelectionbar = viewModel::closeSelectionMode,
                        )
                    }
                }
            }
        }
    }

    fun onClickChapter(chapter: ChapterWithContext)
    {
        when
        {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            viewModel.selectedChaptersUrl.isNotEmpty() ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
            else -> openBookAtChapter(chapter.chapter.url)
        }
    }

    fun onLongClickChapter(chapter: ChapterWithContext)
    {
        when
        {
            viewModel.selectedChaptersUrl.containsKey(chapter.chapter.url) ->
                viewModel.selectedChaptersUrl.remove(chapter.chapter.url)
            else ->
                viewModel.selectedChaptersUrl[chapter.chapter.url] = Unit
        }
    }

//        viewBind.swipeRefreshLayout.setOnRefreshListener {
//            viewModel.updateCover()
//            viewModel.updateDescription()
//            viewModel.updateChaptersList()
//        }


//        viewBind.floatingActionButton.setOnClickListener {
//            val bookUrl = viewModel.bookMetadata.url
//            lifecycleScope.launch(Dispatchers.IO) {
//                val lastReadChapter = viewModel.getLastReadChapter()
//                if (lastReadChapter == null)
//                {
//                    toast(getString(R.string.no_chapters))
//                    return@launch
//                }
//
//                withContext(Dispatchers.Main) {
//                    ReaderActivity
//                        .IntentData(this@ChaptersActivity, bookUrl = bookUrl, chapterUrl = lastReadChapter)
//                        .let(this@ChaptersActivity::startActivity)
//                }
//            }
//        }

    fun bookmarkToggle()
    {
        lifecycleScope.launch {
            val isBookmarked = viewModel.toggleBookmark()
            val msg = if (isBookmarked) R.string.added_to_library else R.string.removed_from_library
            withContext(Dispatchers.Main) {
                toast(getString(msg))
            }
        }
    }

    fun searchBookInDatabase()
    {
        DatabaseSearchResultsActivity
            .IntentData(
                this,
                "https://www.novelupdates.com/",
                DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookTitle)
            )
            .let(this::startActivity)
    }

    fun openInBrowser()
    {
        Intent(Intent.ACTION_VIEW).also {
            it.data = Uri.parse(viewModel.bookMetadata.url)
        }.let(this::startActivity)
    }


    fun openBookAtChapter(chapterUrl: String)
    {
        ReaderActivity
            .IntentData(
                this,
                bookUrl = viewModel.bookMetadata.url,
                chapterUrl = chapterUrl
            )
            .let(::startActivity)
    }
}

//private class ChaptersArrayAdapter(
//    private val context: BaseActivity,
//    private val viewModel: ChaptersViewModel
//) : MyListAdapter<ChapterWithContext, ChaptersArrayAdapter.ViewHolder>()
//{
//    override fun areItemsTheSame(old: ChapterWithContext, new: ChapterWithContext) =
//        old.chapter.url == new.chapter.url
//
//    override fun areContentsTheSame(old: ChapterWithContext, new: ChapterWithContext) = old == new
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
//        ViewHolder(ActivityChaptersListItemBinding.inflate(parent.inflater, parent, false))
//
//    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
//    {
//        val itemData = list[position]
//        val itemBind = viewHolder.viewBind
//
//        itemBind.title.text = itemData.chapter.title
//        itemBind.title.alpha = if (itemData.chapter.read) 0.5f else 1.0f
//        itemBind.downloaded.visibility = if (itemData.downloaded) View.VISIBLE else View.INVISIBLE
//        itemBind.currentlyReading.visibility =
//            if (itemData.lastReadChapter) View.VISIBLE else View.INVISIBLE
//        itemBind.selected.visibility =
//            if (viewModel.selectedChaptersUrl.contains(itemData.chapter.url)) View.VISIBLE else View.INVISIBLE
//
//        itemBind.root.setOnClickListener {
//            if (viewModel.selectedChaptersUrl.isNotEmpty())
////                toggleItemSelection(itemData, itemBind.selected)
//            else
//            {
//                ReaderActivity
//                    .IntentData(
//                        context,
//                        bookUrl = viewModel.bookMetadata.url,
//                        chapterUrl = itemData.chapter.url
//                    )
//                    .let(context::startActivity)
//            }
//        }
//
//        itemBind.root.setOnLongClickListener {
////            toggleItemSelection(itemData, itemBind.selected)
//            true
//        }
//    }
//
////    fun toggleItemSelection(itemData: ChapterWithContext, view: View)
////    {
////        fun <T> MutableSet<T>.removeOrAdd(value: T) =
////            contains(value).also { if (it) remove(value) else add(value) }
////
////        val isRemoved = viewModel.selectedChaptersUrl.removeOrAdd(itemData.chapter.url)
////        view.visibility = if (isRemoved) View.INVISIBLE else View.VISIBLE
////        viewModel.updateSelectionModeBarState()
////    }
//
//    inner class ViewHolder(val viewBind: ActivityChaptersListItemBinding) :
//        RecyclerView.ViewHolder(viewBind.root)
//}

//    viewBind.sourceName.text = scraper.getCompatibleSource(viewModel.bookMetadata.url)?.name ?: ""

//    viewBind.databaseSearchButton.setOnClickListener {
//        DatabaseSearchResultsActivity
//            .IntentData(activity, "https://www.novelupdates.com/", DatabaseSearchResultsActivity.SearchMode.Text(viewModel.bookMetadata.title))
//            .let(activity::startActivity)
//    }
//    viewBind.webpageOpenButton.setOnClickListener {
//        Intent(Intent.ACTION_VIEW).also {
//            it.data = Uri.parse(viewModel.bookMetadata.url)
//        }.let(activity::startActivity)
//    }

