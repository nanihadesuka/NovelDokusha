package my.noveldokusha.composableActions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import my.noveldokusha.services.EpubImportService

@Composable
fun onDoImportEPUB(): () -> Unit {
    val context = LocalContext.current
    val fileExplorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null)
                EpubImportService.start(ctx = context, uri = uri)
        }
    )

    val permissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted)
            fileExplorer.launch("application/epub+zip")
    }

    return { permissions.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
}