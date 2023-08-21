package my.noveldokusha.workers.setup

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import my.noveldokusha.interactor.LibraryUpdatesInteractions
import my.noveldokusha.notifications.LibraryUpdateNotification
import my.noveldokusha.repository.AppRemoteRepository
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.workers.LibraryUpdatesWorker
import my.noveldokusha.workers.UpdatesCheckerWorker
import javax.inject.Inject

class AppWorkerFactory @Inject constructor(
    private val appRemoteRepository: AppRemoteRepository,
    private val notificationsCenter: NotificationsCenter,
    private val libraryUpdateNotification: LibraryUpdateNotification,
    private val libraryUpdatesInteractions: LibraryUpdatesInteractions,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        // Not using Timber as is yet no initialized at this stage
        Log.d("AppWorkerFactory", "AppWorkerFactory: workerClassName=$workerClassName")
        return when (workerClassName) {
            UpdatesCheckerWorker::class.java.name -> UpdatesCheckerWorker(
                context = appContext,
                workerParameters = workerParameters,
                appRemoteRepository = appRemoteRepository,
                notificationsCenter = notificationsCenter
            )
            LibraryUpdatesWorker::class.java.name -> LibraryUpdatesWorker(
                context = appContext,
                workerParameters = workerParameters,
                libraryUpdateNotification = libraryUpdateNotification,
                libraryUpdatesInteractions = libraryUpdatesInteractions,
            )
            else -> null
        }
    }
}