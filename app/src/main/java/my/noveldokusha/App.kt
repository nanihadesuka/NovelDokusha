package my.noveldokusha

import android.app.Application
import android.content.SharedPreferences
import java.io.File

class App : Application()
{
	override fun onCreate()
	{
		_instance = this
		cookiesData.load()
		headersData.load()
		super.onCreate()
	}
	
	val preferencesChangeListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
	
	companion object
	{
		private lateinit var _instance: App
		val instance get() = _instance
		val cacheDir: File get() = _instance.cacheDir
	}
}