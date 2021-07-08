package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentLibraryBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast

class LibraryFragment : BaseFragment()
{
	private lateinit var viewBind: ActivityMainFragmentLibraryBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val viewPage by lazy { LibraryViewPageAdapter(this@LibraryFragment.requireActivity()) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewBind = ActivityMainFragmentLibraryBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewBind.viewPager.offscreenPageLimit = 3
		viewBind.viewPager.adapter = viewAdapter.viewPage
		TabLayoutMediator(viewBind.viewPagerTabs, viewBind.viewPager) { tab, position ->
			tab.text = when (position)
			{
				0 -> "Default"
				else -> "Completed"
			}
		}.attach()
		return viewBind.root
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}
	
	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
	{
		inflater.inflate(R.menu.library_menu__appbar, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
	{
		R.id.import_epub -> openEpubImporter().let { true }
		else -> super.onOptionsItemSelected(item)
	}
	
	fun openEpubImporter()
	{
		val read = Manifest.permission.READ_EXTERNAL_STORAGE
		permissionRequest(read) {
			val intent = Intent(Intent.ACTION_GET_CONTENT).also {
				it.addCategory(Intent.CATEGORY_OPENABLE)
				it.type = "application/epub+zip"
			}
			
			activityRequest(intent) { resultCode, data ->
				if (resultCode != Activity.RESULT_OK) return@activityRequest
				val uri = data?.data ?: return@activityRequest
				val inputStream = requireActivity().contentResolver.openInputStream(uri)
				if (inputStream == null)
				{
					toast(R.string.failed_get_file.stringRes())
					return@activityRequest
				}
				
				CoroutineScope(Dispatchers.IO).launch {
					try
					{
						val epub = inputStream.use { epubReader(it) }
						importEpubToDatabase(epub)
					}
					catch (e: Exception)
					{
						toast(R.string.failed_to_import_epub.stringRes())
						Log.e("EPUB IMPORT FAILED", e.stackTraceToString())
					}
				}
			}
		}
	}
}

private class LibraryViewPageAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity)
{
	override fun getItemCount(): Int = 2
	
	override fun createFragment(position: Int): Fragment = when (position)
	{
		0 -> LibraryPageFragment(showCompleted = false)
		else -> LibraryPageFragment(showCompleted = true)
	}
}