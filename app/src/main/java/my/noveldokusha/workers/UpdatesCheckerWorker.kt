package my.noveldokusha.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import my.noveldokusha.R
import my.noveldokusha.core.Response
import my.noveldokusha.repository.AppRemoteRepository
import my.noveldokusha.utils.NotificationsCenter
import my.noveldokusha.utils.text
import my.noveldokusha.utils.title
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdatesCheckerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val appRemoteRepository: AppRemoteRepository,
    private val notificationsCenter: NotificationsCenter,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val TAG = "UpdatesChecker"

        fun createPeriodicRequest(): PeriodicWorkRequest {
            val builder = PeriodicWorkRequestBuilder<UpdatesCheckerWorker>(
                repeatInterval = 2,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            )

            val constrains = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return builder
                .addTag(TAG)
                .setConstraints(constrains)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
        }

    }

    private val notificationChannel = "NewAppVersion"

    override suspend fun doWork(): Result {
        Timber.d("UpdateCheckerWork: starting")
        val currentVersion = appRemoteRepository.getCurrentAppVersion()
        val newVersion = appRemoteRepository.getLastAppVersion()
            .onSuccess {
                Timber.d("UpdateCheckerWork: success")
                if (it.version > currentVersion) {
                    createUpdateNoticeNotification(
                        newVersion = it.version.toString(),
                        updatePageUrl = it.sourceUrl
                    )
                }
            }
            .onError {
                Timber.e("UpdateCheckerWork: failed", it)
            }

        return when (newVersion) {
            is Response.Error -> Result.failure()
            is Response.Success -> Result.success()
        }
    }

    private fun createUpdateNoticeNotification(newVersion: String, updatePageUrl: String) {

        val browserIntent = Intent(Intent.ACTION_VIEW).also { it.data = Uri.parse(updatePageUrl) }
        val intentStack = PendingIntent.getActivity(
            context,
            notificationChannel.hashCode(),
            browserIntent,
            PendingIntent.FLAG_ONE_SHOT
                    or PendingIntent.FLAG_IMMUTABLE
                    or PendingIntent.FLAG_CANCEL_CURRENT
        )

        notificationsCenter.showNotification(
            channelId = notificationChannel,
            channelName = context.getString(R.string.notification_channel_name_app_updates_checker),
            notificationId = notificationChannel.hashCode(),
            importance = NotificationManager.IMPORTANCE_LOW,
        ) {
            title = context.getString(R.string.new_app_version_available)
            text = newVersion
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
            setSmallIcon(R.drawable.baseline_app_update_notification_24)
            setContentIntent(intentStack)
            setAutoCancel(true)
        }
    }

}
