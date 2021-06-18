package my.noveldokusha.ui.databaseBookInfo

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
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
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.fadeIn
import my.noveldokusha.uiUtils.inflater
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
	private val viewModel by viewModels<DatabaseBookInfoModel>()
	private val viewHolder by lazy { ActivityDatabaseBookInfoBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val relatedBooks by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.relatedBooks, viewModel.database.baseUrl) }
		val similarRecommended by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.similarRecommended, viewModel.database.baseUrl) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(database = scrubber.getCompatibleDatabase(extras.databaseUrlBase)!!, bookMetadata = extras.bookMetadata)
		
		viewHolder.relatedBooks.adapter = viewAdapter.relatedBooks
		viewHolder.similarRecommended.adapter = viewAdapter.similarRecommended
		viewHolder.title.text = viewModel.bookMetadata.title
		viewHolder.globalSourceSearch.setOnClickListener {
			GlobalSourceSearchActivity.IntentData(
				this,
				input = viewModel.bookMetadata.title
			).let(this@DatabaseBookInfoActivity::startActivity)
		}
		
		viewModel.bookDataLiveData.observe(this) { res ->
			val data = if (res is Response.Success) res.data else return@observe
			
			viewModel.relatedBooks.clear()
			viewModel.relatedBooks.addAll(data.relatedBooks)
			viewModel.similarRecommended.clear()
			viewModel.similarRecommended.addAll(data.similarRecommended)
			
			if (data.genres.isNotEmpty())
			{
				viewHolder.genres.text = data.genres.joinToString(" · ")
				viewHolder.genres.fadeIn(700)
				viewHolder.genres.setOnClickListener {
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
				viewHolder.genres.visibility = View.GONE
			
			Glide.with(this)
				.load(data.coverImageUrl)
				.error(R.drawable.ic_baseline_error_outline_24)
				.transform(RoundedCorners(32))
				.transition(DrawableTransitionOptions.withCrossFade())
				.into(viewHolder.coverImage)
			
			fun Float.spToPx() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics).toInt()
			
			viewHolder.linearLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
			var imgFullWidth = false
			viewHolder.coverImage.setOnClickListener {
				imgFullWidth = !imgFullWidth
				viewHolder.coverImage.updateLayoutParams {
					width = if (imgFullWidth) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
					height = if (imgFullWidth) ViewGroup.LayoutParams.WRAP_CONTENT else 220f.spToPx()
				}
			}
			
			if (data.authors.isNotEmpty())
			{
				val author = data.authors.first()
				viewHolder.authors.text = author.name
				viewHolder.authors.fadeIn(700)
				if (author.url != null)
					viewHolder.authors.setOnClickListener {
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
				viewHolder.authors.visibility = View.GONE
			
			viewHolder.description.text = data.description
			viewHolder.alternativeTitles.text = data.alternativeTitles.joinToString("\n\n")
			viewHolder.tags.text = data.tags.joinToString(" · ").ifEmpty { "No tags" }
			viewHolder.bookType.text = data.bookType
			
			viewHolder.description.fadeIn(700)
			viewHolder.alternativeTitles.fadeIn(700)
			viewHolder.tags.fadeIn(700)
			viewHolder.bookType.fadeIn(700)
			
			viewModel.relatedBooks.isEmpty().let { empty ->
				viewHolder.relatedBooks.visibility = if (empty) View.GONE else View.VISIBLE
				viewHolder.relatedBooksNoEntries.visibility = if (empty) View.VISIBLE else View.GONE
			}
			
			viewModel.similarRecommended.isEmpty().let { empty ->
				viewHolder.similarRecommended.visibility = if (empty) View.GONE else View.VISIBLE
				viewHolder.similarRecommendedNoEntries.visibility = if (empty) View.VISIBLE else View.GONE
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
	private val list: ArrayList<BookMetadata>,
	private val databaseUrlBase: String
) : RecyclerView.Adapter<BookArrayAdapter.ViewBinder>()
{
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewBinder(BookListItemBinding.inflate(parent.inflater, parent, false))
	
	override fun getItemCount() = this@BookArrayAdapter.list.size
	
	override fun onBindViewHolder(binder: ViewBinder, position: Int)
	{
		val itemData = this.list[position]
		val itemHolder = binder.viewHolder
		itemHolder.title.text = itemData.title
		itemHolder.title.setOnClickListener {
			DatabaseBookInfoActivity.IntentData(
				context,
				databaseUrlBase = databaseUrlBase,
				bookMetadata = itemData
			).let(context::startActivity)
		}
	}
	
	inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
