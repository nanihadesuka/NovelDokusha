package my.noveldokusha.ui.databaseBookInfo

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import my.noveldokusha.BookMetadata
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityDatabaseBookInfoBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.databaseSearchResults.DatabaseSearchResultsActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.*
import java.util.*
import kotlin.collections.ArrayList

class DatabaseBookInfoActivity : BaseActivity()
{
	class IntentData : Intent
	{
		val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
		var databaseUrlBase by Extra_String()
		private var bookUrl by Extra_String()
		private var bookTitle by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseUrlBase: String, bookMetadata: BookMetadata) : super(ctx, DatabaseBookInfoActivity::class.java)
		{
			this.databaseUrlBase = databaseUrlBase
			this.bookUrl = bookMetadata.url
			this.bookTitle = bookMetadata.title
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewModel by viewModelsFactory {
		DatabaseBookInfoModel(
			database = scrubber.getCompatibleDatabase(extras.databaseUrlBase)!!,
			bookMetadata = extras.bookMetadata
		)
	}
	private val viewBind by lazy { ActivityDatabaseBookInfoBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val relatedBooks by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.database.baseUrl) }
		val similarRecommended by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.database.baseUrl) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		
		viewBind.relatedBooks.adapter = viewAdapter.relatedBooks
		viewBind.similarRecommended.adapter = viewAdapter.similarRecommended
		viewBind.title.text = viewModel.bookMetadata.title
		viewBind.globalSourceSearch.setOnClickListener {
			GlobalSourceSearchActivity.IntentData(
				this,
				input = viewModel.bookMetadata.title
			).let(this@DatabaseBookInfoActivity::startActivity)
		}
		
		viewModel.bookDataLiveData.observe(this) { res ->
			val data = if (res is Response.Success) res.data else return@observe
			
			if (data.genres.isNotEmpty())
			{
				viewBind.genres.text = data.genres.joinToString(" · ")
				viewBind.genres.fadeIn(700)
				viewBind.genres.setOnClickListener {
					lifecycleScope.launch(Dispatchers.IO) {
						val databaseGenres = (viewModel.database.getSearchGenres() as? Response.Success ?: return@launch).data
						if (!isActive) return@launch
						val input = DatabaseSearchResultsActivity.SearchMode.Genres(
							genresIncludeId = ArrayList(data.genres.mapNotNull { databaseGenres.get(it) }),
							genresExcludeId = arrayListOf()
						)
						val intent =
							DatabaseSearchResultsActivity.IntentData(this@DatabaseBookInfoActivity, viewModel.database.baseUrl, input)
						startActivity(intent)
					}
				}
			}
			else
				viewBind.genres.visibility = View.GONE
			
			Glide.with(this)
				.load(data.coverImageUrl)
				.error(R.drawable.ic_baseline_error_outline_24)
				.transform(RoundedCorners(32))
				.transition(DrawableTransitionOptions.withCrossFade())
				.into(viewBind.coverImage)
			
			viewBind.linearLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
			var imgFullWidth = false
			viewBind.coverImage.setOnClickListener {
				imgFullWidth = !imgFullWidth
				viewBind.coverImage.updateLayoutParams {
					width = if (imgFullWidth) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
					height = if (imgFullWidth) ViewGroup.LayoutParams.WRAP_CONTENT else spToPx(220f)
				}
			}
			
			if (data.authors.isNotEmpty())
			{
				val author = data.authors.first()
				viewBind.authors.text = author.name
				viewBind.authors.fadeIn(700)
				if (author.url != null)
					viewBind.authors.setOnClickListener {
						lifecycleScope.launch(Dispatchers.IO) {
							val input = DatabaseSearchResultsActivity.SearchMode.AuthorSeries(
								authorName = author.name, urlAuthorPage = author.url
							)
							val intent =
								DatabaseSearchResultsActivity.IntentData(this@DatabaseBookInfoActivity, viewModel.database.baseUrl, input)
							startActivity(intent)
						}
					}
			}
			else
				viewBind.authors.visibility = View.GONE
			
			viewBind.description.text = data.description.trim()
			viewBind.alternativeTitles.text = data.alternativeTitles.filterNot { it.isBlank() }.joinToString("\n\n")
			viewBind.tags.text = data.tags.joinToString(" · ").ifEmpty { "No tags" }
			viewBind.bookType.text = data.bookType
			
			viewBind.description.fadeIn(700)
			viewBind.alternativeTitles.fadeIn(700)
			viewBind.tags.fadeIn(700)
			viewBind.bookType.fadeIn(700)
			
			viewAdapter.relatedBooks.list = data.relatedBooks.toList()
			data.relatedBooks.isEmpty().let { empty ->
				viewBind.relatedBooks.visibility = if (empty) View.GONE else View.VISIBLE
				viewBind.relatedBooksNoEntries.visibility = if (empty) View.VISIBLE else View.GONE
			}
			
			viewAdapter.similarRecommended.list = data.similarRecommended.toList()
			data.similarRecommended.isEmpty().let { empty ->
				viewBind.similarRecommended.visibility = if (empty) View.GONE else View.VISIBLE
				viewBind.similarRecommendedNoEntries.visibility = if (empty) View.VISIBLE else View.GONE
			}
			
			viewAdapter.relatedBooks.notifyDataSetChanged()
			viewAdapter.similarRecommended.notifyDataSetChanged()
		}
		
		supportActionBar!!.let {
			it.title = "Book info"
			it.subtitle = viewModel.database.name.capitalize(Locale.ROOT)
			it.setDisplayHomeAsUpEnabled(true)
		}
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		android.R.id.home ->
		{
			this.onBackPressed()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}
}

private class BookArrayAdapter(
	private val context: BaseActivity,
	private val databaseUrlBase: String
) : MyListAdapter<BookMetadata, BookArrayAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: BookMetadata, new: BookMetadata) = old.url == new.url
	override fun areContentsTheSame(old: BookMetadata, new: BookMetadata) = old == new
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(BookListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
	{
		val itemData = list[position]
		val itemBind = viewHolder.viewBind
		itemBind.title.text = itemData.title
		itemBind.title.setOnClickListener {
			DatabaseBookInfoActivity.IntentData(
				context,
				databaseUrlBase = databaseUrlBase,
				bookMetadata = itemData
			).let(context::startActivity)
		}
	}
	
	inner class ViewHolder(val viewBind: BookListItemBinding) : RecyclerView.ViewHolder(viewBind.root)
}
