package my.noveldokusha.ui.main

import android.content.Context
import android.content.Intent
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
	
	class IntentData : Intent
	{
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context) : super(ctx, LibraryFragment::class.java)
	}
	
	private lateinit var viewHolder: ActivityMainFragmentLibraryBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
		val viewPage by lazy { LibraryViewPageAdapter(this@LibraryFragment.requireActivity()) }
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentLibraryBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		viewHolder.viewPager.offscreenPageLimit = 3
		viewHolder.viewPager.adapter = viewAdapter.viewPage
		TabLayoutMediator(viewHolder.viewPagerTabs, viewHolder.viewPager) { tab, position ->
			tab.text = when (position)
			{
				0 -> "Default"
				else -> "Completed"
			}
		}.attach()
		return viewHolder.root
	}
}

private class LibraryViewPageAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity)
{
	override fun getItemCount(): Int = 2
	
	override fun createFragment(position: Int): Fragment = when (position)
	{
		0 -> LibraryPageFragment.createInstance(showCompleted = false)
		else -> LibraryPageFragment.createInstance(showCompleted = true)
	}
}