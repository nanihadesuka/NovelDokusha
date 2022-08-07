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
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.network.ScrapperNetworkClient
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    const val mainDatabaseName = "bookEntry"

    @Provides
    fun providesApp(@ApplicationContext context: Context): App {
        return context as App
    }

    @Provides
    @Singleton
    fun provideRepository(
        database: AppDatabase,
        @ApplicationContext context: Context,
        networkClient: NetworkClient,
        scraper: Scraper,
    ): Repository {
        return Repository(database, context, mainDatabaseName, scraper, networkClient)
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
    @Singleton
    fun provideNetworkClient(app: App): NetworkClient {
        return ScrapperNetworkClient(
            cacheDir = File(app.cacheDir, "network_cache"),
            cacheSize = 5L * 1024 * 1024
        )
    }

    @Provides
    @Singleton
    fun provideScrapper(networkClient: NetworkClient): Scraper {
        return Scraper(networkClient)
    }

    @Provides
    fun provideLiveTranslation(
        translationManager: TranslationManager,
        appPreferences: AppPreferences,
    ): LiveTranslation {
        return LiveTranslation(translationManager, appPreferences)
    }
}