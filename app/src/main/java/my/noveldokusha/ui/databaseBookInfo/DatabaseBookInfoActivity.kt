package my.noveldokusha.ui.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.BookMetadata
import my.noveldokusha.Response
import my.noveldokusha.databinding.ActivityDatabaseBookInfoBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.uiUtils.Extra_String
import java.util.*

class DatabaseBookInfoActivity : BaseActivity()
{
	class IntentData : Intent
	{
		val bookMetadata get() = BookMetadata(title = bookTitle, url = bookUrl)
		var databaseUrlBase by Extra_String()
		private var bookUrl by Extra_String()
		private var bookTitle by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, databaseUrlBase: String, bookMetadata: BookMetadata) :super(ctx, DatabaseBookInfoActivity::class.java)
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
			val it = if (res is Response.Success) res.data else return@observe
			
			viewModel.relatedBooks.clear()
			viewModel.relatedBooks.addAll(it.relatedBooks)
			viewModel.similarRecommended.clear()
			viewModel.similarRecommended.addAll(it.similarRecommended)
			
			viewHolder.description.text = it.description
			viewHolder.alternativeTitles.text = it.alternativeTitles.joinToString("\n\n")
			viewHolder.authors.text = it.authors.joinToString("\n") { author -> author.name }
			viewHolder.tags.text = it.tags.joinToString(" · ")
			viewHolder.genres.text = it.genres.joinToString(" · ")
			viewHolder.bookType.text = it.bookType
			
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
		ViewBinder(BookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
	
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
