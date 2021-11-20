package my.noveldokusha.backup

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.uiUtils.stringRes
import okhttp3.internal.closeQuietly
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun backupData(uri: Uri, backupImages: Boolean) = App.scope.launch(Dispatchers.IO) {

    val channel_id = "Backup"
    val builder = App.showNotification(channel_id) {
        title = "Backup"
        text = "Creating backup"
        setProgress(100, 0, true)
    }

    App.instance.contentResolver.openOutputStream(uri)?.use { outputStream ->
        val zip = ZipOutputStream(outputStream)

        builder.showNotification(channel_id) {
            text = "Copying database"
        }

        // Save database
        run {
            val entry = ZipEntry("database.sqlite3")
            val file = App.getDatabasePath(bookstore.appDB.name)
            entry.method = ZipOutputStream.DEFLATED
            file.inputStream().use {
                zip.putNextEntry(entry)
                it.copyTo(zip)
            }
        }

        // Save books extra data (like images)
        if (backupImages)
        {
            builder.showNotification(channel_id) {
                text = "Copying images"
            }
            val basePath = App.folderBooks.toPath().parent
            App.folderBooks.walkBottomUp().filterNot { it.isDirectory }.forEach { file ->
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
        builder.showNotification(channel_id) {
            removeProgressBar()
            text = R.string.backup_saved.stringRes()
        }
    } ?: builder.showNotification(channel_id) {
        removeProgressBar()
        text = R.string.failed_to_make_backup.stringRes()
    }
}