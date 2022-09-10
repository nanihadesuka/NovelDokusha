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
import my.noveldokusha.data.database.AppDatabase
import my.noveldokusha.data.database.AppDatabaseOperations
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import my.noveldokusha.repository.*
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.Toasty
import my.noveldokusha.ui.ToastyToast
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.createRoom(context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideRepository(
        database: AppDatabase,
        @ApplicationContext context: Context,
        libraryBooksRepository: LibraryBooksRepository,
        bookChaptersRepository: BookChaptersRepository,
        chapterBodyRepository: ChapterBodyRepository,
    ): Repository {
        return Repository(
            database,
            context,
            mainDatabaseName,
            libraryBooksRepository,
            bookChaptersRepository,
            chapterBodyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabaseOperations(database: AppDatabase): AppDatabaseOperations {
        return database
    }

    @Provides
    @Singleton
    fun provideLibraryBooksRepository(database: AppDatabase): LibraryBooksRepository {
        return LibraryBooksRepository(libraryDao = database.libraryDao(), database)
    }

    @Provides
    @Singleton
    fun provideChapterBooksRepository(database: AppDatabase): BookChaptersRepository {
        return BookChaptersRepository(chapterDao = database.chapterDao(), database)
    }

    @Provides
    @Singleton
    fun provideChapterBodyRepository(
        database: AppDatabase,
        networkClient: NetworkClient,
        scraper: Scraper,
        bookChaptersRepository: BookChaptersRepository,

        ): ChapterBodyRepository {
        return ChapterBodyRepository(
            chapterBodyDao = database.chapterBodyDao(),
            networkClient = networkClient,
            scraper = scraper,
            bookChaptersRepository = bookChaptersRepository,
            operations = database
        )
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
    fun provideNetworkClient(app: App, @ApplicationContext context: Context): NetworkClient {
        return ScraperNetworkClient(
            cacheDir = File(app.cacheDir, "network_cache"),
            cacheSize = 5L * 1024 * 1024,
            appContext = context
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

    @Provides
    @Singleton
    fun provideToasty(@ApplicationContext context: Context): Toasty {
        return ToastyToast(applicationContext = context)
    }

    @Provides
    @Singleton
    fun provideScraperRepository(
        appPreferences: AppPreferences,
        scraper: Scraper
    ): ScraperRepository {
        return ScraperRepository(appPreferences, scraper)
    }
}