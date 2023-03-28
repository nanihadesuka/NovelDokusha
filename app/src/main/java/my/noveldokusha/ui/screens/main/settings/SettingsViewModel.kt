package my.noveldokusha.ui.screens.main.settings

import android.text.format.Formatter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.repository.Repository
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.theme.Themes
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: AppCoroutineScope,
    private val appPreferences: AppPreferences,
    private val app: App,
    val translationManager: TranslationManager,
) : BaseViewModel() {

    val followsSystem by appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope)

    val theme by derivedStateOf {
        val themeId by appPreferences.THEME_ID.state(viewModelScope)
        Themes.fromIDTheme(themeId) ?: Themes.LIGHT
    }

    var databaseSize by mutableStateOf("")
    var imageFolderSize by mutableStateOf("")

    init {
        viewModelScope.launch(Dispatchers.IO) { updateDatabaseSize() }
        viewModelScope.launch(Dispatchers.IO) { updateImagesFolderSize() }
        viewModelScope.launch(Dispatchers.IO) {
            repository.eventDataRestored.asFlow().collect {
                updateDatabaseSize()
                updateImagesFolderSize()
            }
        }
    }

    fun updateDatabaseSize() = viewModelScope.launch(Dispatchers.IO) {
        val size = repository.getDatabaseSizeBytes()
        withContext(Dispatchers.Main) {
            databaseSize = Formatter.formatFileSize(appPreferences.context, size)
        }
    }

    fun updateImagesFolderSize() = viewModelScope.launch(Dispatchers.IO) {
        val size = getFolderSizeBytes(repository.settings.folderBooks)
        withContext(Dispatchers.Main) {
            imageFolderSize = Formatter.formatFileSize(appPreferences.context, size)
        }
    }

    fun cleanDatabase() = appScope.launch(Dispatchers.IO) {
        repository.settings.clearNonLibraryData()
        repository.vacuum()
        updateDatabaseSize()
    }

    fun cleanImagesFolder() = appScope.launch(Dispatchers.IO) {
        val libraryFolders = repository.libraryBooks.getAllInLibrary()
            .mapNotNull { """^local://(.+)$""".toRegex().find(it.url)?.destructured?.component1() }
            .toSet()

        repository.settings.folderBooks.listFiles()?.asSequence()
            ?.filter { it.isDirectory && it.exists() }
            ?.filter { it.name !in libraryFolders }
            ?.forEach { it.deleteRecursively() }

        updateImagesFolderSize()
        Glide.get(app).clearDiskCache()
    }

    fun onFollowSystem(follow: Boolean) {
        appPreferences.THEME_FOLLOW_SYSTEM.value = follow
    }

    fun onThemeSelected(themes: Themes) {
        appPreferences.THEME_ID.value = Themes.toIDTheme(themes)
    }
}

private suspend fun getFolderSizeBytes(file: File): Long = withContext(Dispatchers.IO) {
    when {
        !file.exists() -> 0
        file.isFile -> file.length()
        else -> file.walkBottomUp().sumOf { if (it.isDirectory) 0 else it.length() }
    }
}


