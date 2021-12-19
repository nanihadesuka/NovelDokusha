package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentLibraryBinding
import my.noveldokusha.services.EpubImportService
import my.noveldokusha.ui.BaseFragment

@AndroidEntryPoint
class LibraryFragment : BaseFragment()
{
    private val viewModel by viewModels<LibraryViewModel>()
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
        R.id.library_filter_actions -> openLibraryFilterActions().let { true }
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
                EpubImportService.start(requireContext(), uri)
            }
        }
    }

    fun openLibraryFilterActions()
    {
        if (null != parentFragmentManager.findFragmentByTag(LibraryFragmentBottomSheetDialog.tag))
            return

        LibraryFragmentBottomSheetDialog()
            .show(parentFragmentManager, LibraryFragmentBottomSheetDialog.tag)
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