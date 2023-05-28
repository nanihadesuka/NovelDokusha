package my.noveldokusha.services.narratorMediaControls

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.noveldokusha.services.BackupDataService
import my.noveldokusha.utils.*
import javax.inject.Inject


@AndroidEntryPoint
class NarratorMediaControlsService : Service() {

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

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupDataService::class.java)
    }

    @Inject
    lateinit var narratorNotification: NarratorMediaControlsNotification

    override fun onCreate() {
        super.onCreate()

        val notification = narratorNotification.createNotificationMediaControls(this) ?: return

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