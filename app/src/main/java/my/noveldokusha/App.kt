package my.noveldokusha

import android.app.Application

class App : Application()
{
	override fun onCreate()
	{
		_instance = this
		super.onCreate()
	}
	
	companion object
	{
		private lateinit var _instance: App
		val instance get() = _instance
	}
}