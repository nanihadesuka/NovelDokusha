package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import my.noveldokusha.databinding.ActivityMainFragmentLibraryBinding

class LibraryFragment : Fragment()
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