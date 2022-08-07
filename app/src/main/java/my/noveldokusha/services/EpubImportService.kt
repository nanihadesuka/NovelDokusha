package my.noveldokusha.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.noveldokusha.R
import my.noveldokusha.createEpubBook
import my.noveldokusha.data.Repository
import my.noveldokusha.importEpubToRepository
import my.noveldokusha.utils.*
import javax.inject.Inject

@AndroidEntryPoint
class EpubImportService : Service() {
    @Inject
    lateinit var repository: Repository

    private class IntentData : Intent {
        var uri by Extra_Uri()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, uri: Uri) : super(ctx, EpubImportService::class.java) {
            this.uri = uri
        }
    }

    companion object {
        fun start(ctx: Context, uri: Uri) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, uri))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(EpubImportService::class.java)
    }

    private val channel_id = "Import EPUB"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = showNotification(this, channel_id) {}
        startForeground(channel_id.hashCode(), notificationBuilder.build())
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationBuilder.showNotification(channel_id) {
                    title = getString(R.string.import_epub)
                    foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                    setProgress(100, 0, true)
                }
                val inputStream = contentResolver.openInputStream(intentData.uri)
                if (inputStream == null) {
                    notificationBuilder.showNotification("epub import error") {
                        text = getString(R.string.failed_get_file)
                        removeProgressBar()
                    }
                    return@launch
                }
                val epub = inputStream.use { createEpubBook(it) }

                notificationBuilder.showNotification(channel_id) {
                    text = getString(R.string.importing_epub)
                }
                importEpubToRepository(repository, epub)

                notificationBuilder.showNotification(channel_id) {
                    text = getString(R.string.epub_added_to_library)
                    removeProgressBar()
                }
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "Failed to start command")
                Log.e("EPUB IMPORT FAILED", e.stackTraceToString())
                notificationBuilder.showNotification("epub import error") {
                    text = getString(R.string.failed_to_import_epub)
                    removeProgressBar()
                }
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}