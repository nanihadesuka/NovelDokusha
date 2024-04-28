package my.noveldokusha.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.file.Paths
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppFileResolver @Inject constructor(
    @ApplicationContext context: Context,
) {
    companion object {
        const val coverPathRelativeToBook = "__cover_image"
    }

    val folderBooks = File(context.filesDir, "books")

    fun getLocalIfContentType(url: String, bookFolderName: String) =
        if (url.isContentUri) bookFolderName.addLocalUriPrefix else url

    fun getLocalBookCoverPath(): String = Paths.get(
        coverPathRelativeToBook
    ).toString().addLocalUriPrefix

    fun getLocalBookChapterPath(bookFolderName: String, chapterName: String): String = Paths.get(
        bookFolderName.removeLocalUriPrefix,
        chapterName.removeLocalUriPrefix
    ).toString().addLocalUriPrefix

    fun getLocalBookPath(bookFolderName: String): String = Paths.get(
        bookFolderName.removeLocalUriPrefix
    ).toString().addLocalUriPrefix

    fun getStorageBookCoverImageFile(bookFolderName: String): File = Paths.get(
        folderBooks.absolutePath,
        bookFolderName.removeLocalUriPrefix,
        coverPathRelativeToBook
    ).toFile()

    fun getStorageBookImageFile(bookFolderName: String, imagePath: String): File {
        val localBookFolderName = when {
            imagePath.isLocalUri -> getLocalBookFolderName(bookFolderName)
            else -> bookFolderName
        }
        return Paths.get(
            folderBooks.absolutePath,
            localBookFolderName.removeLocalUriPrefix,
            imagePath.removeLocalUriPrefix
        ).toFile()
    }

    fun getLocalBookFolderName(bookUrl: String): String = when {
        bookUrl.isHttpsUrl -> Base64.getEncoder().encodeToString(bookUrl.encodeToByteArray())
        bookUrl.isLocalUri -> bookUrl.removeLocalUriPrefix
        else -> bookUrl
    }

    /**
     * Returns the path to the image if local, no changes if non local.
     */
    fun resolvedBookImagePath(bookUrl: String, imagePath: String): Any = when {
        imagePath.isHttpsUrl -> imagePath
        bookUrl.isContentUri -> imagePath
        else -> getStorageBookImageFile(bookUrl, imagePath)
    }
}

/**
 * Returns the path to the image if local, no changes if non local.
 */
@Composable
fun rememberResolvedBookImagePath(bookUrl: String, imagePath: String): Any {
    val context = LocalContext.current
    val appFileResolver = remember(context) { AppFileResolver(context) }
    return remember(context, bookUrl, imagePath) {
        mutableStateOf(
            appFileResolver.resolvedBookImagePath(
                bookUrl = bookUrl,
                imagePath = imagePath
            )
        )
    }.value
}