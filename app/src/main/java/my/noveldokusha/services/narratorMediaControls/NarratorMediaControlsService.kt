package my.noveldokusha.services.narratorMediaControls

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.R
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.services.BackupDataService
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.main.MainActivity
import my.noveldokusha.ui.screens.reader.ReaderActivity
import my.noveldokusha.ui.screens.reader.ReaderManager
import my.noveldokusha.ui.screens.reader.ReaderSession
import my.noveldokusha.ui.screens.reader.chapterReadPercentage
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.isServiceRunning
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class NarratorMediaControlsService : Service(), CoroutineScope {

    companion object {
        const val channel_id = "ReaderNarrator"

        fun start(ctx: Context) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(
                    ctx,
                    Intent(ctx, NarratorMediaControlsService::class.java)
                )
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, NarratorMediaControlsService::class.java))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupDataService::class.java)
    }

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    @Inject
    lateinit var readerManager: ReaderManager

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("NarratorService")

    private var mediaSession: MediaSession? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()

        val readerSession = readerManager.session ?: return

        val notification = createNotificationMediaControls(readerSession)

        startForeground(channel_id.hashCode(), notification)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createNotificationMediaControls(readerSession: ReaderSession): Notification {

        val mediaSession = MediaSession
            .Builder(this, createNarratorMediaControlsPlayer())
            .setCallback(createNarratorMediaControlsCommands(readerSession))
            .build()
            .also { mediaSession = it }

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)

        val intentStack = TaskStackBuilder.create(this).also {
            it.addParentStack(MainActivity::class.java)
            it.addNextIntent(
                ChaptersActivity.IntentData(
                    this@NarratorMediaControlsService,
                    BookMetadata(url = readerSession.bookUrl, title = readerSession.bookUrl)
                )
            )
            it.addNextIntent(
                ReaderActivity.IntentData(
                    ctx = this@NarratorMediaControlsService,
                    bookUrl = readerSession.bookUrl,
                    chapterUrl = readerSession.currentChapter.chapterUrl,
                    scrollToSpeakingItem = true
                )
            )
        }

        val notificationBuilder = notificationsCenter.showNotification(channel_id) {
            title = ""
            text = ""
            setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_logo))
            setStyle(mediaStyle)
            setSilent(true)
            setContentIntent(intentStack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
        }

        // Update chapter notification title
        launch {
            snapshotFlow { readerSession.readerSpeaker.currentTextPlaying.value }
                .mapNotNull { readerSession.chaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
                .map { it.chapter.title }
                .distinctUntilChanged()
                .collectLatest { chapterTitle ->
                    notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                        title = chapterTitle
                    }
                }
        }

        // Update chapter notification intent
        launch {
            snapshotFlow { readerSession.readerSpeaker.currentTextPlaying.value }
                .mapNotNull { readerSession.chaptersLoader.chaptersStats[it.itemPos.chapterUrl] }
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
                    notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                        setContentIntent(
                            intentStack.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )
                        )
                    }
                }
        }

        // Update chapter progress
        launch {
            snapshotFlow { readerSession.speakerStats.value }
                .filterNotNull()
                .collectLatest {
                    val chapterPos = getString(
                        R.string.chapter_x_over_n,
                        it.chapterPosition + 1,
                        it.chapterCount
                    )
                    val progress = getString(
                        R.string.progress_x_percentage,
                        it.chapterReadPercentage()
                    )
                    notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                        text = "$chapterPos  $progress"
                    }
                }
        }

        return notificationBuilder.build()
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
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