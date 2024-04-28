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
import my.noveldokusha.BuildConfig
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.core.AppInternalState
import my.noveldokusha.ui.Toasty
import my.noveldokusha.ui.ToastyToast
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
        fun providesWorkManager(
            @ApplicationContext context: Context
        ): WorkManager {
            return WorkManager.getInstance(context)
        }

        @Provides
        @Singleton
        fun providesAppInternalState(): AppInternalState = object : AppInternalState {
            override val isDebugMode = BuildConfig.DEBUG
        }
    }
}