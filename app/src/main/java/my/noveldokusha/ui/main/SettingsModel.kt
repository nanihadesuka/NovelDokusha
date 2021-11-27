package my.noveldokusha.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ReportFragment
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.data.Repository
import my.noveldokusha.ui.BaseViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsModel @Inject constructor(
    private val repository: Repository
) : BaseViewModel()
{
    val themes = AppPreferences.let { it.globalThemeListLight + it.globalThemeListDark }
    val databseSizeBytes = MutableLiveData<Long>()
    val imagesFolderSizeBytes = MutableLiveData<Long>()
    val eventDataRestored = repository.eventDataRestored

    init
    {
        viewModelScope.launch(Dispatchers.IO) { updateDatabaseSize() }
        viewModelScope.launch(Dispatchers.IO) { updateImagesFolderSize() }
    }

    fun updateDatabaseSize() = viewModelScope.launch(Dispatchers.IO) {
        databseSizeBytes.postValue(repository.getDatabaseSizeBytes())
    }

    fun updateImagesFolderSize() = viewModelScope.launch(Dispatchers.IO) {
        imagesFolderSizeBytes.postValue(getFolderSizeBytes(App.folderBooks))
    }

    fun cleanDatabase() = App.scope.launch(Dispatchers.IO) {
        repository.settings.clearNonLibraryData()
        repository.vacuum()
        updateDatabaseSize()
    }

    fun cleanImagesFolder() = App.scope.launch(Dispatchers.IO) {
        val libraryFolders = repository.bookLibrary.getAllInLibrary()
            .mapNotNull { """^local://(.+)$""".toRegex().find(it.url)?.destructured?.component1() }
            .toSet()

        App.folderBooks.listFiles()?.asSequence()
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


