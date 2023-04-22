package my.noveldokusha.tools.epub

import android.os.Build
import android.os.FileObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.Repository
import timber.log.Timber
import java.io.File
import java.nio.file.Paths
import kotlin.coroutines.resume


// TODO: this doesn't seem to work
suspend inline fun waitForFileDoneWriting(file: File, crossinline operation: () -> Unit): Unit =
    suspendCancellableCoroutine {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val observer = object : FileObserver(file) {
                override fun onEvent(event: Int, path: String?) {
                    when (event and ALL_EVENTS) {
                        CLOSE_WRITE -> it.resume(Unit)
                        else -> Unit
                    }
                }
            }
            observer.startWatching()
            operation()
            it.invokeOnCancellation {
                observer.stopWatching()
            }
        } else {
            it.resume(Unit)
        }
    }

suspend fun epubCoverImporter(
    storageFolderName: String,
    appFileResolver: AppFileResolver,
    coverImage: EpubImage,
) = withContext(Dispatchers.IO) {
    val imgFile = Paths.get(
        appFileResolver.folderBooks.path,
        storageFolderName,
        coverImage.absoluteFilePath
    ).toFile()
    imgFile.parentFile?.also { parent ->
        parent.mkdirs()
        if (parent.exists()) {
            waitForFileDoneWriting(imgFile) {
                imgFile.writeBytes(coverImage.image)
            }
        } else {
            Timber.e("Failed to create folder ${parent.absolutePath}")
        }
    }
}

suspend fun epubImporter(
    storageFolderName: String,
    repository: Repository,
    epub: EpubBook,
    addToLibrary: Boolean
): Unit = withContext(Dispatchers.IO) {
    val databaseBookUrl = storageFolderName.addLocalPrefix()

    // First clean any previous entries from the book
    repository.bookChapters.chapters(databaseBookUrl)
        .map { it.url }
        .let { repository.chapterBody.removeRows(it) }
    repository.bookChapters.removeAllFromBook(databaseBookUrl)
    repository.libraryBooks.remove(databaseBookUrl)

    // Insert new book data
    Book(
        title = epub.title,
        url = databaseBookUrl,
        coverImageUrl = epub.coverImagePath.addLocalPrefix(),
        inLibrary = addToLibrary
    ).let { repository.libraryBooks.insert(it) }

    epub.chapters
        .mapIndexed { i, it ->
            Chapter(
                title = it.title,
                url = it.url.addLocalPrefix(),
                bookUrl = databaseBookUrl,
                position = i
            )
        }
        .let { repository.bookChapters.insert(it) }

    epub.chapters
        .map { ChapterBody(url = it.url.addLocalPrefix(), body = it.body) }
        .let { repository.chapterBody.insertReplace(it) }

    epub.images.map {
        async {
            val imgFile = Paths.get(
                repository.settings.folderBooks.path,
                storageFolderName,
                it.absoluteFilePath
            ).toFile()
            imgFile.parentFile?.also { parent ->
                parent.mkdirs()
                if (parent.exists()) {
                    waitForFileDoneWriting(imgFile) {
                        imgFile.writeBytes(it.image)
                    }
                } else {
                    Timber.e("Failed to create folder ${parent.absolutePath}")
                }
            }
        }
    }.awaitAll()
}