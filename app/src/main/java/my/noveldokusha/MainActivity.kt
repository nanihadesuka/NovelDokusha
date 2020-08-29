package my.noveldokusha

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity()
{
	private val viewModel: MainActivityModel by lazy {
		ViewModelProvider(this).get(MainActivityModel::class.java)
	}

	private lateinit var job: Job
	override fun onCreate(savedInstanceState: Bundle?)
	{
		Log.e("MainActivity", "onCreate")
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)

		val navController = findNavController(R.id.nav_host_fragment)
		val appBarConfiguration = AppBarConfiguration(
			setOf(R.id.navigation_home, R.id.navigation_dashboard)
		)
		setupActionBarWithNavController(navController, appBarConfiguration)
		navView.setupWithNavController(navController)
	}
}