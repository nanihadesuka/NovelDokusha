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
        channelId: String,
        channelName: String = channelId,
        notificationId: String = channelId,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        builder: NotificationCompat.Builder.() -> Unit = {}
    ) = run {
        buildNotification(context, channelId, channelName, importance, builder).apply {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId.hashCode(), build())
        }
    }

    fun close(channelId: String) = manager.cancel(channelId.hashCode())

    private fun buildNotification(
        context: Context,
        channelId: String,
        channelName: String = channelId,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        builder: NotificationCompat.Builder.() -> Unit,
    ) = run {
        NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(R.mipmap.ic_logo)
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo)
            priority = NotificationCompat.PRIORITY_DEFAULT
            builder(this)

            val channel = NotificationChannel(channelId, channelName, importance)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun modifyNotification(
        builder: NotificationCompat.Builder,
        channelId: String,
        modifierBlock: NotificationCompat.Builder.() -> Unit
    ) {
        modifierBlock(builder)
        NotificationManagerCompat
            .from(context)
            .notify(channelId.hashCode(), builder.build())
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

