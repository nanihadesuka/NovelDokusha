package my.noveldokusha.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.noveldokusha.App
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import my.noveldokusha.ui.Toasty
import my.noveldokusha.ui.ToastyToast
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindToasty(toast: ToastyToast): Toasty

    companion object {

        @Provides
        @Singleton
        fun providesApp(@ApplicationContext context: Context): App {
            return context as App
        }

        @Provides
        @Singleton
        fun provideAppCoroutineScope(): AppCoroutineScope {
            return object : AppCoroutineScope {
                override val coroutineContext =
                    SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("App")
            }
        }

        @Provides
        @Singleton
        fun provideNetworkClient(app: App, @ApplicationContext appContext: Context): NetworkClient {
            return ScraperNetworkClient(
                cacheDir = File(app.cacheDir, "network_cache"),
                cacheSize = 5L * 1024 * 1024,
                appContext = appContext
            )
        }

        @Provides
        fun providesWorkManager(
            @ApplicationContext context: Context
        ): WorkManager {
            return WorkManager.getInstance(context)
        }
    }
}