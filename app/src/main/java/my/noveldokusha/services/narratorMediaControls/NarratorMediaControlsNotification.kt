package my.noveldokusha.services.narratorMediaControls

import android.app.Notification
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.main.MainActivity
import my.noveldokusha.ui.screens.reader.ReaderActivity
import my.noveldokusha.ui.screens.reader.chapterReadPercentage
import my.noveldokusha.ui.screens.reader.manager.ReaderManager
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.getPendingIntentCompat
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title

@androidx.annotation.OptIn(UnstableApi::class)
class NarratorMediaControlsNotification(
    private val notificationsCenter: NotificationsCenter,
    private val readerManager: ReaderManager,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("NarratorNotificationService")
    )
) {

    private var mediaSession: MediaSession? = null

    fun createNotificationMediaControls(context: Context): Notification? {

        val readerSession = readerManager.session ?: return null

        val mediaSession = MediaSession
            .Builder(context, createNarratorMediaControlsPlayer(readerSession))
            .build()
            .also { mediaSession = it }

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)

        val intentStack = TaskStackBuilder.create(context).also {
            it.addParentStack(MainActivity::class.java)
            it.addNextIntent(
                ChaptersActivity.IntentData(
                    context,
                    BookMetadata(url = readerSession.bookUrl, title = readerSession.bookTitle ?: "")
                )
            )
            it.addNextIntent(
                ReaderActivity.IntentData(
                    ctx = context,
                    bookUrl = readerSession.bookUrl,
                    chapterUrl = readerSession.currentChapter.chapterUrl,
                    scrollToSpeakingItem = true
                )
            )
        }

        val notificationBuilder =
            notificationsCenter.showNotification(NarratorMediaControlsService.channel_id) {
                title = ""
                text = ""
                setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo))
                setStyle(mediaStyle)
                setSilent(true)
                setContentIntent(
                    intentStack.getPendingIntentCompat(
                        requestCode = 0,
                        flags = PendingIntent.FLAG_UPDATE_CURRENT,
                        isMutable = true
                    )
                )
            }

        // Update chapter notification title
        scope.launch {
            snapshotFlow { readerSession.readerTextToSpeech.currentTextPlaying.value }
                .mapNotNull { readerSession.readerChaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
                .map { it.chapter.title }
                .distinctUntilChanged()
                .collectLatest { chapterTitle ->
                    notificationsCenter.modifyNotification(
                        notificationBuilder,
                        NarratorMediaControlsService.channel_id
                    ) {
                        title = chapterTitle
                    }
                }
        }

        // Update chapter notification intent
        scope.launch {
            snapshotFlow { readerSession.readerTextToSpeech.currentTextPlaying.value }
                .mapNotNull { readerSession.readerChaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
                .map { it.chapter.url }
                .distinctUntilChanged()
                .mapNotNull { chapterUrl ->
                    intentStack.intents
                        .filterIsInstance<ReaderActivity.IntentData>()
                        .firstOrNull()
                        ?.let { chapterUrl to it }
                }
                .collectLatest { (chapterUrl, readerIntent) ->
                    readerIntent.chapterUrl = chapterUrl
                    notificationsCenter.modifyNotification(
                        notificationBuilder,
                        NarratorMediaControlsService.channel_id
                    ) {
                        setContentIntent(
                            intentStack.getPendingIntentCompat(
                                requestCode = 0,
                                flags = PendingIntent.FLAG_UPDATE_CURRENT,
                                isMutable = true
                            )
                        )
                    }
                }
        }


        // Update chapter progress
        scope.launch {
            snapshotFlow { readerSession.speakerStats.value }
                .filterNotNull()
                .collectLatest {
                    val chapterPos = context.getString(
                        R.string.chapter_x_over_n,
                        it.chapterIndex + 1,
                        it.chapterCount
                    )
                    val progress = context.getString(
                        R.string.progress_x_percentage,
                        it.chapterReadPercentage()
                    )
                    notificationsCenter.modifyNotification(
                        notificationBuilder,
                        NarratorMediaControlsService.channel_id
                    ) {
                        text = "$chapterPos  $progress"
                    }
                }
        }

        return notificationBuilder.build()
    }

    fun close() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        scope.cancel()
    }
}