package my.noveldokusha.uiUtils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import my.noveldokusha.App
import my.noveldokusha.R
import kotlin.reflect.KProperty

fun buildNotification(context: Context, channel_id: String, channel_name: String = channel_id, builder: NotificationCompat.Builder.() -> Unit) = run {
    NotificationCompat.Builder(context, channel_id).apply {
        setSmallIcon(R.mipmap.ic_logo)
        setLargeIcon(R.mipmap.ic_logo)
        priority = NotificationCompat.PRIORITY_DEFAULT
        builder(this)

        val channel = NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_DEFAULT)
        manager(context).createNotificationChannel(channel)
    }
}

fun showNotification(context: Context, channel_id: String, channel_name: String = channel_id, builder: NotificationCompat.Builder.() -> Unit) = run {
    buildNotification(context, channel_id, channel_name, builder).apply {
        NotificationManagerCompat
            .from(context)
            .notify(channel_id.hashCode(), build())
    }
}

var NotificationCompat.Builder.title: String by NotificationBuilderObserver("") { _, _, n -> setContentTitle(n) }
var NotificationCompat.Builder.text: String by NotificationBuilderObserver("") { _, _, n -> setContentText(n) }

fun NotificationCompat.Builder.manager(context: Context) = NotificationManagerCompat.from(context)

fun NotificationCompat.Builder.removeProgressBar() = setProgress(0, 0, false)
fun NotificationCompat.Builder.setLargeIcon(id: Int) = setLargeIcon(BitmapFactory.decodeResource(App.instance.resources, id))
fun NotificationCompat.Builder.close(context: Context, channel_id: String) = manager(context).cancel(channel_id.hashCode())

fun NotificationCompat.Builder.showNotification(channel_id: String, builder: NotificationCompat.Builder.() -> Unit) = run {
    builder(this)
    NotificationManagerCompat
        .from(App.instance.applicationContext)
        .notify(channel_id.hashCode(), build())
}

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