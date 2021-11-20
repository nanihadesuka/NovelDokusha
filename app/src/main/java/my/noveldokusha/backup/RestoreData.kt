package my.noveldokusha.backup

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.*
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Restore data function. Restores the library and images data given an uri.
 * This uir must point to a zip file where there must be a root file
 * "database.sqlite3" and an optional "books" folder where all the images
 * are stored (each subfolder is a book with its own structure).
 *
 * This function assumes the READ_EXTERNAL_STORAGE permission is granted.
 * This function will also show a status notificaton of the restoration progress.
 */
fun restoreData(uri: Uri) = App.scope.launch(Dispatchers.IO) {
    val channel_id = "Restore backup"
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
        return@launch
    }

    val zipSequence = ZipInputStream(inputStream).let { zipStream ->
        generateSequence { zipStream.nextEntry }
            .filterNot { it.isDirectory }
            .associateWith { zipStream.readBytes() }
    }


    suspend fun mergeToDatabase(inputStream: InputStream) = withContext(Dispatchers.IO)
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

    suspend fun mergetoBookFolder(entry: ZipEntry, inputStream: InputStream) = withContext(Dispatchers.IO)
    {
        val file = File(App.folderBooks.parentFile, entry.name)
        if (file.isDirectory) return@withContext
        file.parentFile?.mkdirs()
        if (file.parentFile?.exists() != true) return@withContext
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