package my.noveldokusha.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.*

open class BaseActivity : AppCompatActivity()
{
	private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == sharedPreferences::THEME_ID.name || key == sharedPreferences::THEME_FOLLOW_SYSTEM.name)
			recreate()
	}
	
	private fun getAppTheme(): Int
	{
		if (!sharedPreferences.THEME_FOLLOW_SYSTEM)
			return sharedPreferences.THEME_ID
		
		val isThemeLight = globalThemeList.light.contains(sharedPreferences.THEME_ID)
		val isSystemThemeLight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
		
		if (isSystemThemeLight && !isThemeLight) return R.style.AppTheme_Light
		if (!isSystemThemeLight && isThemeLight) return R.style.AppTheme_BaseDark_Dark
		return sharedPreferences.THEME_ID
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(getAppTheme())
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
		super.onCreate(savedInstanceState)
	}
	
	val sharedPreferences: SharedPreferences by lazy { appSharedPreferences() }
}