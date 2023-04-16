package my.noveldokusha.scraper

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.di.AppCoroutineScope
import timber.log.Timber
import javax.inject.Inject

class LocalSourcesDirectories @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appCoroutineScope: AppCoroutineScope,
) {
    val list: List<Uri>
        get() = appContext.contentResolver.persistedUriPermissions.map { it.uri }

    val listState = mutableStateOf<List<Uri>>(list)

    fun add(uri: Uri) {
        Timber.d("Add directory access read permission for :$uri")
        appContext.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        updateState()
    }

    fun remove(uri: Uri) {
        Timber.d("Remove directory access read permission for :$uri")
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