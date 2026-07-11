package my.noveldoksuha.coreui.composableActions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun onDoAskForImage(onSuccess: (uri: Uri) -> Unit): () -> Unit {
    val result by rememberUpdatedState(newValue = onSuccess)
    val imageExplorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                result(uri)
            }
        }
    )
    return {
        imageExplorer.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }
}
