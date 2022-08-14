package my.noveldokusha

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

val String.isLocalUri: Boolean get() = startsWith("local://")

val Context.folderBooks: File get() = File(filesDir, "books")
fun Context.getLocalBookFolder(bookUrl: String) =
    File(folderBooks, bookUrl.removePrefix("local://"))

fun Context.getLocalBookImage(bookUrl: String, imagePath: String) =
    File(getLocalBookFolder(bookUrl), imagePath.removePrefix("local://"))


/**
 * Returns the path to the image if local, no changes if non local.
 */
fun resolvedBookImagePath(context: Context, bookUrl: String, imagePath: String) = when {
    imagePath.startsWith("http://", ignoreCase = true) -> imagePath
    imagePath.startsWith("https://", ignoreCase = true) -> imagePath
    else -> context.getLocalBookImage(bookUrl, imagePath)
}

/**
 * Returns the path to the image if local, no changes if non local.
 */
@Composable
fun rememberResolvedBookImagePath(bookUrl: String, imagePath: String): State<Any> {
    val context = LocalContext.current
    return remember(context, bookUrl, imagePath) {
        mutableStateOf(resolvedBookImagePath(context, bookUrl, imagePath))
    }
}