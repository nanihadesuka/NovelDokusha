package my.noveldokusha

import android.app.Application

class App : Application()
{
	override fun onCreate()
	{
		bookstore.setContext(this)
		super.onCreate()
	}
}