package my.noveldokusha

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.AppDatabase
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    const val mainDatabaseName = "bookEntry"

    @Provides
    @Singleton
    fun provideRepository(database: AppDatabase, @ApplicationContext context: Context): Repository {
        return Repository(database, context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.createRoom(context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("App"))
    }

    @Provides
    fun provideLiveTranslation(
        translationManager: TranslationManager,
        appPreferences: AppPreferences,
    ): LiveTranslation {
        return LiveTranslation(translationManager, appPreferences)
    }

}