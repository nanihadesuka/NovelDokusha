package my.noveldokusha.services.libraryUpdate

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.main.MainActivity
import my.noveldokusha.ui.screens.reader.ReaderActivity
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.getPendingIntentCompat
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title
import javax.inject.Inject

class LibraryUpdateNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationsCenter: NotificationsCenter
) {

    private val channelName = context.getString(R.string.notification_channel_name_library_update)
    private val channelId = "Library update"
    val notificationId: Int = channelId.hashCode()

    private val notifyNewChapters = object {
        val channelName = context.getString(R.string.notification_channel_name_new_chapters)
        val channelId = "New chapters"
    }

    private val notifyFailedUpdates = object {
        val channelName = context.getString(R.string.notification_channel_name_failed_updates)
        val channelId = "Failed updates"
        val notificationId: Int = channelId.hashCode()
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    fun createEmptyUpdatingNotification(): Notification {
        notificationBuilder = notificationsCenter.showNotification(
            channelId = channelId,
            channelName = channelName,
            notificationId = notificationId,
            importance = NotificationManager.IMPORTANCE_LOW
        ) {
            setStyle(NotificationCompat.BigTextStyle())
            title = context.getString(R.string.updaing_library)
        }
        return notificationBuilder.build()
    }

    fun updateUpdatingNotification(
        countingUpdated: Int,
        countingTotal: Int,
        books: Set<Book>
    ) {
        notificationsCenter.modifyNotification(notificationBuilder, notificationId) {
            title = context.getString(R.string.updating_library, countingUpdated, countingTotal)
            text = books.joinToString(separator = "\n") { "· " + it.title }
            setProgress(countingTotal, countingUpdated, false)
        }
    }

    fun showNewChaptersNotification(
        book: Book,
        newChapters: List<Chapter>,
        silent: Boolean
    ) {
        val chain = mutableListOf<Intent>().also {
            it.add(Intent(context, MainActivity::class.java))
            it.add(
                ChaptersActivity.IntentData(
                    context, bookMetadata = BookMetadata(
                        coverImageUrl = book.coverImageUrl,
                        description = book.description,
                        title = book.title,
                        url = book.url
                    )
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            newChapters.firstOrNull()?.let { chapter ->
                it.add(
                    ReaderActivity.IntentData(
                        ctx = context,
                        bookUrl = book.url,
                        chapterUrl = chapter.url,
                        scrollToSpeakingItem = false
                    )
                )
            }
        }

        val intentStack = PendingIntent.getActivities(
            context,
            book.url.hashCode(),
            chain.toTypedArray(),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        notificationsCenter.showNotification(
            notificationId = book.url.hashCode(),
            channelId = notifyNewChapters.channelId,
            channelName = "New chapters",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
        {
            title = book.title
            val newText = newChapters.take(3).joinToString("\n") { "· " + it.title }
            text = newText + if (newChapters.size > 3) "\n..." else ""
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
            setGroup(book.url)
            setSmallIcon(R.drawable.new_chapters_icon_notification)
            setContentIntent(intentStack)
            setAutoCancel(true)
            setSilent(silent)
        }

        if (book.coverImageUrl.isBlank()) return

        Glide.with(context)
            .asBitmap()
            .load(book.coverImageUrl)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        notificationsCenter.showNotification(
                            notificationId = book.url.hashCode(),
                            channelId = notifyNewChapters.channelId,
                            channelName = notifyNewChapters.channelName,
                            importance = NotificationManager.IMPORTANCE_DEFAULT
                        ) {
                            title = book.title
                            val newText = newChapters.take(3).joinToString("\n") {
                                "· " + it.title
                            }
                            text = newText + if (newChapters.size > 3) "\n..." else ""
                            setStyle(NotificationCompat.BigTextStyle().bigText(text))
                            setGroup(book.url)
                            setSmallIcon(R.drawable.new_chapters_icon_notification)
                            setLargeIcon(resource)
                            setContentIntent(intentStack)
                            setAutoCancel(true)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                })
    }

    fun showFailedNotification(
        books: Set<Book>,
    ) {
        notificationsCenter.showNotification(
            notificationId = notifyFailedUpdates.notificationId,
            channelId = notifyFailedUpdates.channelId,
            channelName = notifyFailedUpdates.channelName,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ) {
            title = context.getString(R.string.update_error)
            text = books.joinToString("\n") { "· " + it.title }
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
    }

}