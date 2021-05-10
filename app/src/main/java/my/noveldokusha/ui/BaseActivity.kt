package my.noveldokusha.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.AppPreferences
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.getAppThemeId

open class BaseActivity : AppCompatActivity()
{
	private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == AppPreferences.THEME_ID.name)
			recreate()
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(sharedPreferences.getAppThemeId())
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
		super.onCreate(savedInstanceState)
	}
	
	val sharedPreferences: SharedPreferences by lazy { appSharedPreferences() }
}