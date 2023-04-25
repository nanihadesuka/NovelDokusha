package my.noveldokusha.composableActions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import my.noveldokusha.services.RestoreDataService

@Composable
fun onDoRestore(): () -> Unit {
    val context = LocalContext.current
    val fileExplorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null)
                RestoreDataService.start(ctx = context, uri = uri)
        }
    )

    return { fileExplorer.launch("application/*") }
}