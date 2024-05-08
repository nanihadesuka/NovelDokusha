package my.noveldokusha.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.R
import my.noveldokusha.core.tryAsResponse
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.core.utils.Extra_Boolean
import my.noveldokusha.core.utils.Extra_Uri
import my.noveldokusha.core.utils.NotificationsCenter
import my.noveldokusha.core.utils.isServiceRunning
import my.noveldokusha.core.utils.removeProgressBar
import my.noveldokusha.core.utils.text
import my.noveldokusha.core.utils.title
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class BackupDataService : Service() {

    @Inject
    lateinit var appDatabase: my.noveldokusha.tooling.local_database.AppDatabase

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    private val channelName by lazy { getString(R.string.notification_channel_name_backup) }
    private val channelId = "Backup"
    private val notificationId: Int = channelId.hashCode()

    private class IntentData : Intent {
        var uri by Extra_Uri()
        var backupImages by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, uri: Uri, backupImages: Boolean) : super(
            ctx,
            BackupDataService::class.java
        ) {
            this.uri = uri
            this.backupImages = backupImages
        }
    }

    companion object {
        fun start(ctx: Context, uri: Uri, backupImages: Boolean) {
            if (!isRunning(ctx))
                ctx.startService(IntentData(ctx, uri, backupImages))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupDataService::class.java)
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(
            channelId = channelId,
            channelName = channelName,
            notificationId = notificationId
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
                backupData(intentData.uri, intentData.backupImages)
            }.onError {
                Timber.e(it.exception)
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    /**
     * Backup data function. Backups the library and images data given an uri.
     * An zip file is created with a root file named "database.sqlite3" and
     * an optional "books" folder where all the images will be stored (each subfolder
     * is a book with its own structure).
     *
     * This function assumes the WRITE_EXTERNAL_STORAGE permission is granted.
     * This function will also show a status notificaton of the backup progress.
     */
    suspend fun backupData(uri: Uri, backupImages: Boolean) = withContext(Dispatchers.IO) {

        notificationsCenter.showNotification(
            notificationId = notificationId,
            channelName = channelName,
            channelId = channelId
        ) {
            title = getString(R.string.backup)
            text = getString(R.string.creating_backup)
            setProgress(100, 0, true)
        }

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val zip = ZipOutputStream(outputStream)

            notificationsCenter.modifyNotification(
                notificationBuilder,
                notificationId = notificationId
            ) {
                text = getString(R.string.copying_database)
            }

            // Save database
            run {
                val entry = ZipEntry("database.sqlite3")
                val file = this@BackupDataService.getDatabasePath(appDatabase.name)
                entry.method = ZipOutputStream.DEFLATED
                file.inputStream().use {
                    zip.putNextEntry(entry)
                    it.copyTo(zip)
                }
            }

            // Save books extra data (like images)
            if (backupImages) {
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.copying_images)
                }
                val basePath = appRepository.settings.folderBooks.toPath().parent
                appRepository.settings.folderBooks.walkBottomUp().filterNot { it.isDirectory }
                    .forEach { file ->
                        val name = basePath.relativize(file.toPath()).toString()
                        val entry = ZipEntry(name)
                        entry.method = ZipOutputStream.DEFLATED
                        file.inputStream().use {
                            zip.putNextEntry(entry)
                            it.copyTo(zip)
                        }
                    }
            }

            zip.closeQuietly()
            notificationsCenter.showNotification(
                notificationId = "Backup saved success".hashCode(),
                channelId = channelId,
                channelName = channelName
            ) {
                title = getString(R.string.backup_saved)
            }
        } ?: notificationsCenter.modifyNotification(
            notificationBuilder,
            notificationId = notificationId
        ) {
            removeProgressBar()
            text = getString(R.string.failed_to_make_backup)
        }
    }
}