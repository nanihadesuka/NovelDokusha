package my.noveldokusha.repository

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.addLocalUriPrefix
import my.noveldokusha.isContentUri
import my.noveldokusha.removeLocalUriPrefix
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject


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

    fun getStorageBookImageFile(bookFolderName: String, imagePath: String): File = Paths.get(
        folderBooks.absolutePath,
        bookFolderName.removeLocalUriPrefix,
        imagePath.removeLocalUriPrefix
    ).toFile()

    /**
     * Returns the path to the image if local, no changes if non local.
     */
    fun resolvedBookImagePath(bookUrl: String, imagePath: String): Any = when {
        imagePath.startsWith("http://", ignoreCase = true) -> imagePath
        imagePath.startsWith("https://", ignoreCase = true) -> imagePath
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