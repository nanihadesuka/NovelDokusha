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
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.uiUtils.addBottomMargin
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
		val relatedBooks by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.relatedBooks, viewModel.database.baseUrl) }
		val similarRecommended by lazy { BookArrayAdapter(this@DatabaseBookInfoActivity, viewModel.similarRecommended, viewModel.database.baseUrl) }
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
		
		viewModel.bookDataLiveData.observe(this) {
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
	private val list: ArrayList<bookstore.BookMetadata>,
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
			val intent = DatabaseBookInfoActivity.Extras(databaseUrlBase = databaseUrlBase, bookMetadata = itemData)
				.intent(context)
			context.startActivity(intent)
		}
		
		binder.addBottomMargin { position == list.lastIndex }
	}
	
	inner class ViewBinder(val viewHolder: BookListItemBinding) : RecyclerView.ViewHolder(viewHolder.root)
}
