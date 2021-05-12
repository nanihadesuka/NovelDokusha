package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityMainFragmentFinderBinding
import my.noveldokusha.databinding.ActivityMainFragmentFinderListviewItemHeaderBinding
import my.noveldokusha.databinding.BookListItemBinding
import my.noveldokusha.ui.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.sourceCatalog.SourceCatalogActivity

class FinderFragment : Fragment()
{
	private val viewModel by viewModels<FinderModel>()
	private lateinit var viewHolder: ActivityMainFragmentFinderBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val listView by lazy { ListAdapter(viewModel.sourcesList) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentFinderBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.listView.itemAnimator = DefaultItemAnimator()
		
		return viewHolder.root
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		
		setHasOptionsMenu(true)
	}
	
	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
	{
		inflater.inflate(R.menu.finder_fragment_menu__appbar, menu)
		val searchViewItem = menu.findItem(R.id.action_search)
		val searchView = searchViewItem.actionView as SearchView
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
		{
			override fun onQueryTextSubmit(query: String?): Boolean
			{
				query?.let {
					GlobalSourceSearchActivity
						.IntentData(requireContext(), it)
						.let(this@FinderFragment::startActivity)
				}
				return true
			}
			
			override fun onQueryTextChange(newText: String?): Boolean = true
		})
		super.onCreateOptionsMenu(menu, inflater)
	}
	
	inner class ListAdapter(private val list: ArrayList<Item>) : RecyclerView.Adapter<ItemViewHolder>()
	{
		override fun getItemViewType(position: Int) = list[position].id.ordinal
		
		override fun getItemCount() = list.size
		
		override fun onBindViewHolder(holder: ItemViewHolder, position: Int): Unit = when (val itemData = this.list[position])
		{
			is Item.Source -> with(holder as ItemViewHolder.Source)
			{
				viewHolder.title.text = itemData.name
				viewHolder.title.setOnClickListener {
					SourceCatalogActivity
						.IntentData(requireContext(), sourceBaseUrl = itemData.baseUrl)
						.let(requireContext()::startActivity)
				}
			}
			is Item.Database -> with(holder as ItemViewHolder.Database)
			{
				viewHolder.title.text = itemData.name
				viewHolder.title.setOnClickListener {
					DatabaseSearchActivity
						.IntentData(requireContext(), databaseBaseUrl = itemData.baseUrl)
						.let(this@FinderFragment::startActivity)
				}
			}
			is Item.Header -> with(holder as ItemViewHolder.Header)
			{
				viewHolder.title.text = itemData.text
			}
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (ItemID.getByOrdinal(viewType))
		{
			ItemID.Source -> ItemViewHolder.Source(BookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
			ItemID.Database -> ItemViewHolder.Database(BookListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
			ItemID.Header -> ItemViewHolder.Header(ActivityMainFragmentFinderListviewItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
		}
	}
	
	sealed class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view)
	{
		class Source(val viewHolder: BookListItemBinding) : ItemViewHolder(viewHolder.root)
		class Database(val viewHolder: BookListItemBinding) : ItemViewHolder(viewHolder.root)
		class Header(val viewHolder: ActivityMainFragmentFinderListviewItemHeaderBinding) : ItemViewHolder(viewHolder.root)
	}
	
	enum class ItemID
	{
		Source, Database, Header;
		
		companion object
		{
			private val list = values()
			fun getByOrdinal(ordinal: Int) = list.firstOrNull { it.ordinal == ordinal }!!
		}
	}
	
	sealed class Item(val id: ItemID)
	{
		data class Source(val name: String, val baseUrl: String) : Item(ItemID.Source)
		data class Database(val name: String, val baseUrl: String) : Item(ItemID.Database)
		data class Header(val text: String) : Item(ItemID.Header)
	}
}
