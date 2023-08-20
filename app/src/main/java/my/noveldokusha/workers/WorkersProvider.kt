package my.noveldokusha.workers

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.di.AppCoroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkersProvider @Inject constructor(
    private val appPreferences: AppPreferences,
    private val workManager: WorkManager,
    private val appCoroutineScope: AppCoroutineScope
) {

    private fun startUpdatesChecker() {
        Timber.d("startUpdatesChecker: called")
        if (!appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED.value) {
            if (!workManager.getWorkInfosByTag(UpdatesCheckerWorker.TAG).isCancelled) {
                workManager.cancelAllWorkByTag(UpdatesCheckerWorker.TAG)
            }
            return
        }

        workManager.enqueueUniquePeriodicWork(
            UpdatesCheckerWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            UpdatesCheckerWorker.createRequest(),
        )
    }

    fun init() {

        appCoroutineScope.launch {
            appPreferences.GLOBAL_APP_UPDATER_CHECKER_ENABLED
                .flow()
                .collectLatest { startUpdatesChecker() }
        }
    }
}