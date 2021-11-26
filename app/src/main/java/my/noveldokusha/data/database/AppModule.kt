package my.noveldokusha.data.database

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.DAOs.ChapterBodyDao
import my.noveldokusha.data.database.DAOs.ChapterDao
import my.noveldokusha.data.database.DAOs.LibraryDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule
{
    const val mainDatabaseName = "bookEntry"

    @Provides
    @Singleton
    fun provideRepository(database: AppDatabase, @ApplicationContext context: Context): Repository
    {
        return Repository(database, context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase
    {
        return AppDatabase.createRoom(context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()

    @Provides
    @Singleton
    fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()

    @Provides
    @Singleton
    fun provideChapterBodyDao(db: AppDatabase): ChapterBodyDao = db.chapterBodyDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences
    {
        return context.appSharedPreferences()
    }
}