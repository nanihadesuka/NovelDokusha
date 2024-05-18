package my.noveldokusha.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldoksuha.coreui.states.NotificationsCenter
import my.noveldoksuha.coreui.states.removeProgressBar
import my.noveldoksuha.coreui.states.text
import my.noveldoksuha.coreui.states.title
import my.noveldoksuha.data.AppRepository
import my.noveldoksuha.data.BookChaptersRepository
import my.noveldoksuha.data.ChapterBodyRepository
import my.noveldoksuha.data.DownloaderRepository
import my.noveldoksuha.data.EpubImporterRepository
import my.noveldoksuha.data.LibraryBooksRepository
import my.noveldokusha.R
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.Toasty
import my.noveldokusha.core.tryAsResponse
import my.noveldokusha.core.utils.Extra_Uri
import my.noveldokusha.core.utils.isServiceRunning
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.tooling.local_database.AppDatabase
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

@AndroidEntryPoint
class RestoreDataService : Service() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var scraper: Scraper

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var appFileResolver: AppFileResolver

    @Inject
    lateinit var epubImporterRepository: EpubImporterRepository

    @Inject
    lateinit var toasty: Toasty

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

    @Inject
    lateinit var appCoroutineScope: AppCoroutineScope

    @Inject
    lateinit var downloaderRepository: DownloaderRepository

    private class IntentData : Intent {
        var uri by Extra_Uri()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, uri: Uri) : super(ctx, RestoreDataService::class.java) {
            this.uri = uri
        }
    }

    companion object {
        fun start(ctx: Context, uri: Uri) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, uri))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(RestoreDataService::class.java)
    }

    private val channelName by lazy { getString(R.string.notification_channel_name_restore_backup) }
    private val channelId = "Restore backup"
    private val notificationId = channelId.hashCode()


    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(
            notificationId = notificationId,
            channelId = channelId,
            channelName = channelName
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

        if (job?.isActive == true) return START_NOT_STICKY
        job = CoroutineScope(Dispatchers.IO).launch {
            tryAsResponse {
                restoreData(intentData.uri)
                appRepository.eventDataRestored.emit(Unit)
            }.onError {
                Timber.e(it.exception)
            }

            stopSelf(startId)
        }
        return START_STICKY
    }

    /**
     * Restore data function. Restores the library and images data given an uri.
     * The uri must point to a zip file where there must be a root file
     * "database.sqlite3" and an optional "books" folder where all the images
     * are stored (each subfolder is a book with its own structure).
     *
     * This function assumes the READ_EXTERNAL_STORAGE permission is granted.
     * This function will also show a status notificaton of the restoration progress.
     */
    private suspend fun restoreData(uri: Uri) = withContext(Dispatchers.IO) {

        notificationsCenter.modifyNotification(
            notificationBuilder,
            notificationId = notificationId
        ) {
            title = getString(R.string.restore_data)
            text = getString(R.string.loading_data)
            setProgress(100, 0, true)
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            notificationsCenter.showNotification(
                channelName = channelName,
                channelId = channelId,
                notificationId = "Backup restore failure".hashCode()
            ) {
                text = getString(R.string.failed_to_restore_cant_access_file)
            }
            return@withContext
        }

        val zipSequence = ZipInputStream(inputStream).let { zipStream ->
            generateSequence { zipStream.nextEntry }
                .filterNot { it.isDirectory }
                .associateWith { zipStream.readBytes() }
        }


        suspend fun mergeToDatabase(inputStream: InputStream) {
            tryAsResponse {
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.loading_database)
                }
                val backupDatabase = object {
                    val newDatabase = inputStream.use {
                        AppDatabase.createRoomFromStream(context, "temp_database", it)
                    }
                    val bookChapters = BookChaptersRepository(
                        chapterDao = newDatabase.chapterDao(),
                    )
                    val chapterBody = ChapterBodyRepository(
                        chapterBodyDao = newDatabase.chapterBodyDao(),
                        appDatabase = newDatabase,
                        bookChaptersRepository = bookChapters,
                        downloaderRepository = downloaderRepository
                    )
                    val libraryBooks = LibraryBooksRepository(
                        libraryDao = newDatabase.libraryDao(),
                        appDatabase = newDatabase,
                        context = context,
                        appFileResolver = appFileResolver,
                        appCoroutineScope = appCoroutineScope
                    )
                    fun close() = newDatabase.closeDatabase()
                    fun delete() = newDatabase.clearDatabase()
                }
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.adding_books)
                }
                appRepository.libraryBooks.insertReplace(backupDatabase.libraryBooks.getAll())
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.adding_chapters)
                }
                appRepository.bookChapters.insert(backupDatabase.bookChapters.getAll())
                notificationsCenter.modifyNotification(
                    notificationBuilder,
                    notificationId = notificationId
                ) {
                    text = getString(R.string.adding_chapters_text)
                }
                appRepository.chapterBody.insertReplace(backupDatabase.chapterBody.getAll())
                backupDatabase.close()
                backupDatabase.delete()
            }.onError {
                notificationsCenter.showNotification(
                    channelName = channelName,
                    channelId = channelId,
                    notificationId = "Backup restore failure - invalid database".hashCode()
                ) {
                    removeProgressBar()
                    text = getString(R.string.failed_to_restore_invalid_backup_database)
                }
            }.onSuccess {
                notificationsCenter.showNotification(
                    channelName = channelName,
                    channelId = channelId,
                    notificationId = "Backup restore success".hashCode()
                ) {
                    title = getString(R.string.backup_restored)
                }
            }
        }

        suspend fun mergeToBookFolder(entry: ZipEntry, inputStream: InputStream) {
            val file = File(appRepository.settings.folderBooks.parentFile, entry.name)
            if (file.isDirectory) return
            file.parentFile?.mkdirs()
            if (file.parentFile?.exists() != true) return
            file.outputStream().use { output ->
                inputStream.use { it.copyTo(output) }
            }
        }

        notificationsCenter.modifyNotification(
            notificationBuilder,
            notificationId = notificationId
        ) {
            text = getString(R.string.adding_images)
        }
        for ((entry, file) in zipSequence) when {
            entry.name == "database.sqlite3" -> mergeToDatabase(file.inputStream())
            entry.name.startsWith("books/") -> mergeToBookFolder(entry, file.inputStream())
        }

        inputStream.closeQuietly()
        notificationsCenter.modifyNotification(
            notificationBuilder,
            notificationId = notificationId
        ) {
            removeProgressBar()
            text = getString(R.string.data_restored)
        }
    }
}