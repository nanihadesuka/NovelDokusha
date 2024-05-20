package my.noveldokusha.tooling.local_source

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.noveldokusha.core.AppCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSourcesDirectories @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appCoroutineScope: AppCoroutineScope,
) {
    val list: List<Uri>
        get() = appContext.contentResolver.persistedUriPermissions.map { it.uri }

    private val _listState = MutableStateFlow<List<Uri>>(list)
    val listState = _listState.asStateFlow()

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
        appCoroutineScope.launch(Dispatchers.Default) {
            _listState.update { list }
        }
    }
}