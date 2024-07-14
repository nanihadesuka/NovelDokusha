package my.noveldokusha.feature.local_database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.feature.local_database.DAOs.ChapterBodyDao
import my.noveldokusha.feature.local_database.DAOs.ChapterDao
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class LocalDatabaseModule {

    companion object {

        @Provides
        @Singleton
        internal fun provideAppRoomDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.createRoom(
                context,
                name = "bookEntry"
            )
        }

        @Provides
        @Singleton
        fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()

        @Provides
        @Singleton
        fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

        @Provides
        @Singleton
        fun provideChapterBodyDao(database: AppDatabase): ChapterBodyDao = database.chapterBodyDao()
    }
}