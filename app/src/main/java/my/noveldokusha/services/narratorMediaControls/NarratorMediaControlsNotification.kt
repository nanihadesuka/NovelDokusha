package my.noveldokusha.services.narratorMediaControls

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
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

class NarratorMediaControlsNotification(
    private val notificationsCenter: NotificationsCenter,
    private val readerManager: ReaderManager,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("NarratorNotificationService")
    )
) {

    private var mediaSession: MediaSessionCompat? = null

    fun handleCommand(intent: Intent?) {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
    }

    fun createNotificationMediaControls(context: Context): Notification? {

        val readerSession = readerManager.session ?: return null

        val mbrIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_PLAY_PAUSE
        )
        val mediaSession = MediaSessionCompat(
            context,
            "NovelDokusha_narratorMediaControls",
            ComponentName(context, MediaButtonReceiver::class.java),
            mbrIntent
        ).also {
            it.setCallback(NarratorMediaControlsCallback(readerSession.readerTextToSpeech))
            mediaSession = it
        }

        val cancelButton = MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_STOP
        )

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowCancelButton(true)
            .setShowActionsInCompactView(0, 2, 4)
            .setCancelButtonIntent(cancelButton)
            .setMediaSession(mediaSession.sessionToken)

        val intentStack = TaskStackBuilder.create(context).also {
            it.addParentStack(MainActivity::class.java)
            it.addNextIntent(
                ChaptersActivity.IntentData(
                    context,
                    BookMetadata(
                        url = readerSession.bookUrl,
                        title = readerSession.bookTitle ?: ""
                    )
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

        val actionIntentPrevious = NotificationCompat.Action(
            R.drawable.ic_media_control_previous,
            context.getString(R.string.media_control_previous),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        val actionIntentRewind = NotificationCompat.Action(
            R.drawable.ic_media_control_rewind,
            context.getString(R.string.media_control_rewind),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context, PlaybackStateCompat.ACTION_REWIND
            )
        )
        val actionIntentPause = NotificationCompat.Action(
            R.drawable.ic_media_control_pause,
            context.getString(R.string.media_control_play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context, PlaybackStateCompat.ACTION_PAUSE
            )
        )
        val actionIntentPlay = NotificationCompat.Action(
            R.drawable.ic_media_control_play,
            context.getString(R.string.media_control_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context, PlaybackStateCompat.ACTION_PLAY
            )
        )
        val actionIntentFastForward = NotificationCompat.Action(
            R.drawable.ic_media_control_fast_forward,
            context.getString(R.string.media_control_fast_forward),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context, PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        )
        val actionIntentNext = NotificationCompat.Action(
            R.drawable.ic_media_control_next,
            context.getString(R.string.media_control_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )

        fun NotificationCompat.Builder.defineActions(
            isPlaying: Boolean
        ) {
            clearActions()
            addAction(actionIntentPrevious)
            addAction(actionIntentRewind)
            if (isPlaying) {
                addAction(actionIntentPause)
            } else {
                addAction(actionIntentPlay)
            }
            addAction(actionIntentFastForward)
            addAction(actionIntentNext)
        }

        val notificationBuilder =
            notificationsCenter.showNotification(
                NarratorMediaControlsService.channel_id,
                importance = NotificationManager.IMPORTANCE_LOW
            ) {
                title = ""
                text = ""
                defineActions(isPlaying = readerSession.readerTextToSpeech.settings.isPlaying.value)
                setOngoing(true)
                setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                priority = NotificationCompat.PRIORITY_HIGH
                setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo))
                setStyle(mediaStyle)
                setDeleteIntent(cancelButton)
                color = Color.CYAN
                setContentIntent(
                    intentStack.getPendingIntentCompat(
                        requestCode = 0,
                        flags = PendingIntent.FLAG_UPDATE_CURRENT,
                        isMutable = true
                    )
                )
            }

        // Update reader speaking state
        scope.launch {
            snapshotFlow { readerSession.readerTextToSpeech.settings.isPlaying.value }
                .collectLatest { isPlaying ->
                    notificationsCenter.modifyNotification(
                        notificationBuilder,
                        NarratorMediaControlsService.channel_id
                    ) {
                        defineActions(isPlaying = isPlaying)
                    }
                }
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
        mediaSession?.release()
        mediaSession = null
        scope.cancel()
    }
}