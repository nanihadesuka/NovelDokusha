package my.noveldokusha.workers.setup

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.domain.LibraryCategory
import my.noveldokusha.workers.LibraryUpdatesWorker
import my.noveldokusha.workers.UpdatesCheckerWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodicWorkersInitializer @Inject constructor(
    private val appPreferences: AppPreferences,
    private val workManager: WorkManager,
    private val appCoroutineScope: AppCoroutineScope
) {

    private fun startUpdatesChecker(enabled: Boolean) {
        Timber.d("startUpdatesChecker: called enabled=$enabled")
        if (!enabled) {
            if (!workManager.getWorkInfosByTag(UpdatesCheckerWorker.TAG).isCancelled) {
                workManager.cancelAllWorkByTag(UpdatesCheckerWorker.TAG)
            }
            return
        }

        workManager.enqueueUniquePeriodicWork(
            UpdatesCheckerWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            UpdatesCheckerWorker.createPeriodicRequest(),
        )
    }

    private fun startLibraryUpdates(enabled: Boolean, intervalHours: Int) {
        Timber.d("startLibraryUpdates: called enabled=$enabled intervalHours=$intervalHours")
        if (!enabled) {
            if (!workManager.getWorkInfosByTag(LibraryUpdatesWorker.TAG).isCancelled) {
                workManager.cancelAllWorkByTag(LibraryUpdatesWorker.TAG)
            }
            return
        }

        workManager.enqueueUniquePeriodicWork(
            LibraryUpdatesWorker.TAG,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            LibraryUpdatesWorker.createPeriodicRequest(
                updateCategory = LibraryCategory.DEFAULT,
                repeatIntervalHours = intervalHours
            ),
        )
    }

    fun init() {
        appCoroutineScope.launch {
            appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED
                .flow()
                .collectLatest { enabled ->
                    startUpdatesChecker(enabled)
                }
        }

        appCoroutineScope.launch {
            combine(
                appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_ENABLED.flow(),
                appPreferences.GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_INTERVAL_HOURS.flow()
            ) { enabled, intervalHours ->
                startLibraryUpdates(enabled, intervalHours)
            }.collect()
        }
    }
}