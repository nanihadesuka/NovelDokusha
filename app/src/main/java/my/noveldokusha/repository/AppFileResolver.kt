package my.noveldokusha.repository

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AppFileResolver @Inject constructor(
    @ApplicationContext context: Context,
) {
    val folderBooks = File(context.filesDir, "books")

    fun getLocalBookFolder(bookUrl: String) =
        File(folderBooks, bookUrl.removePrefix("local://"))

    fun getLocalBookImage(bookUrl: String, imagePath: String) =
        File(getLocalBookFolder(bookUrl), imagePath.removePrefix("local://"))

    /**
     * Returns the path to the image if local, no changes if non local.
     */
    fun resolvedBookImagePath(bookUrl: String, imagePath: String): Any = when {
        imagePath.startsWith("http://", ignoreCase = true) -> imagePath
        imagePath.startsWith("https://", ignoreCase = true) -> imagePath
        else -> getLocalBookImage(bookUrl, imagePath)
    }

    fun resolvedBookImagePathString(bookUrl: String, imagePath: String): String = when {
        imagePath.startsWith("http://", ignoreCase = true) -> imagePath
        imagePath.startsWith("https://", ignoreCase = true) -> imagePath
        else -> getLocalBookImage(bookUrl, imagePath).canonicalPath
    }
}

/**
 * Returns the path to the image if local, no changes if non local.
 */
@Composable
fun rememberResolvedBookImagePath(bookUrl: String, imagePath: String): State<Any> {
    val context = LocalContext.current
    val appFileResolver = remember(context) { AppFileResolver(context) }
    return remember(context, bookUrl, imagePath) {
        mutableStateOf(
            appFileResolver.resolvedBookImagePath(
                bookUrl = bookUrl,
                imagePath = imagePath
            )
        )
    }
}