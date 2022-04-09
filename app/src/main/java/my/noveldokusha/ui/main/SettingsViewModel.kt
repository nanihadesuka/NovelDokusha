package my.noveldokusha.ui.main

import android.text.format.Formatter
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import my.noveldokusha.*
import my.noveldokusha.data.Repository
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.theme.Themes
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: Repository,
    private val appScope: CoroutineScope,
    private val appPreferences: AppPreferences
) : BaseViewModel()
{
    fun <T> stateCreator(theFlow: Flow<T>, initialValue: T): MutableState<T>
    {
        val value = mutableStateOf(initialValue)
        viewModelScope.launch(Dispatchers.IO) {
            theFlow.collect { withContext(Dispatchers.Main) { value.value = it } }
        }
        return value
    }

    val followsSystem by appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope)
    val theme by stateCreator(
        appPreferences.THEME_ID.flow().mapNotNull { Themes.fromIDTheme(it) },
        Themes.fromIDTheme(appPreferences.THEME_ID.value) ?: Themes.LIGHT
    )

    var databaseSize by mutableStateOf("")
    var imageFolderSize by mutableStateOf("")

    init
    {
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
        val libraryFolders = repository.bookLibrary.getAllInLibrary()
            .mapNotNull { """^local://(.+)$""".toRegex().find(it.url)?.destructured?.component1() }
            .toSet()

        repository.settings.folderBooks.listFiles()?.asSequence()
            ?.filter { it.isDirectory && it.exists() }
            ?.filter { it.name !in libraryFolders }
            ?.forEach { it.deleteRecursively() }

        updateImagesFolderSize()
        Glide.get(App.instance).clearDiskCache()
    }
}

private suspend fun getFolderSizeBytes(file: File): Long = withContext(Dispatchers.IO) {
    when
    {
        !file.exists() -> 0
        file.isFile -> file.length()
        else -> file.walkBottomUp().sumOf { if (it.isDirectory) 0 else it.length() }
    }
}


