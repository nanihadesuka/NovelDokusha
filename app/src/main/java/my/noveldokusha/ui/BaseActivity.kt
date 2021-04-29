package my.noveldokusha.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.noveldokusha.R

open class BaseActivity : AppCompatActivity()
{
	private val preferenceThemeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == "id")
			recreate()
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(preferencesGetThemeId())
		preferencesGetTheme().registerOnSharedPreferenceChangeListener(preferenceThemeListener)
		super.onCreate(savedInstanceState)
	}
	
	fun preferencesGetTheme(): SharedPreferences = getSharedPreferences("GLOBAL_THEME", MODE_PRIVATE)
	fun preferencesGetThemeId() = preferencesGetTheme().getInt("id", R.style.AppTheme_Light)
	fun preferencesSetThemeId(id: Int) = preferencesGetTheme().edit().putInt("id", id).apply()
	
	companion object
	{
		val globalThemeList = mapOf(
			"Light" to R.style.AppTheme_Light,
			"Dark" to R.style.AppTheme_Dark,
			"Grey" to R.style.AppTheme_Grey,
			"Black" to R.style.AppTheme_Black
		)
	}
}