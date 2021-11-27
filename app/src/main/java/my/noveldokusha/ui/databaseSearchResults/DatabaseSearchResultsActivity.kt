package my.noveldokusha.ui.databaseSearchResults

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.*
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiAdapters.ProgressBarAdapter
import my.noveldokusha.uiUtils.*
import java.util.*

@AndroidEntryPoint
class DatabaseSearchResultsActivity : BaseActivity()
{
    class IntentData : Intent, DatabaseSearchResultsStateBundle
    {
        override var databaseUrlBase by Extra_String()
        override var text by Extra_String()
        override var genresIncludeId by Extra_StringArrayList()
        override var genresExcludeId by Extra_StringArrayList()
        override var searchMode by Extra_String()
        override var authorName by Extra_String()
        override var urlAuthorPage by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseUrlBase: String, input: SearchMode) : super(ctx, DatabaseSearchResultsActivity::class.java)
        {
            set(databaseUrlBase, input)
        }
    }

    sealed class SearchMode
    {
        data class Text(val text: String) : SearchMode()
        data class Genres(val genresIncludeId: ArrayList<String>, val genresExcludeId: ArrayList<String>) : SearchMode()
        data class AuthorSeries(val authorName: String, val urlAuthorPage: String) : SearchMode()
    }

    private val viewModel by viewModels<DatabaseSearchResultsModel>()
    private val viewBind by lazy { ActivityDatabaseSearchResultsBinding.inflate(layoutInflater) }
    private val viewAdapter = object
    {
        val recyclerView by lazy { ChaptersArrayAdapter(this@DatabaseSearchResultsActivity, viewModel.database.baseUrl) }
        val progressBar by lazy { ProgressBarAdapter() }
    }
    private val viewLayoutManager = object
    {
        val recyclerView by lazy { LinearLayoutManager(this@DatabaseSearchResultsActivity) }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)
        setSupportActionBar(viewBind.toolbar)

        viewBind.recyclerView.adapter = ConcatAdapter(viewAdapter.recyclerView, viewAdapter.progressBar)
        viewBind.recyclerView.layoutManager = viewLayoutManager.recyclerView
        viewBind.recyclerView.itemAnimator = DefaultItemAnimator()

        viewBind.recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
            viewModel.fetchIterator.fetchTrigger {
                val pos = viewLayoutManager.recyclerView.findLastVisibleItemPosition()
                return@fetchTrigger pos >= viewAdapter.recyclerView.itemCount - 3
            }
        }

        viewModel.fetchIterator.onCompletedEmpty.observe(this) {
            viewBind.noResultsMessage.visibility = View.VISIBLE
        }
        viewModel.fetchIterator.onFetching.observe(this) {
            viewAdapter.progressBar.visible = it
        }
        viewModel.fetchIterator.onSuccess.observe(this) {
            viewAdapter.recyclerView.list = it
        }

        supportActionBar!!.let {
            it.title = getString(R.string.database) + ": " + viewModel.database.name.capitalize(Locale.ROOT)
            it.subtitle = when (val res = viewModel.input)
            {
                is SearchMode.AuthorSeries -> getString(R.string.search_by_author) + ": " + res.authorName
                is SearchMode.Genres -> getString(R.string.search_by_genre)
                is SearchMode.Text -> getString(R.string.search_by_title) + ": " + res.text
            }
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
    {
        android.R.id.home -> this.onBackPressed().let { true }
        else -> super.onOptionsItemSelected(item)
    }

}

private class ChaptersArrayAdapter(
    private val context: BaseActivity,
    private val databaseUrlBase: String
) : MyListAdapter<BookMetadata, ChaptersArrayAdapter.ViewHolder>()
{
    override fun areItemsTheSame(old: BookMetadata, new: BookMetadata) = old.url == new.url
    override fun areContentsTheSame(old: BookMetadata, new: BookMetadata) = old == new

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(BookListItemBinding.inflate(parent.inflater, parent, false))

    override fun onBindViewHolder(binder: ViewHolder, position: Int)
    {
        val itemData = this.list[position]
        val itemBind = binder.viewBind
        itemBind.title.text = itemData.title
        itemBind.title.setOnClickListener {
            DatabaseBookInfoActivity
                .IntentData(context, databaseUrlBase = databaseUrlBase, bookMetadata = itemData)
                .let(context::startActivity)
        }
    }

    inner class ViewHolder(val viewBind: BookListItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}
