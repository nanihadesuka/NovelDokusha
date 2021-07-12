package my.noveldokusha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import kotlin.reflect.KProperty

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
		val folderBooks: File get() = File(App.instance.getExternalFilesDir(null) ?: App.instance.filesDir, "books")
		fun getDatabasePath(databaseName: String): File = instance.applicationContext.getDatabasePath(databaseName)
		
		fun showNotification(channel_id: String, channel_name: String, builder: NotificationCompat.Builder.() -> Unit) = run {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT).apply {
					description = "channel_description"
				}
				val notificationManager = instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
				notificationManager.createNotificationChannel(channel)
			}
			
			NotificationCompat.Builder(instance, channel_id).apply {
				setSmallIcon(R.mipmap.ic_logo)
				setLargeIcon(R.mipmap.ic_logo)
				priority = NotificationCompat.PRIORITY_DEFAULT
				builder(this)
				NotificationManagerCompat
					.from(instance.applicationContext)
					.notify(channel_id.hashCode(), build())
			}
		}
	}
}

fun NotificationCompat.Builder.setLargeIcon(id: Int) = setLargeIcon(BitmapFactory.decodeResource(App.instance.resources, id))

private class NotificationBuilderObserver<T>(initialValue: T, val observer: NotificationCompat.Builder.(KProperty<*>, T, T) -> Unit)
{
	private var value = initialValue
	operator fun getValue(thisRef: NotificationCompat.Builder, property: KProperty<*>) = value
	operator fun setValue(thisRef: NotificationCompat.Builder, property: KProperty<*>, value: T)
	{
		val oldValue = this.value
		this.value = value
		observer(thisRef, property, oldValue, value)
	}
}

var NotificationCompat.Builder.title: String by NotificationBuilderObserver("") { _, _, n -> setContentTitle(n) }
var NotificationCompat.Builder.text: String by NotificationBuilderObserver("") { _, _, n -> setContentText(n) }