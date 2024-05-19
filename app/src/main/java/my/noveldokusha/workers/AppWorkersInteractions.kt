package my.noveldokusha.workers

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import my.noveldoksuha.interactor.WorkersInteractions
import my.noveldokusha.core.domain.LibraryCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWorkersInteractions @Inject constructor(
    private val workManager: WorkManager
): WorkersInteractions {

    override fun checkForLibraryUpdates(libraryCategory: LibraryCategory) {
        workManager.beginUniqueWork(
            LibraryUpdatesWorker.TAGManual,
            ExistingWorkPolicy.REPLACE,
            LibraryUpdatesWorker.createManualRequest(updateCategory = libraryCategory)
        ).enqueue()
    }
}