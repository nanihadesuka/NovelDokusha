package my.noveldokusha.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.THEME_ID
import my.noveldokusha.appSharedPreferences

open class BaseActivity : AppCompatActivity()
{
	private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == sharedPreferences::THEME_ID.name)
			recreate()
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(sharedPreferences.THEME_ID)
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
		super.onCreate(savedInstanceState)
	}
	
	val sharedPreferences: SharedPreferences by lazy { appSharedPreferences() }
}