package my.noveldokusha.ui.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityDatabaseBookInfoBinding
import my.noveldokusha.databinding.ActivityDatabaseSearchResultsListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import java.util.*

class DatabaseBookInfoActivity : BaseActivity()
{
	class Extras(val databaseUrlBase: String, val bookMetadata: bookstore.BookMetadata)
	{
		fun intent(ctx: Context) = Intent(ctx, DatabaseBookInfoActivity::class.java).also {
			it.putExtra(::databaseUrlBase.name, databaseUrlBase)
			it.putExtra(bookMetadata::title.name, bookMetadata.title)
			it.putExtra(bookMetadata::url.name, bookMetadata.url)
		}
	}
	
	private val extras = object
	{
		fun databaseUrlBase(): String = intent.extras!!.getString(Extras::databaseUrlBase.name)!!
		fun bookMetadata(): bookstore.BookMetadata = bookstore.BookMetadata(
			title = intent.extras!!.getString(bookstore.BookMetadata::title.name)!!,
			url = intent.extras!!.getString(bookstore.BookMetadata::url.name)!!
		)
	}
	
	private val viewModel by viewModels<DatabaseBookInfoModel>()
	private val viewHolder by lazy { ActivityDatabaseBookInfoBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{
		val relatedBooks by lazy { BookArrayAdapter(viewModel.relatedBooks) }
		val similarRecommended by lazy { BookArrayAdapter(viewModel.similarRecommended) }
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization(database = scrubber.getCompatibleDatabase(extras.databaseUrlBase())!!, bookMetadata = extras.bookMetadata())
		
		viewHolder.relatedBooks.adapter = viewAdapter.relatedBooks
		viewHolder.similarRecommended.adapter = viewAdapter.similarRecommended
		viewHolder.title.text = viewModel.bookMetadata.title
		viewHolder.globalSourceSearch.setOnClickListener {
			val intent = GlobalSourceSearchActivity.Extras(viewModel.bookMetadata.title).intent(this)
			startActivity(intent)
		}
		
		viewModel.bookDataUpdated.observe(this) {
			viewHolder.description.text = viewModel.bookData.description
			viewHolder.alternativeTitles.text = viewModel.bookData.alternativeTitles.joinToString("\n\n")
			viewHolder.authors.text = viewModel.bookData.authors.joinToString("\n") { author -> author.name }
			viewHolder.tags.text = viewModel.bookData.tags.joinToString(" · ")
			viewHolder.genres.text = viewModel.bookData.genres.joinToString(" · ")
			viewHolder.bookType.text = viewModel.bookData.bookType
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
	
	private inner class BookArrayAdapter(private val list: ArrayList<bookstore.BookMetadata>) : RecyclerView.Adapter<BookArrayAdapter.ViewBinder>()
	{
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			ViewBinder(ActivityDatabaseSearchResultsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		
		override fun getItemCount() = this@BookArrayAdapter.list.size
		
		override fun onBindViewHolder(binder: ViewBinder, position: Int)
		{
			val itemData = this.list[position]
			val itemHolder = binder.viewHolder
			itemHolder.title.text = itemData.title
			itemHolder.title.setOnClickListener {
				val intent = Extras(databaseUrlBase = viewModel.database.baseUrl, bookMetadata = itemData)
					.intent(this@DatabaseBookInfoActivity)
				startActivity(intent)
			}
		}
		
		inner class ViewBinder(val viewHolder: ActivityDatabaseSearchResultsListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
	}
}