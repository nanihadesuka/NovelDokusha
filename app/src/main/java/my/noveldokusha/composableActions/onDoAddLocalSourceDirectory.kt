package my.noveldokusha.composableActions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun onDoAddLocalSourceDirectory(
    onResult: (uri: Uri) -> Unit
): () -> Unit {
    val directorySelector = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeReadPersistent(),
        onResult = { uri ->
            if (uri != null) {
                onResult(uri)
            }
        }
    )

    return { directorySelector.launch(null) }
}

private class OpenDocumentTreeReadPersistent : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input)
            .addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
    }
}