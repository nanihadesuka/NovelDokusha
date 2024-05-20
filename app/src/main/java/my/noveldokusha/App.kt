package my.noveldokusha

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import my.noveldokusha.di.HiltAppEntryPoint
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import my.noveldokusha.tooling.application_workers.setup.PeriodicWorkersInitializer
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class App : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var periodicWorkersInitializer: PeriodicWorkersInitializer

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        periodicWorkersInitializer.init()
    }

    override fun newImageLoader(): ImageLoader = when (val networkClient = networkClient) {
        is ScraperNetworkClient -> ImageLoader
            .Builder(this)
            .okHttpClient(networkClient.client)
            .build()

        else -> ImageLoader(this)
    }

    // WorkManager
    override fun getWorkManagerConfiguration(): Configuration {
        val appWorkerFactory = EntryPoints
            .get(this, HiltAppEntryPoint::class.java)
            .workerFactory()

        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setWorkerFactory(appWorkerFactory)
            .build()
    }
}
