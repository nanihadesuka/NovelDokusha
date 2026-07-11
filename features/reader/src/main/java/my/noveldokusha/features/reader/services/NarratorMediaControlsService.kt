package my.noveldokusha.features.reader.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.core.utils.isServiceRunning
import javax.inject.Inject


@AndroidEntryPoint
internal class NarratorMediaControlsService : Service() {

    companion object {
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

        private fun isRunning(context: Context): Boolean =
            context.isServiceRunning(NarratorMediaControlsService::class.java)
    }

    @Inject
    lateinit var narratorNotification: NarratorMediaControlsNotification

    override fun onCreate() {
        super.onCreate()

        // Always call startForeground FIRST with a placeholder notification
        // to satisfy the 5-second ForegroundServiceDidNotStartInTimeException
        // deadline. If readerManager.session is null (race condition between
        // ReaderSession.close() and startForegroundService()), we still need
        // to call startForeground — otherwise the system crashes the app.
        val notification = narratorNotification.createNotificationMediaControls(this)
            ?: run {
                // Session is null — create a minimal notification and stop
                val placeholder = androidx.core.app.NotificationCompat.Builder(this, "reader_narrator")
                    .setContentTitle("ParasDokusha")
                    .setSmallIcon(my.noveldokusha.reader.R.drawable.ic_media_control_play)
                    .build()
                startForeground(narratorNotification.notificationId, placeholder)
                stopSelf()
                return
            }

        startForeground(narratorNotification.notificationId, notification)
    }

    override fun onDestroy() {
        narratorNotification.close()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        narratorNotification.handleCommand(intent)
        if (intent == null) return START_NOT_STICKY
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
