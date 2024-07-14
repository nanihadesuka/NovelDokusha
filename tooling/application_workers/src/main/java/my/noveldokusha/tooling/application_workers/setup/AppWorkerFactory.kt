package my.noveldokusha.tooling.application_workers.setup

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import my.noveldoksuha.coreui.states.NotificationsCenter
import my.noveldoksuha.data.AppRemoteRepository
import my.noveldoksuha.interactor.LibraryUpdatesInteractions
import my.noveldokusha.tooling.application_workers.LibraryUpdatesWorker
import my.noveldokusha.tooling.application_workers.UpdatesCheckerWorker
import my.noveldokusha.tooling.application_workers.notifications.LibraryUpdateNotification
import javax.inject.Inject

class AppWorkerFactory @Inject internal constructor(
    private val appRemoteRepository: AppRemoteRepository,
    private val notificationsCenter: NotificationsCenter,
    private val libraryUpdateNotification: LibraryUpdateNotification,
    private val libraryUpdatesInteractions: LibraryUpdatesInteractions,
) : WorkerFactory() {
    @SuppressLint("LogNotTimber")
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