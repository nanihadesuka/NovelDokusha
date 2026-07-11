package my.noveldokusha.tooling.application_workers

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldoksuha.interactor.WorkersInteractions
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppWorkersModule {

    @Binds
    @Singleton
    internal abstract fun bindAppWorkersInteractions(v: AppWorkersInteractions): WorkersInteractions

    companion object {

        @Provides
        fun providesWorkManager(@ApplicationContext context: Context): WorkManager {
            return WorkManager.getInstance(context)
        }
    }
}