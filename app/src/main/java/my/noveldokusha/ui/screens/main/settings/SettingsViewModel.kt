package my.noveldokusha.ui.screens.main.settings

import android.text.format.Formatter
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.isLocalUri
import my.noveldokusha.removeLocalUriPrefix
import my.noveldokusha.repository.Repository
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.utils.asMutableStateOf
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: AppCoroutineScope,
    private val appPreferences: AppPreferences,
    private val app: App,
    private val translationManager: TranslationManager,
    private val stateHandle: SavedStateHandle,
) : BaseViewModel() {

    private val themeId by appPreferences.THEME_ID.state(viewModelScope)

    val state = SettingsScreenState(
        databaseSize = stateHandle.asMutableStateOf("databaseSize") { "" },
        imageFolderSize = stateHandle.asMutableStateOf("imageFolderSize") { "" },
        followsSystemTheme = appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope),
        currentTheme = derivedStateOf { Themes.fromIDTheme(themeId) },
        isTranslationSettingsVisible = mutableStateOf(translationManager.available),
        translationModelsStates = translationManager.models,
    )

    init {
        updateDatabaseSize()
        updateImagesFolderSize()
        viewModelScope.launch {
            repository.eventDataRestored.collect {
                updateDatabaseSize()
                updateImagesFolderSize()
            }
        }
    }

    fun downloadTranslationModel(lang: String) {
        translationManager.downloadModel(lang)
    }

    fun removeTranslationModel(lang: String) {
        translationManager.removeModel(lang)
    }

    fun cleanDatabase() = appScope.launch(Dispatchers.IO) {
        repository.settings.clearNonLibraryData()
        repository.vacuum()
        updateDatabaseSize()
    }

    fun cleanImagesFolder() = appScope.launch(Dispatchers.IO) {
        val libraryFolders = repository.libraryBooks.getAllInLibrary()
            .asSequence()
            .filter { it.url.isLocalUri }
            .map { it.url.removeLocalUriPrefix }
            .toSet()

        repository.settings.folderBooks.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.exists() }
            ?.filter { it.name !in libraryFolders }
            ?.forEach { it.deleteRecursively() }
        updateImagesFolderSize()
        Glide.get(app).clearDiskCache()
    }

    fun onFollowSystemChange(follow: Boolean) {
        appPreferences.THEME_FOLLOW_SYSTEM.value = follow
    }

    fun onThemeChange(themes: Themes) {
        appPreferences.THEME_ID.value = themes.themeId
    }

    private fun updateDatabaseSize() = viewModelScope.launch {
        val size = repository.getDatabaseSizeBytes()
        state.databaseSize.value = Formatter.formatFileSize(appPreferences.context, size)
    }

    private fun updateImagesFolderSize() = viewModelScope.launch {
        val size = getFolderSizeBytes(repository.settings.folderBooks)
        state.imageFolderSize.value = Formatter.formatFileSize(appPreferences.context, size)
    }
}

private suspend fun getFolderSizeBytes(file: File): Long = withContext(Dispatchers.IO) {
    when {
        !file.exists() -> 0
        file.isFile -> file.length()
        else -> file.walkBottomUp().sumOf { if (it.isDirectory) 0 else it.length() }
    }
}


