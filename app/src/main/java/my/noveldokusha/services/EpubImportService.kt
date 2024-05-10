package my.noveldokusha.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.epubImporter
import my.noveldokusha.R
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.asSequence
import my.noveldokusha.core.tryAsResponse
import my.noveldokusha.core.utils.Extra_Uri
import my.noveldokusha.core.utils.NotificationsCenter
import my.noveldokusha.core.utils.isServiceRunning
import my.noveldokusha.core.utils.removeProgressBar
import my.noveldokusha.core.utils.text
import my.noveldokusha.core.utils.title
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EpubImportService : Service() {
    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    @Inject
    lateinit var appFileResolver: AppFileResolver

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

        private fun isRunning(context: Context): Boolean =
            context.isServiceRunning(EpubImportService::class.java)
    }

    private val channelName by lazy { getString(R.string.notification_channel_name_import_epub) }
    private val channelId = "Import EPUB"
    private val notificationId = channelId.hashCode()

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(
            notificationId = notificationId,
            channelId = channelId,
            channelName = channelName,
        )
        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)
        job = CoroutineScope(Dispatchers.IO).launch {
            tryAsResponse {
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    title = getString(R.string.import_epub)
                    foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
                    setProgress(100, 0, true)
                }
                val inputStream = contentResolver.openInputStream(intentData.uri)
                if (inputStream == null) {
                    notificationsCenter.showNotification(
                        channelName = channelName,
                        channelId = channelId,
                        notificationId = "Import EPUB failure".hashCode()
                    ) {
                        text = getString(R.string.failed_get_file)
                        removeProgressBar()
                    }
                    return@tryAsResponse
                }

                val fileName = contentResolver.query(
                    intentData.uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                    null
                ).asSequence().map { it.getString(0) }.last()

                val epub = inputStream.use { my.noveldokusha.epub_parser.epubParser(inputStream = it) }

                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.importing_epub)
                }
                epubImporter(
                    storageFolderName = fileName,
                    appFileResolver = appFileResolver,
                    appRepository = appRepository,
                    epub = epub,
                    addToLibrary = true
                )
            }.onError {
                Timber.e(it.exception)
                notificationsCenter.showNotification(
                    channelName = channelName,
                    channelId = channelId,
                    notificationId = "Import EPUB failure".hashCode()
                ) {
                    text = getString(R.string.failed_to_import_epub)
                    setSubText(it.message)
                    removeProgressBar()
                }
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}