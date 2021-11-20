package my.noveldokusha.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.uiUtils.*
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class RestoreDataService : Service()
{
    private class IntentData : Intent
    {
        var uri by Extra_Uri()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, uri: Uri) : super(ctx, RestoreDataService::class.java)
        {
            this.uri = uri
        }
    }

    companion object
    {
        fun start(ctx: Context, uri: Uri)
        {
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

    override fun onCreate()
    {
        super.onCreate()
        notificationBuilder = App.showNotification(channel_id) {}
        startForeground(channel_id.hashCode(), notificationBuilder.build())
    }

    override fun onDestroy()
    {
        job?.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if (intent == null) return START_NOT_STICKY
        val intent = IntentData(intent)

        job = CoroutineScope(Dispatchers.IO).launch {
            try
            {
                restoreData(intent.uri)
                bookstore.eventDataRestored.postValue(Unit)
            } catch (e: Exception)
            {
                Log.e(this::class.simpleName, "Failed to start command")
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

        val builder = App.showNotification(channel_id) {
            title = "Restore data"
            text = "Loading data"
            setProgress(100, 0, true)
        }

        val inputStream = App.instance.contentResolver.openInputStream(uri)
        if (inputStream == null)
        {
            builder.showNotification(channel_id) {
                removeProgressBar()
                text = R.string.failed_to_restore_cant_access_file.stringRes()
            }
            return@withContext
        }

        val zipSequence = ZipInputStream(inputStream).let { zipStream ->
            generateSequence { zipStream.nextEntry }
                .filterNot { it.isDirectory }
                .associateWith { zipStream.readBytes() }
        }


        suspend fun mergeToDatabase(inputStream: InputStream)
        {
            try
            {
                builder.showNotification(channel_id) { text = "Loading database" }
                val backupDatabase = inputStream.use { bookstore.DBase(bookstore.db_context, "temp_database", it) }
                builder.showNotification(channel_id) { text = "Adding books" }
                bookstore.bookLibrary.insertReplace(backupDatabase.bookLibrary.getAll())
                builder.showNotification(channel_id) { text = "Adding chapters" }
                bookstore.bookChapter.insert(backupDatabase.bookChapter.getAll())
                builder.showNotification(channel_id) { text = "Adding chapters text" }
                bookstore.bookChapterBody.insertReplace(backupDatabase.bookChapterBody.getAll())
                toast(R.string.database_restored.stringRes())
                backupDatabase.close()
                backupDatabase.delete()
            } catch (e: Exception)
            {
                builder.showNotification(channel_id) {
                    removeProgressBar()
                    text = R.string.failed_to_restore_invalid_backup_database.stringRes()
                }
            }
        }

        suspend fun mergetoBookFolder(entry: ZipEntry, inputStream: InputStream)
        {
            val file = File(App.folderBooks.parentFile, entry.name)
            if (file.isDirectory) return
            file.parentFile?.mkdirs()
            if (file.parentFile?.exists() != true) return
            builder.showNotification(channel_id) { text = "Adding image: ${file.name}" }
            file.outputStream().use { output ->
                inputStream.use { it.copyTo(output) }
            }
        }

        for ((entry, file) in zipSequence) when
        {
            entry.name == "database.sqlite3" -> mergeToDatabase(file.inputStream())
            entry.name.startsWith("books/") -> mergetoBookFolder(entry, file.inputStream())
        }

        inputStream.closeQuietly()
        builder.showNotification(channel_id) {
            removeProgressBar()
            text = "Data restored"
        }
    }
}