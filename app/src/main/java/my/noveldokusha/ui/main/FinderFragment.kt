package my.noveldokusha.ui.main

import android.content.Context
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
import my.noveldokusha.uiAdapters.MyListAdapter
import my.noveldokusha.uiUtils.inflater

class FinderFragment : Fragment()
{
	private val viewModel by viewModels<FinderModel>()
	private lateinit var viewHolder: ActivityMainFragmentFinderBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val listView by lazy { ListItemAdapter(requireContext()) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentFinderBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewHolder.listView.adapter = viewAdapter.listView
		viewHolder.listView.itemAnimator = DefaultItemAnimator()
		viewAdapter.listView.list = viewModel.sourcesList
		
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
}

sealed class Item
{
	data class Source(val name: String, val baseUrl: String) : Item()
	data class Database(val name: String, val baseUrl: String) : Item()
	data class Header(val text: String) : Item()
}

private class ListItemAdapter(val ctx: Context) : MyListAdapter<Item, ListItemAdapter.ViewHolder>()
{
	override fun areItemsTheSame(old: Item, new: Item) = false
	override fun areContentsTheSame(old: Item, new: Item) = false
	override fun getItemViewType(position: Int) = when (list[position])
	{
		is Item.Source -> 0
		is Item.Database -> 1
		is Item.Header -> 2
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int): Unit = when (val itemData = this.list[position])
	{
		is Item.Source -> with(holder as ViewHolder.Source)
		{
			viewHolder.title.text = itemData.name
			viewHolder.title.setOnClickListener {
				SourceCatalogActivity
					.IntentData(ctx, sourceBaseUrl = itemData.baseUrl)
					.let(ctx::startActivity)
			}
		}
		is Item.Database -> with(holder as ViewHolder.Database)
		{
			viewHolder.title.text = itemData.name
			viewHolder.title.setOnClickListener {
				DatabaseSearchActivity
					.IntentData(ctx, databaseBaseUrl = itemData.baseUrl)
					.let(ctx::startActivity)
			}
		}
		is Item.Header -> with(holder as ViewHolder.Header)
		{
			viewHolder.title.text = itemData.text
		}
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType)
	{
		0 -> ViewHolder.Source(BookListItemBinding.inflate(parent.inflater, parent, false))
		1 -> ViewHolder.Database(BookListItemBinding.inflate(parent.inflater, parent, false))
		2 -> ViewHolder.Header(ActivityMainFragmentFinderListviewItemHeaderBinding.inflate(parent.inflater, parent, false))
		else -> throw Exception("No view defined for viewType: $viewType")
	}
	
	sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
	{
		class Source(val viewHolder: BookListItemBinding) : ViewHolder(viewHolder.root)
		class Database(val viewHolder: BookListItemBinding) : ViewHolder(viewHolder.root)
		class Header(val viewHolder: ActivityMainFragmentFinderListviewItemHeaderBinding) : ViewHolder(viewHolder.root)
	}
	
}