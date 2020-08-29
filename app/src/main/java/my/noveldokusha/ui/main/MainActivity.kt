package my.noveldokusha.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityMainBinding

open class MainActivity : AppCompatActivity()
{
	private val viewHolder by lazy { ActivityMainBinding.inflate(layoutInflater) }
	
	private val bottomNavigationController by lazy { findNavController(R.id.navHostFragment) }
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		
		viewHolder.bottomNavigationView.setupWithNavController(bottomNavigationController)
		viewHolder.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
			if (viewHolder.bottomNavigationView.selectedItemId == item.itemId)
				return@setOnNavigationItemSelectedListener false
			NavigationUI.onNavDestinationSelected(item, bottomNavigationController)
		}
	}
}