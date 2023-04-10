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
import my.noveldokusha.R
import my.noveldokusha.data.database.AppDatabase
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.BookChaptersRepository
import my.noveldokusha.repository.ChapterBodyRepository
import my.noveldokusha.repository.LibraryBooksRepository
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.Extra_Uri
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.isServiceRunning
import my.noveldokusha.utils.removeProgressBar
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title
import my.noveldokusha.utils.tryAsResponse
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
    lateinit var repository: Repository

    @Inject
    lateinit var scraper: Scraper

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var toasty: Toasty

    @Inject
    lateinit var notificationsCenter: NotificationsCenter

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

    private val channel_id = "Restore backup"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(channel_id)
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
            tryAsResponse {
                restoreData(intentData.uri)
                repository.eventDataRestored.postValue(Unit)
            }.onError {
                Timber.e(it.exception)
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
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
    suspend fun restoreData(uri: Uri) = withContext(Dispatchers.IO) {

        val builder = notificationsCenter.showNotification(channel_id) {
            title = getString(R.string.restore_data)
            text = getString(R.string.loading_data)
            setProgress(100, 0, true)
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            notificationsCenter.modifyNotification(builder, channel_id) {
                removeProgressBar()
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
                notificationsCenter.modifyNotification(builder, channel_id) {
                    text = getString(R.string.loading_database)
                }
                val backupDatabase = inputStream.use {
                    val newDatabase = AppDatabase.createRoomFromStream(context, "temp_database", it)
                    val bookChaptersRepository = BookChaptersRepository(
                        chapterDao = newDatabase.chapterDao(),
                        operations = newDatabase
                    )
                    Repository(
                        db = newDatabase,
                        context = context,
                        name = "temp_database",
                        bookChaptersRepository = bookChaptersRepository,
                        chapterBodyRepository = ChapterBodyRepository(
                            chapterBodyDao = newDatabase.chapterBodyDao(),
                            operations = newDatabase,
                            bookChaptersRepository = bookChaptersRepository,
                            scraper = scraper,
                            networkClient = networkClient
                        ),
                        libraryBooksRepository = LibraryBooksRepository(
                            libraryDao = newDatabase.libraryDao(),
                            operations = newDatabase
                        )
                    )
                }
                notificationsCenter.modifyNotification(builder, channel_id) {
                    text = getString(R.string.adding_books)
                }
                repository.libraryBooks.insertReplace(backupDatabase.libraryBooks.getAll())
                notificationsCenter.modifyNotification(builder, channel_id) {
                    text = getString(R.string.adding_chapters)
                }
                repository.bookChapters.insert(backupDatabase.bookChapters.getAll())
                notificationsCenter.modifyNotification(builder, channel_id) {
                    text = getString(R.string.adding_chapters_text)
                }
                repository.chapterBody.insertReplace(backupDatabase.chapterBody.getAll())
                toasty.show(R.string.database_restored)
                backupDatabase.close()
                backupDatabase.delete()
            }.onError {
                notificationsCenter.modifyNotification(builder, channel_id) {
                    removeProgressBar()
                    text = getString(R.string.failed_to_restore_invalid_backup_database)
                }
            }
        }

        suspend fun mergeToBookFolder(entry: ZipEntry, inputStream: InputStream) {
            val file = File(repository.settings.folderBooks.parentFile, entry.name)
            if (file.isDirectory) return
            file.parentFile?.mkdirs()
            if (file.parentFile?.exists() != true) return
            file.outputStream().use { output ->
                inputStream.use { it.copyTo(output) }
            }
        }

        notificationsCenter.modifyNotification(builder, channel_id) {
            text = getString(R.string.adding_images)
        }
        for ((entry, file) in zipSequence) when {
            entry.name == "database.sqlite3" -> mergeToDatabase(file.inputStream())
            entry.name.startsWith("books/") -> mergeToBookFolder(entry, file.inputStream())
        }

        inputStream.closeQuietly()
        notificationsCenter.modifyNotification(builder, channel_id) {
            removeProgressBar()
            text = getString(R.string.data_restored)
        }
    }
}