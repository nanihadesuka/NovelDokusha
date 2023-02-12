package my.noveldokusha.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import my.noveldokusha.R
import my.noveldokusha.ui.screens.reader.ReaderManager
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.isServiceRunning
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class NarratorMediaPlayerService : Service(), CoroutineScope {

    companion object {
        const val channel_id = "ReaderNarrator"

        fun start(ctx: Context) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(
                    ctx,
                    Intent(ctx, NarratorMediaPlayerService::class.java)
                )
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupDataService::class.java)
    }

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    @Inject
    lateinit var readerManager: ReaderManager

    private lateinit var notificationBuilder: NotificationCompat.Builder

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("NarratorService")

    override fun onCreate() {
        super.onCreate()

        val readerSession = readerManager.session ?: return

        val mediaSession = MediaSession
            .Builder(this, createNarratorSessionPlayer())
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    Timber.d("playerCommand: $playerCommand")
                    return when (playerCommand) {
                        Player.COMMAND_PLAY_PAUSE -> {
                            val isPlaying = readerSession.readerSpeaker.settings.isPlaying.value
                            readerSession.readerSpeaker.settings.setPlaying(!isPlaying)
                            SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_NEXT -> {
                            readerSession.readerSpeaker.settings.playNextChapter()
                            SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS -> {
                            readerSession.readerSpeaker.settings.playPreviousChapter()
                            SessionResult.RESULT_SUCCESS
                        }
                        else -> super.onPlayerCommandRequest(session, controller, playerCommand)
                    }
                }
            })
            .build()

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)

        notificationBuilder = notificationsCenter.showNotification(channel_id) {
            title = "Reader mode"
            text = ""
            setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_logo))
            setStyle(mediaStyle)
            setSilent(true)
        }

        // TODO FORCE CLOSE WHEN APP CLOSE ?

        launch {
            snapshotFlow { readerSession.readerSpeaker.currentTextPlaying.value }
                .mapNotNull { readerSession.orderedChapters.getOrNull(it.item.chapterIndex) }
                .collectLatest {
                    notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                        title = it.title
                    }
                }
        }

        launch {
            snapshotFlow { readerSession.speakerChapterPercentageProgress.value }
                .collectLatest {
                    notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                        text = getString(R.string.progress_x_percentage, it)
                    }
                }
        }

        startForeground(channel_id.hashCode(), notificationBuilder.build())
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null
}