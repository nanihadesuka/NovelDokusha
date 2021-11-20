package my.noveldokusha.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.ui.BaseViewModel
import java.io.File

class SettingsModel : BaseViewModel()
{
    val themes = (globalThemeList.light + globalThemeList.dark)
    val databseSizeBytes = MutableLiveData<Long>()
    val imagesFolderSizeBytes = MutableLiveData<Long>()

    init
    {
        viewModelScope.launch(Dispatchers.IO) { updateDatabaseSize() }
        viewModelScope.launch(Dispatchers.IO) { updateImagesFolderSize() }
    }

    suspend fun updateDatabaseSize() = withContext(Dispatchers.IO) {
        databseSizeBytes.postValue(bookstore.appDB.getDatabaseSizeBytes())
    }

    suspend fun updateImagesFolderSize() = withContext(Dispatchers.IO) {
        imagesFolderSizeBytes.postValue(getFolderSizeBytes(App.folderBooks))
    }

    fun cleanDatabase() = App.scope.launch(Dispatchers.IO) {
        bookstore.settings.clearNonLibraryData()
        bookstore.appDB.vacuum()
        updateDatabaseSize()
    }

    fun cleanImagesFolder() = App.scope.launch(Dispatchers.IO) {
        val libraryFolders = bookstore.bookLibrary.getAllInLibrary()
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


