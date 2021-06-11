package my.noveldokusha

import android.app.Application
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
	
	companion object
	{
		private lateinit var _instance: App
		val instance get() = _instance
		val cacheDir: File get() = _instance.cacheDir
	}
}