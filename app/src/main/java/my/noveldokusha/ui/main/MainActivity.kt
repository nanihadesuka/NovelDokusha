package my.noveldokusha.ui.main

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityMainBinding
import my.noveldokusha.ui.BaseActivity

open class MainActivity : BaseActivity()
{
	private val viewBind by lazy { ActivityMainBinding.inflate(layoutInflater) }
	
	private val bottomNavigationController by lazy { findNavController(R.id.navHostFragment) }
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		
		viewBind.bottomNavigationView.setupWithNavController(bottomNavigationController)
		
		viewBind.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
			if (viewBind.bottomNavigationView.selectedItemId == item.itemId)
				return@setOnNavigationItemSelectedListener false
			NavigationUI.onNavDestinationSelected(item, bottomNavigationController)
		}
	}
}