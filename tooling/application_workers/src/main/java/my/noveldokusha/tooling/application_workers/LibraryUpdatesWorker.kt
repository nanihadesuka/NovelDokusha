package my.noveldokusha.tooling.application_workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldoksuha.interactor.LibraryUpdatesInteractions
import my.noveldokusha.core.Response
import my.noveldokusha.core.domain.LibraryCategory
import my.noveldokusha.core.tryAsResponse
import my.noveldokusha.feature.local_database.tables.Book
import my.noveldokusha.tooling.application_workers.notifications.LibraryUpdateNotification
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
internal class LibraryUpdatesWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val libraryUpdateNotification: LibraryUpdateNotification,
    private val libraryUpdatesInteractions: LibraryUpdatesInteractions,
) : CoroutineWorker(context, workerParameters) {

    companion object {

        // TODO: solve when both are run at the same time (notifications will get weird)
        const val TAG = "LibraryUpdates"
        const val TAG_MANUAL = "LibraryUpdatesManual"

        private const val DATA_UPDATE_CATEGORY = "updateCategory"

        fun createPeriodicRequest(
            updateCategory: LibraryCategory,
            repeatIntervalHours: Int,
        ): PeriodicWorkRequest {
            val builder = PeriodicWorkRequestBuilder<LibraryUpdatesWorker>(
                repeatInterval = repeatIntervalHours.toLong(),
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )

            val constrains = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return builder
                .addTag(TAG)
                .setConstraints(constrains)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(createInputData(updateCategory))
                .build()
        }

        fun createManualRequest(
            updateCategory: LibraryCategory
        ): OneTimeWorkRequest {
            val builder = OneTimeWorkRequestBuilder<LibraryUpdatesWorker>()
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS
            return builder
                .addTag(TAG_MANUAL)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setInputData(createInputData(updateCategory))
                .build()
        }

        private fun createInputData(
            updateCategory: LibraryCategory
        ) = Data.Builder()
            .putString(DATA_UPDATE_CATEGORY, updateCategory.name)
            .build()
    }

    override suspend fun doWork(): Result {
        val updateCategory: LibraryCategory =
            inputData.getString(DATA_UPDATE_CATEGORY)?.let(LibraryCategory::valueOf)
                ?: LibraryCategory.DEFAULT

        Timber.d("LibraryUpdatesWorker: starting $updateCategory")

        val result = updateLibrary(updateCategory = updateCategory)
            .onError { Timber.e(it.exception) }

        return when (result) {
            is Response.Error -> Result.failure()
            is Response.Success -> Result.success()
        }
    }

    /**
     * Library update function. Will update non completed books or completed books.
     * This function will also show a status notificaton of the update progress.
     */
    private suspend fun updateLibrary(
        updateCategory: LibraryCategory
    ) = tryAsResponse {
        withContext(Dispatchers.Main) {

            libraryUpdateNotification.createEmptyUpdatingNotification()

            val countingUpdating =
                MutableStateFlow<LibraryUpdatesInteractions.CountingUpdating?>(null)
            val currentUpdating = MutableStateFlow<Set<Book>>(setOf())
            val newUpdates = MutableStateFlow<Set<LibraryUpdatesInteractions.NewUpdate>>(setOf())
            val failedUpdates = MutableStateFlow<Set<Book>>(setOf())

            val currentUpdatingNotifyJob = launch(Dispatchers.Main) {
                combine(
                    flow = countingUpdating,
                    flow2 = currentUpdating,
                ) { counting, current ->
                    libraryUpdateNotification.updateUpdatingNotification(
                        countingUpdated = counting?.updated ?: 0,
                        countingTotal = counting?.total ?: 0,
                        books = current
                    )
                }.collect()
            }

            libraryUpdatesInteractions.updateLibraryBooks(
                completedOnes = updateCategory == LibraryCategory.COMPLETED,
                countingUpdating = countingUpdating,
                currentUpdating = currentUpdating,
                newUpdates = newUpdates,
                failedUpdates = failedUpdates,
            )

            newUpdates.value.forEachIndexed { index, newUpdate ->
                libraryUpdateNotification.showNewChaptersNotification(
                    book = newUpdate.book,
                    newChapters = newUpdate.newChapters,
                    silent = index == 0
                )
            }

            if (failedUpdates.value.isNotEmpty()) {
                libraryUpdateNotification.showFailedNotification(
                    books = failedUpdates.value
                )
            }

            currentUpdatingNotifyJob.cancel()
            libraryUpdateNotification.closeNotification()
        }
    }

}