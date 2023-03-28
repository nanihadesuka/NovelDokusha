package my.noveldokusha.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.R
import kotlin.reflect.KProperty

class NotificationsCenter(
    @ApplicationContext private val context: Context
) {

    private val manager = NotificationManagerCompat.from(context)

    @SuppressLint("MissingPermission")
    fun showNotification(
        channel_id: String,
        channel_name: String = channel_id,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        builder: NotificationCompat.Builder.() -> Unit = {}
    ) = run {
        buildNotification(context, channel_id, channel_name, importance, builder).apply {
            NotificationManagerCompat
                .from(context)
                .notify(channel_id.hashCode(), build())
        }
    }

    fun close(channel_id: String) = manager.cancel(channel_id.hashCode())

    private fun buildNotification(
        context: Context,
        channel_id: String,
        channel_name: String = channel_id,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        builder: NotificationCompat.Builder.() -> Unit,
    ) = run {
        NotificationCompat.Builder(context, channel_id).apply {
            setSmallIcon(R.mipmap.ic_logo)
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo)
            priority = NotificationCompat.PRIORITY_DEFAULT
            builder(this)

            val channel = NotificationChannel(channel_id, channel_name, importance)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun modifyNotification(
        builder: NotificationCompat.Builder,
        channel_id: String,
        modifierBlock: NotificationCompat.Builder.() -> Unit
    ) {
        modifierBlock(builder)
        NotificationManagerCompat
            .from(context)
            .notify(channel_id.hashCode(), builder.build())
    }
}

fun NotificationCompat.Builder.removeProgressBar() = setProgress(0, 0, false)

private class NotificationBuilderObserver<T>(
    initialValue: T,
    val observer: NotificationCompat.Builder.(KProperty<*>, T, T) -> Unit
) {
    private var value = initialValue
    operator fun getValue(thisRef: NotificationCompat.Builder, property: KProperty<*>) = value
    operator fun setValue(thisRef: NotificationCompat.Builder, property: KProperty<*>, value: T) {
        val oldValue = this.value
        this.value = value
        observer(thisRef, property, oldValue, value)
    }
}

var NotificationCompat.Builder.title: String by NotificationBuilderObserver("") { _, _, n ->
    setContentTitle(n)
}
var NotificationCompat.Builder.text: String by NotificationBuilderObserver("") { _, _, n ->
    setContentText(n)
}

