package my.noveldokusha.scraper

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppCoroutineScope
import javax.inject.Inject

class LocalSourcesDirectories @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appCoroutineScope: AppCoroutineScope,
) {
    val list: List<Uri>
        get() = appContext.contentResolver.persistedUriPermissions.map { it.uri }

    val listState = mutableStateOf<List<Uri>>(list)

    fun add(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        updateState()
    }

    fun remove(uri: Uri) {
        appContext.contentResolver.releasePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        updateState()
    }

    private fun updateState() {
        appCoroutineScope.launch {
            listState.value = withContext(Dispatchers.IO) { list }
        }
    }
}